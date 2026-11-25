package com.example.ui.editor.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.ui.editor.common.sampleBitmapLuminance
import com.example.ui.editor.photo.PhotoFilterPreset
import com.example.ui.editor.photo.PhotoGpuRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VideoEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var initializedKey: String? = null
    private var initialEditState: VideoEditState? = null

    private val _state = MutableStateFlow(VideoEditState())
    val state: StateFlow<VideoEditState> = _state.asStateFlow()

    val hasUnsavedChanges: StateFlow<Boolean> = _state
        .map { current ->
            val base = initialEditState ?: return@map false
            current.copy(selectedClipId = null) != base.copy(selectedClipId = null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val seenEditorTips: StateFlow<Set<String>> = settingsRepository.seenEditorTipsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val skipAllEditorTips: StateFlow<Boolean> = settingsRepository.skipAllEditorTipsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _backdropLuminance = MutableStateFlow(0.25f)
    val backdropLuminance: StateFlow<Float> = _backdropLuminance.asStateFlow()

    private val _isSliderDragging = MutableStateFlow(false)
    val isSliderDragging: StateFlow<Boolean> = _isSliderDragging.asStateFlow()

    private val _undoStack = MutableStateFlow<List<VideoEditState>>(emptyList())
    private val _redoStack = MutableStateFlow<List<VideoEditState>>(emptyList())

    val canUndo: StateFlow<Boolean> = combine(_undoStack) { it.first().isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val canRedo: StateFlow<Boolean> = combine(_redoStack) { it.first().isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _activeTab = MutableStateFlow(VideoEditorTab.TRIM)
    val activeTab: StateFlow<VideoEditorTab> = _activeTab.asStateFlow()

    private val _filterThumbnails = MutableStateFlow<Map<PhotoFilterPreset, Bitmap>>(emptyMap())
    val filterThumbnails: StateFlow<Map<PhotoFilterPreset, Bitmap>> = _filterThumbnails.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _clipThumbnails = MutableStateFlow<Map<String, List<Bitmap>>>(emptyMap())
    val clipThumbnails: StateFlow<Map<String, List<Bitmap>>> = _clipThumbnails.asStateFlow()

    private val _builtInTracks = MutableStateFlow<List<VideoAudioTrack>>(emptyList())
    val builtInTracks: StateFlow<List<VideoAudioTrack>> = _builtInTracks.asStateFlow()

    private val _exportProgress = MutableStateFlow<Float?>(null)
    val exportProgress: StateFlow<Float?> = _exportProgress.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private val _isTrimDragging = MutableStateFlow(false)
    val isTrimDragging: StateFlow<Boolean> = _isTrimDragging.asStateFlow()

    private var sliderDragBaseState: VideoEditState? = null
    private var initJob: Job? = null
    private var exportJob: Job? = null

    private val stripCacheDir by lazy {
        File(context.cacheDir, "video_strip_cache").apply { mkdirs() }
    }

    private companion object {
        val stripMemoryCache = LruCache<String, List<Bitmap>>(12)
    }

    fun initialize(uri: Uri, displayName: String, initialDurationMs: Long) {
        val key = "${uri}_${displayName}"
        if (initializedKey == key && _state.value.clips.isNotEmpty()) return
        initializedKey = key

        val initialClip = VideoClipItem(
            uriString = uri.toString(),
            displayName = displayName,
            sourceDurationMs = initialDurationMs.coerceAtLeast(1000L),
            trimStartMs = 0L,
            trimEndMs = initialDurationMs.coerceAtLeast(1000L)
        )
        val initialState = VideoEditState(
            clips = listOf(initialClip),
            selectedClipId = initialClip.id
        )
        initialEditState = initialState
        _state.value = initialState
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        _playheadMs.value = 0L

        initJob?.cancel()
        initJob = viewModelScope.launch {
            val tracksDeferred = async(Dispatchers.IO) {
                VideoAudioSynthesizer.ensureBuiltInTracks(context)
            }
            val probeDeferred = async {
                probeVideoAndThumbs(uri, initialDurationMs)
            }

            _builtInTracks.value = tracksDeferred.await()

            val (dur, thumbs) = probeDeferred.await()
            val currentClips = _state.value.clips
            if (currentClips.size == 1 && currentClips.first().id == initialClip.id) {
                val updatedClip = currentClips.first().let {
                    it.copy(
                        sourceDurationMs = dur,
                        trimEndMs = if (it.trimEndMs <= 1000L || it.trimEndMs > dur) dur else it.trimEndMs
                    )
                }
                val updatedState = _state.value.copy(clips = listOf(updatedClip))
                _state.value = updatedState
                if (_undoStack.value.isEmpty()) {
                    initialEditState = updatedState
                }
            }
            _clipThumbnails.value = _clipThumbnails.value + (uri.toString() to thumbs)
            val firstFrame = thumbs.firstOrNull()
            if (firstFrame != null) {
                val lumDeferred = async(Dispatchers.Default) { sampleBitmapLuminance(firstFrame) }
                _filterThumbnails.value = withContext(Dispatchers.Default) {
                    PhotoGpuRenderer.generateFilterThumbnails(firstFrame, thumbSize = 96)
                }
                _backdropLuminance.value = lumDeferred.await()
            }
        }
    }

    private fun stripCacheKey(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private suspend fun probeVideoAndThumbs(uri: Uri, fallbackDurationMs: Long): Pair<Long, List<Bitmap>> =
        withContext(Dispatchers.IO) {
            val cacheKey = stripCacheKey(uri)
            val cachedMem = stripMemoryCache.get(cacheKey)
            val diskDir = File(stripCacheDir, cacheKey)

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(500L)
                    ?: fallbackDurationMs.coerceAtLeast(1000L)

                if (cachedMem != null && cachedMem.isNotEmpty()) {
                    return@withContext dur to cachedMem
                }

                // Check disk cache before extracting frames
                val diskFrames = (0 until 5).mapNotNull { idx ->
                    val f = File(diskDir, "f_$idx.webp")
                    if (f.isFile && f.length() > 0L) BitmapFactory.decodeFile(f.absolutePath) else null
                }
                if (diskFrames.size == 5) {
                    stripMemoryCache.put(cacheKey, diskFrames)
                    return@withContext dur to diskFrames
                }

                val frameCount = 5
                val list = ArrayList<Bitmap>(frameCount)
                diskDir.mkdirs()
                for (i in 0 until frameCount) {
                    val tUs = ((dur * i) / frameCount.toLong()) * 1000L
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 96, 96)
                    } else {
                        retriever.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { raw ->
                            val scaled = Bitmap.createScaledBitmap(raw, 96, 96, true)
                            if (scaled !== raw) raw.recycle()
                            scaled
                        }
                    }
                    if (bmp != null) {
                        list.add(bmp)
                        runCatching {
                            FileOutputStream(File(diskDir, "f_$i.webp")).use { out ->
                                val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    Bitmap.CompressFormat.WEBP_LOSSY
                                } else {
                                    @Suppress("DEPRECATION")
                                    Bitmap.CompressFormat.WEBP
                                }
                                bmp.compress(fmt, 78, out)
                            }
                        }
                    }
                }
                val result = list.toList()
                if (result.isNotEmpty()) {
                    stripMemoryCache.put(cacheKey, result)
                }
                dur to result
            } catch (_: Throwable) {
                fallbackDurationMs.coerceAtLeast(1000L) to (cachedMem ?: emptyList())
            } finally {
                runCatching { retriever.release() }
            }
        }

    fun selectTab(tab: VideoEditorTab) {
        _activeTab.value = tab
    }

    fun markTipSeen(tab: VideoEditorTab) {
        viewModelScope.launch { settingsRepository.markEditorTipSeen("video_${tab.name}") }
    }

    fun skipAllTips() {
        viewModelScope.launch { settingsRepository.skipAllEditorTips() }
    }

    fun setSliderDragging(dragging: Boolean) {
        _isSliderDragging.value = dragging
    }

    fun updatePlayhead(ms: Long) {
        _playheadMs.value = ms.coerceIn(0L, _state.value.totalDurationMs)
    }

    private fun commitNewState(newState: VideoEditState, baseForUndo: VideoEditState = _state.value) {
        if (newState == baseForUndo) return
        _undoStack.value = (_undoStack.value + baseForUndo).takeLast(40)
        _redoStack.value = emptyList()
        _state.value = newState
    }

    fun undo() {
        val stack = _undoStack.value
        if (stack.isEmpty()) return
        val prev = stack.last()
        _undoStack.value = stack.dropLast(1)
        _redoStack.value = (_redoStack.value + _state.value).takeLast(40)
        _state.value = prev
    }

    fun redo() {
        val stack = _redoStack.value
        if (stack.isEmpty()) return
        val next = stack.last()
        _redoStack.value = stack.dropLast(1)
        _undoStack.value = (_undoStack.value + _state.value).takeLast(40)
        _state.value = next
    }

    // --- 1. Timeline Operations ---
    fun selectClip(clipId: String) {
        _state.value = _state.value.copy(selectedClipId = clipId)
    }

    fun addClip(uri: Uri) {
        viewModelScope.launch {
            val displayName = resolveDisplayName(uri) ?: "Clip_${_state.value.clips.size + 1}.mp4"
            val (dur, thumbs) = probeVideoAndThumbs(uri, 4000L)
            val newClip = VideoClipItem(
                uriString = uri.toString(),
                displayName = displayName,
                sourceDurationMs = dur,
                trimStartMs = 0L,
                trimEndMs = dur
            )
            _clipThumbnails.value = _clipThumbnails.value + (uri.toString() to thumbs)
            val current = _state.value
            commitNewState(
                current.copy(
                    clips = current.clips + newClip,
                    selectedClipId = newClip.id
                )
            )
        }
    }

    fun moveClip(fromIndex: Int, toIndex: Int) {
        val current = _state.value
        if (fromIndex !in current.clips.indices || toIndex !in current.clips.indices || fromIndex == toIndex) return
        val mutable = current.clips.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        commitNewState(current.copy(clips = mutable))
    }

    fun splitAtPlayhead() {
        val current = _state.value
        val targetMs = _playheadMs.value
        var cursorMs = 0L
        for (i in current.clips.indices) {
            val clip = current.clips[i]
            val clipDur = clip.effectiveDurationMs
            val clipEnd = cursorMs + clipDur
            if (targetMs > cursorMs + 250L && targetMs < clipEnd - 250L) {
                val offsetInEffective = targetMs - cursorMs
                val sourceDelta = (offsetInEffective.toFloat() * clip.speed).toLong()
                val splitSourceMs = (clip.trimStartMs + sourceDelta)
                    .coerceIn(clip.trimStartMs + 250L, clip.trimEndMs - 250L)

                val firstHalf = clip.copy(
                    id = UUID.randomUUID().toString(),
                    trimEndMs = splitSourceMs
                )
                val secondHalf = clip.copy(
                    id = UUID.randomUUID().toString(),
                    trimStartMs = splitSourceMs
                )
                val updated = current.clips.toMutableList().apply {
                    removeAt(i)
                    add(i, secondHalf)
                    add(i, firstHalf)
                }
                commitNewState(current.copy(clips = updated, selectedClipId = secondHalf.id))
                return
            }
            cursorMs = clipEnd
        }
    }

    fun previewTrimSelectedClip(startMs: Long, endMs: Long) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        _isTrimDragging.value = true
        val selected = _state.value.selectedClip ?: return
        val safeStart = startMs.coerceIn(0L, (selected.sourceDurationMs - 300L).coerceAtLeast(0L))
        val safeEnd = endMs.coerceIn(safeStart + 300L, selected.sourceDurationMs)
        val updated = _state.value.clips.map {
            if (it.id == selected.id) it.copy(trimStartMs = safeStart, trimEndMs = safeEnd) else it
        }
        _state.value = _state.value.copy(clips = updated)
    }

    fun commitSliderChange() {
        _isTrimDragging.value = false
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun deleteSelectedClip() {
        val current = _state.value
        if (current.clips.size <= 1) return
        val targetId = current.selectedClip?.id ?: return
        val remaining = current.clips.filterNot { it.id == targetId }
        commitNewState(
            current.copy(
                clips = remaining,
                selectedClipId = remaining.firstOrNull()?.id
            )
        )
    }

    // --- 2. Crop & Rotate ---
    fun setAspectRatio(ratio: VideoAspectRatio) {
        val current = _state.value
        commitNewState(current.copy(aspectRatio = ratio))
    }

    fun rotateSelectedClip() {
        val current = _state.value
        val target = current.selectedClip ?: return
        val updated = current.clips.map {
            if (it.id == target.id) it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) else it
        }
        commitNewState(current.copy(clips = updated))
    }

    fun flipSelectedClip() {
        val current = _state.value
        val target = current.selectedClip ?: return
        val updated = current.clips.map {
            if (it.id == target.id) it.copy(flipHorizontal = !it.flipHorizontal) else it
        }
        commitNewState(current.copy(clips = updated))
    }

    // --- 3. Speed (0.25x..4x, whole clip or segment) ---
    fun setSelectedClipSpeed(speed: Float) {
        val current = _state.value
        val target = current.selectedClip ?: return
        val safeSpeed = speed.coerceIn(0.25f, 4f)
        val updated = current.clips.map {
            if (it.id == target.id) it.copy(speed = safeSpeed) else it
        }
        commitNewState(current.copy(clips = updated))
    }

    fun applySpeedToSegment(segStartOffsetMs: Long, segEndOffsetMs: Long, speed: Float) {
        val current = _state.value
        val idx = current.clips.indexOfFirst { it.id == current.selectedClip?.id }
        if (idx < 0) return
        val clip = current.clips[idx]
        val safeSpeed = speed.coerceIn(0.25f, 4f)

        val clipSrcSpan = clip.trimmedSourceDurationMs
        if (clipSrcSpan < 900L || (segStartOffsetMs <= 150L && segEndOffsetMs >= clipSrcSpan - 150L)) {
            setSelectedClipSpeed(safeSpeed)
            return
        }

        val sMs = (clip.trimStartMs + segStartOffsetMs).coerceIn(clip.trimStartMs, clip.trimEndMs - 300L)
        val eMs = (clip.trimStartMs + segEndOffsetMs).coerceIn(sMs + 300L, clip.trimEndMs)

        val segments = ArrayList<VideoClipItem>(3)
        if (sMs - clip.trimStartMs > 200L) {
            segments.add(clip.copy(id = UUID.randomUUID().toString(), trimEndMs = sMs))
        }
        val midSeg = clip.copy(
            id = UUID.randomUUID().toString(),
            trimStartMs = sMs,
            trimEndMs = eMs,
            speed = safeSpeed
        )
        segments.add(midSeg)
        if (clip.trimEndMs - eMs > 200L) {
            segments.add(clip.copy(id = UUID.randomUUID().toString(), trimStartMs = eMs))
        }

        val updated = current.clips.toMutableList().apply {
            removeAt(idx)
            addAll(idx, segments)
        }
        commitNewState(current.copy(clips = updated, selectedClipId = midSeg.id))
    }

    // --- 4. Stabilize ---
    fun toggleStabilizeSelectedClip() {
        val current = _state.value
        val target = current.selectedClip ?: return
        val updated = current.clips.map {
            if (it.id == target.id) it.copy(stabilize = !it.stabilize) else it
        }
        commitNewState(current.copy(clips = updated))
    }

    // --- 5. Filters & Color Adjust ---
    fun selectFilter(filter: PhotoFilterPreset) {
        val current = _state.value
        commitNewState(
            current.copy(
                colorAdjustments = current.colorAdjustments.copy(filter = filter)
            )
        )
    }

    fun previewColorAdjustments(updated: VideoColorAdjustments) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        _state.value = _state.value.copy(colorAdjustments = updated)
    }

    fun resetFilterIntensity() {
        val current = _state.value
        if (current.colorAdjustments.filterIntensity == 1f) return
        commitNewState(
            current.copy(colorAdjustments = current.colorAdjustments.copy(filterIntensity = 1f))
        )
    }

    fun resetColorParam(param: VideoColorParam) {
        val current = _state.value
        val c = current.colorAdjustments
        val reset = when (param) {
            VideoColorParam.BRIGHTNESS -> if (c.brightness == 0f) return else c.copy(brightness = 0f)
            VideoColorParam.CONTRAST -> if (c.contrast == 0f) return else c.copy(contrast = 0f)
            VideoColorParam.SATURATION -> if (c.saturation == 0f) return else c.copy(saturation = 0f)
            VideoColorParam.WARMTH -> if (c.warmth == 0f) return else c.copy(warmth = 0f)
        }
        commitNewState(current.copy(colorAdjustments = reset))
    }

    // --- 6. Text Overlays ---
    fun addOrUpdateTextOverlay(overlay: VideoTextOverlay) {
        val current = _state.value
        val idx = current.textOverlays.indexOfFirst { it.id == overlay.id }
        val updated = if (idx >= 0) {
            current.textOverlays.toMutableList().apply { set(idx, overlay) }
        } else {
            current.textOverlays + overlay
        }
        commitNewState(current.copy(textOverlays = updated))
    }

    fun previewMoveTextOverlay(id: String, normX: Float, normY: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        val updated = _state.value.textOverlays.map {
            if (it.id == id) it.copy(normX = normX.coerceIn(0.08f, 0.92f), normY = normY.coerceIn(0.10f, 0.90f))
            else it
        }
        _state.value = _state.value.copy(textOverlays = updated)
    }

    fun deleteTextOverlay(id: String) {
        val current = _state.value
        commitNewState(current.copy(textOverlays = current.textOverlays.filterNot { it.id == id }))
    }

    // --- 7. Music & Volume Mixer ---
    fun selectMusicTrack(track: VideoAudioTrack?) {
        val current = _state.value
        commitNewState(current.copy(selectedMusic = track))
    }

    fun importDeviceAudio(uri: Uri) {
        viewModelScope.launch {
            val title = resolveDisplayName(uri) ?: "Custom Audio"
            val dur = withContext(Dispatchers.IO) {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(context, uri)
                    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()?.coerceAtLeast(1000L) ?: 15_000L
                } catch (_: Throwable) {
                    15_000L
                } finally {
                    runCatching { r.release() }
                }
            }
            val track = VideoAudioTrack(
                id = "device_${UUID.randomUUID()}",
                title = title.substringBeforeLast('.'),
                uriString = uri.toString(),
                isBuiltIn = false,
                durationMs = dur,
                trimStartMs = 0L,
                trimEndMs = dur
            )
            selectMusicTrack(track)
        }
    }

    fun previewMusicTrim(startMs: Long, endMs: Long) {
        val music = _state.value.selectedMusic ?: return
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        val s = startMs.coerceIn(0L, (music.durationMs - 500L).coerceAtLeast(0L))
        val e = endMs.coerceIn(s + 500L, music.durationMs)
        _state.value = _state.value.copy(
            selectedMusic = music.copy(trimStartMs = s, trimEndMs = e)
        )
    }

    fun previewOriginalVolume(volume: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        _state.value = _state.value.copy(originalVolume = volume.coerceIn(0f, 1f))
    }

    fun previewMusicVolume(volume: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        _state.value = _state.value.copy(musicVolume = volume.coerceIn(0f, 1f))
    }

    fun resetOriginalVolume() {
        val current = _state.value
        if (current.originalVolume == 1f) return
        commitNewState(current.copy(originalVolume = 1f))
    }

    fun resetMusicVolume() {
        val current = _state.value
        if (current.musicVolume == 0.7f) return
        commitNewState(current.copy(musicVolume = 0.7f))
    }

    fun setAudioPreset(origVol: Float, musVol: Float) {
        val current = _state.value
        commitNewState(
            current.copy(
                originalVolume = origVol.coerceIn(0f, 1f),
                musicVolume = musVol.coerceIn(0f, 1f)
            )
        )
    }

    // --- 8. Templates ---
    fun applyTemplate(preset: VideoTemplatePreset) {
        val current = _state.value
        val selectedId = current.selectedClip?.id
        val track = _builtInTracks.value.firstOrNull { it.id == preset.trackId }
        val updatedClips = current.clips.map { clip ->
            if (selectedId == null || clip.id == selectedId) {
                clip.copy(transitionAfter = preset.transition)
            } else clip
        }
        val introOverlay = VideoTextOverlay(
            text = preset.textLabel,
            fontStyle = preset.textFont,
            colorArgb = preset.textColorArgb,
            sizeSp = 28f,
            normX = 0.5f,
            normY = 0.80f,
            startMs = 0L,
            endMs = minOf(3500L, current.totalDurationMs)
        )
        commitNewState(
            current.copy(
                clips = updatedClips,
                colorAdjustments = preset.color,
                selectedMusic = track ?: current.selectedMusic,
                musicVolume = 0.65f,
                textOverlays = current.textOverlays.filterNot { it.startMs == 0L && it.endMs <= 3500L } + introOverlay
            )
        )
    }

    // --- 9. Export ---
    fun clearExportError() {
        _exportError.value = null
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _exportProgress.value = null
    }

    fun exportVideo(
        resolution: VideoExportResolution,
        onExportedFile: (File) -> Unit
    ) {
        if (_exportProgress.value != null) return
        _exportError.value = null
        _exportProgress.value = 0f

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            val outcome = VideoExporter.exportComposition(
                context = context,
                state = _state.value,
                resolution = resolution,
                onProgress = { pct -> _exportProgress.value = pct }
            )
            _exportProgress.value = null
            when (outcome) {
                is VideoExporter.ExportOutcome.Success -> onExportedFile(outcome.file)
                is VideoExporter.ExportOutcome.Failure -> _exportError.value = outcome.message
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    override fun onCleared() {
        initJob?.cancel()
        exportJob?.cancel()
        _filterThumbnails.value = emptyMap()
        _clipThumbnails.value = emptyMap()
        super.onCleared()
    }
}
