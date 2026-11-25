package com.example.ui.screens

import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.ui.editor.common.EditorBottomTabBar
import com.example.ui.editor.common.EditorPalette
import com.example.ui.editor.common.EditorTabUi
import com.example.ui.editor.common.EditorTooltipBanner
import com.example.ui.editor.common.EditorTopBar
import com.example.ui.editor.common.GlassButton
import com.example.ui.editor.common.GlassDialogShell
import com.example.ui.editor.common.GlassPanel
import com.example.ui.editor.common.GlassToastBanner
import com.example.ui.editor.common.LocalEditorGlassConfig
import com.example.ui.editor.common.UnsavedChangesDialog
import com.example.ui.editor.common.glassSheetMotion
import com.example.ui.editor.common.rememberEditorGlassConfig
import com.example.ui.editor.common.withGlassLegibility
import com.example.ui.editor.photo.MarkupFontStyle
import com.example.ui.editor.photo.toComposeColor
import com.example.ui.editor.video.MultiClipTimelineBar
import com.example.ui.editor.video.VideoAudioPanel
import com.example.ui.editor.video.VideoEditorTab
import com.example.ui.editor.video.VideoEditorViewModel
import com.example.ui.editor.video.VideoEffectsFactory
import com.example.ui.editor.video.VideoExportResolution
import com.example.ui.editor.video.VideoFiltersPanel
import com.example.ui.editor.video.VideoMusicPanel
import com.example.ui.editor.video.VideoSpeedPanel
import com.example.ui.editor.video.VideoTextOverlay
import com.example.ui.editor.video.VideoTextPanel
import com.example.ui.editor.video.VideoTrimPanel
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.tactilePressMotion
import com.example.ui.viewmodel.DateGroupingUtils
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    uri: Uri,
    displayName: String,
    initialDurationMs: Long,
    onCancel: () -> Unit,
    onPublishExportedVideo: (File, String, () -> Unit) -> Unit,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uri, displayName) {
        viewModel.initialize(uri, displayName, initialDurationMs)
    }

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val backdropLuminance by viewModel.backdropLuminance.collectAsStateWithLifecycle()
    val isSliderDragging by viewModel.isSliderDragging.collectAsStateWithLifecycle()
    val isTrimDragging by viewModel.isTrimDragging.collectAsStateWithLifecycle()

    val editState by viewModel.state.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
    val clipThumbnails by viewModel.clipThumbnails.collectAsStateWithLifecycle()
    val filterThumbnails by viewModel.filterThumbnails.collectAsStateWithLifecycle()
    val builtInTracks by viewModel.builtInTracks.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val exportError by viewModel.exportError.collectAsStateWithLifecycle()
    val seenEditorTips by viewModel.seenEditorTips.collectAsStateWithLifecycle()
    val skipAllEditorTips by viewModel.skipAllEditorTips.collectAsStateWithLifecycle()

    var isPlaying by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var selectedTextOverlayId by remember { mutableStateOf<String?>(null) }
    var editingOverlay by remember { mutableStateOf<VideoTextOverlay?>(null) }
    var overlayTextDraft by remember { mutableStateOf("") }
    var glassToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(glassToastMessage) {
        if (glassToastMessage != null) {
            delay(2000L)
            glassToastMessage = null
        }
    }

    // Zero-permission Android Photo Picker for adding clips
    val addClipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri ->
        if (pickedUri != null) {
            viewModel.addClip(pickedUri)
        }
    }

    // Zero-permission SAF audio picker for picking device audio
    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { pickedUri ->
        if (pickedUri != null) {
            viewModel.importDeviceAudio(pickedUri)
        }
    }

    val videoPlayer = remember {
        runCatching {
            val factory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(2_000, 8_000, 250, 500)
                .build()
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            ExoPlayer.Builder(context, factory)
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ALL
                }
        }.getOrNull()
    }

    val musicPlayer = remember {
        runCatching {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, false)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                }
        }.getOrNull()
    }

    DisposableEffect(lifecycleOwner, videoPlayer, musicPlayer) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    runCatching {
                        videoPlayer?.playWhenReady = false
                        videoPlayer?.pause()
                    }
                    runCatching {
                        musicPlayer?.playWhenReady = false
                        musicPlayer?.pause()
                    }
                    isPlaying = false
                }
                Lifecycle.Event.ON_DESTROY -> {
                    runCatching {
                        videoPlayer?.playWhenReady = false
                        videoPlayer?.stop()
                        videoPlayer?.clearVideoSurface()
                        videoPlayer?.release()
                    }
                    runCatching {
                        musicPlayer?.playWhenReady = false
                        musicPlayer?.stop()
                        musicPlayer?.release()
                    }
                    isPlaying = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            runCatching {
                videoPlayer?.playWhenReady = false
                videoPlayer?.stop()
                videoPlayer?.clearVideoSurface()
                videoPlayer?.release()
            }
            runCatching {
                musicPlayer?.playWhenReady = false
                musicPlayer?.stop()
                musicPlayer?.release()
            }
        }
    }

    val handleExitRequest = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onCancel()
        }
    }

    BackHandler(enabled = exportProgress == null) {
        handleExitRequest()
    }

    // Sync video playlist when clips or clip trims change
    val playlistSignature = remember(editState.clips) {
        editState.clips.map { "${it.id}_${it.uriString}_${it.trimStartMs}_${it.trimEndMs}" }
    }
    LaunchedEffect(playlistSignature, videoPlayer, isTrimDragging) {
        if (isTrimDragging) return@LaunchedEffect
        val player = videoPlayer ?: return@LaunchedEffect
        val mediaItems = editState.clips.map { clip ->
            MediaItem.Builder()
                .setUri(Uri.parse(clip.uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                        .setEndPositionMs(clip.trimEndMs.coerceAtLeast(clip.trimStartMs + 250L))
                        .build()
                )
                .build()
        }
        if (mediaItems.isNotEmpty()) {
            player.setMediaItems(mediaItems, true)
            player.prepare()
        }
    }

    val activeClipForEffects = editState.selectedClip
    LaunchedEffect(
        videoPlayer,
        activeClipForEffects?.rotationDegrees,
        activeClipForEffects?.flipHorizontal,
        activeClipForEffects?.stabilize,
        activeClipForEffects?.speed,
        editState.aspectRatio,
        editState.colorAdjustments
    ) {
        val player = videoPlayer ?: return@LaunchedEffect
        val clip = activeClipForEffects ?: return@LaunchedEffect
        delay(36L)
        val effects = VideoEffectsFactory.buildClipVideoEffects(
            clip = clip,
            aspectRatio = editState.aspectRatio,
            colorAdjustments = editState.colorAdjustments,
            includeSpeedEffect = false
        )
        runCatching {
            player.setVideoEffects(effects)
            player.setPlaybackParameters(PlaybackParameters(clip.speed.coerceIn(0.25f, 4f)))
        }
    }

    LaunchedEffect(videoPlayer, editState.originalVolume) {
        runCatching { videoPlayer?.volume = editState.originalVolume }
    }
    LaunchedEffect(musicPlayer, editState.musicVolume) {
        runCatching { musicPlayer?.volume = editState.musicVolume }
    }

    val selectedMusic = editState.selectedMusic
    LaunchedEffect(
        musicPlayer,
        selectedMusic?.id,
        selectedMusic?.uriString,
        selectedMusic?.trimStartMs,
        selectedMusic?.trimEndMs
    ) {
        val mp = musicPlayer ?: return@LaunchedEffect
        if (selectedMusic == null) {
            runCatching { mp.stop(); mp.clearMediaItems() }
        } else {
            val item = MediaItem.Builder()
                .setUri(Uri.parse(selectedMusic.uriString))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(selectedMusic.trimStartMs.coerceAtLeast(0L))
                        .setEndPositionMs(selectedMusic.trimEndMs.coerceAtLeast(selectedMusic.trimStartMs + 500L))
                        .build()
                )
                .build()
            runCatching {
                mp.setMediaItem(item)
                mp.prepare()
                if (isPlaying) mp.play()
            }
        }
    }

    val latestClips by rememberUpdatedState(editState.clips)
    val latestMusic by rememberUpdatedState(selectedMusic)

    DisposableEffect(videoPlayer, musicPlayer) {
        val player = videoPlayer
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    val mp = musicPlayer ?: return
                    if (latestMusic != null) {
                        runCatching {
                            if (playing && !mp.isPlaying) mp.play()
                            else if (!playing && mp.isPlaying) mp.pause()
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val clips = latestClips
                    val idx = player.currentMediaItemIndex.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
                    val speed = clips.getOrNull(idx)?.speed ?: 1f
                    runCatching { player.setPlaybackParameters(PlaybackParameters(speed.coerceIn(0.25f, 4f))) }
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
    }

    LaunchedEffect(videoPlayer) {
        val player = videoPlayer ?: return@LaunchedEffect
        snapshotFlow { isPlaying }.collectLatest { playing ->
            if (!playing) return@collectLatest
            while (true) {
                runCatching {
                    val clips = latestClips
                    val idx = player.currentMediaItemIndex.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
                    val prefixMs = clips.take(idx).sumOf { it.effectiveDurationMs }
                    val speed = clips.getOrNull(idx)?.speed ?: 1f
                    val inClipEffective = (player.currentPosition.coerceAtLeast(0L).toFloat() / speed).toLong()
                    viewModel.updatePlayhead(prefixMs + inClipEffective)
                }
                delay(60L)
            }
        }
    }

    val seekTimelineTo: (Long) -> Unit = { targetGlobalMs ->
        val player = videoPlayer
        if (player != null && editState.clips.isNotEmpty()) {
            var remaining = targetGlobalMs.coerceIn(0L, editState.totalDurationMs)
            var targetIndex = 0
            var targetOffsetMs = 0L
            for (i in editState.clips.indices) {
                val clip = editState.clips[i]
                val dur = clip.effectiveDurationMs
                if (remaining <= dur || i == editState.clips.lastIndex) {
                    targetIndex = i
                    targetOffsetMs = (remaining.toFloat() * clip.speed).toLong()
                        .coerceIn(0L, clip.trimmedSourceDurationMs)
                    break
                }
                remaining -= dur
            }
            runCatching {
                player.seekTo(targetIndex, targetOffsetMs)
                viewModel.updatePlayhead(targetGlobalMs)
            }
        } else {
            viewModel.updatePlayhead(targetGlobalMs)
        }
    }

    val videoTabs = remember {
        listOf(
            EditorTabUi(VideoEditorTab.TRIM, "Trim", Icons.Rounded.ContentCut, "video_tab_trim"),
            EditorTabUi(VideoEditorTab.SPEED, "Speed", Icons.Rounded.Speed, "video_tab_speed"),
            EditorTabUi(VideoEditorTab.FILTERS, "Filters", Icons.Rounded.FilterVintage, "video_tab_filters"),
            EditorTabUi(VideoEditorTab.TEXT, "Text", Icons.Rounded.TextFields, "video_tab_text"),
            EditorTabUi(VideoEditorTab.MUSIC, "Music", Icons.Rounded.MusicNote, "video_tab_music"),
            EditorTabUi(VideoEditorTab.AUDIO, "Audio", Icons.Rounded.GraphicEq, "video_tab_audio")
        )
    }

    val editorHazeState = rememberHazeState(
        blurEnabled = !appSettings.reduceTransparency && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    )
    val glassConfig = rememberEditorGlassConfig(
        themeMode = appSettings.themeMode,
        reduceTransparencyPref = appSettings.reduceTransparency,
        backdropLuminance = backdropLuminance,
        hazeState = editorHazeState
    )
    val canvasBackground = if (glassConfig.isDarkGlass) EditorPalette.CanvasBg else EditorPalette.LightCanvasBg

    val motion = LocalMotionConfig.current
    val density = LocalDensity.current
    val statusTopPx = WindowInsets.statusBars.getTop(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    var measuredTopBarPx by remember { mutableIntStateOf(0) }
    var measuredBottomDockPx by remember { mutableIntStateOf(0) }

    val targetTopStagePadding = if (measuredTopBarPx > 0) {
        with(density) { (statusTopPx + measuredTopBarPx).toDp() } + 8.dp
    } else {
        with(density) { statusTopPx.toDp() } + 76.dp
    }
    val targetBottomStagePadding = if (measuredBottomDockPx > 0) {
        with(density) { measuredBottomDockPx.toDp() } + 10.dp
    } else {
        with(density) { navBottomPx.toDp() } + 300.dp
    }
    val topStagePadding by animateDpAsState(
        targetValue = targetTopStagePadding,
        animationSpec = if (motion.prefersReducedMotion) snap() else MotionTokens.SnappyDpSpring,
        label = "video_stage_top_pad"
    )
    val bottomStagePadding by animateDpAsState(
        targetValue = targetBottomStagePadding,
        animationSpec = if (motion.prefersReducedMotion) snap() else MotionTokens.SnappyDpSpring,
        label = "video_stage_bottom_pad"
    )

    CompositionLocalProvider(LocalEditorGlassConfig provides glassConfig) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(canvasBackground)
            ) {
                // Layer 0: Video Stage framed cleanly above the floating edit dock
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = editorHazeState, zIndex = 0f)
                        .padding(
                            top = topStagePadding,
                            bottom = bottomStagePadding,
                            start = 12.dp,
                            end = 12.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val previewRatio = editState.aspectRatio.ratio
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val stageMod = if (previewRatio != null) {
                            Modifier.then(
                                if (maxWidth / maxHeight > previewRatio) {
                                    Modifier.fillMaxHeight().aspectRatio(previewRatio)
                                } else {
                                    Modifier.fillMaxWidth().aspectRatio(previewRatio)
                                }
                            )
                        } else {
                            Modifier.fillMaxSize()
                        }

                        Box(
                            modifier = stageMod.background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (videoPlayer != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        val view = LayoutInflater.from(ctx).inflate(
                                            R.layout.view_texture_player,
                                            null,
                                            false
                                        ) as PlayerView
                                        view.apply {
                                            useController = false
                                            isClickable = false
                                            isFocusable = false
                                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                            setKeepContentOnPlayerReset(true)
                                            resizeMode = if (previewRatio != null) {
                                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            } else {
                                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            }
                                            player = videoPlayer
                                        }
                                    },
                                    update = { pv ->
                                        pv.resizeMode = if (previewRatio != null) {
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        } else {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Draggable Text Overlays visible at current playhead
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val density = LocalDensity.current
                                val stageWPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                                val stageHPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

                                val latestStageWPx by rememberUpdatedState(stageWPx)
                                val latestStageHPx by rememberUpdatedState(stageHPx)
                                editState.textOverlays
                                    .filter { playheadMs in it.startMs..it.endMs }
                                    .forEach { overlay ->
                                        key(overlay.id) {
                                            val latestOverlay by rememberUpdatedState(overlay)
                                            val fontFam = when (overlay.fontStyle) {
                                                MarkupFontStyle.SANS -> FontFamily.SansSerif
                                                MarkupFontStyle.SERIF -> FontFamily.Serif
                                                MarkupFontStyle.MONO -> FontFamily.Monospace
                                                MarkupFontStyle.BOLD -> FontFamily.Default
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .offset {
                                                        IntOffset(
                                                            (overlay.normX * stageWPx).roundToInt() - 48,
                                                            (overlay.normY * stageHPx).roundToInt() - 22
                                                        )
                                                    }
                                                    .pointerInput(overlay.id) {
                                                        detectDragGestures(
                                                            onDrag = { change, drag ->
                                                                change.consume()
                                                                val cur = latestOverlay
                                                                viewModel.previewMoveTextOverlay(
                                                                    cur.id,
                                                                    cur.normX + drag.x / latestStageWPx,
                                                                    cur.normY + drag.y / latestStageHPx
                                                                )
                                                            },
                                                            onDragEnd = { viewModel.commitSliderChange() }
                                                        )
                                                    }
                                                    .clickable {
                                                        selectedTextOverlayId = latestOverlay.id
                                                        overlayTextDraft = latestOverlay.text
                                                        editingOverlay = latestOverlay
                                                    }
                                                    .padding(4.dp)
                                            ) {
                                                Text(
                                                    text = overlay.text,
                                                    color = overlay.colorArgb.toComposeColor(),
                                                    fontSize = overlay.sizeSp.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = fontFam
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }

                // Layer 1: Floating Top Glass Controls + Glass Toast
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = !isSliderDragging,
                        enter = if (motion.prefersReducedMotion) motion.fadeEnter(MotionTokens.MicroMs)
                        else motion.fadeEnter(MotionTokens.MicroMs) + slideInVertically(motion.uiTween(MotionTokens.MicroMs)) { -it / 2 },
                        exit = if (motion.prefersReducedMotion) motion.fadeExit(MotionTokens.MicroFastMs)
                        else motion.fadeExit(MotionTokens.MicroFastMs) + slideOutVertically(motion.exitTween(MotionTokens.MicroFastMs)) { -it / 2 },
                        modifier = Modifier.onSizeChanged { size ->
                            if (!isSliderDragging && size.height > 0) {
                                measuredTopBarPx = size.height
                            }
                        }
                    ) {
                        EditorTopBar(
                            canUndo = canUndo,
                            canRedo = canRedo,
                            primaryActionLabel = "Export",
                            primaryActionIcon = Icons.Rounded.Download,
                            primaryEnabled = exportProgress == null,
                            isBusy = exportProgress != null,
                            onCancel = handleExitRequest,
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            onPrimaryAction = {
                                runCatching { videoPlayer?.pause(); musicPlayer?.pause() }
                                showExportDialog = true
                            },
                            cancelTag = "video_editor_cancel",
                            undoTag = "video_editor_undo",
                            redoTag = "video_editor_redo",
                            primaryTag = "video_editor_export"
                        )
                    }

                    GlassToastBanner(
                        message = glassToastMessage,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Layer 2: Floating Glass Scrubber, Tooltip, Timeline Glass Strip, Tool Sheet, Pill Tab Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (!isSliderDragging && !isTrimDragging && size.height > 0) {
                                measuredBottomDockPx = size.height
                            }
                        }
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact Glass Play/Pause + Scrubber Row (>= 48dp touch targets)
                    AnimatedVisibility(
                        visible = !isSliderDragging,
                        enter = motion.fadeEnter(MotionTokens.MicroMs),
                        exit = motion.fadeExit(MotionTokens.MicroFastMs)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassButton(
                                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause video" else "Play video",
                                active = isPlaying,
                                enableBlur = false,
                                strongFill = true,
                                modifier = Modifier.testTag("video_play_pause"),
                                onClick = {
                                    val vp = videoPlayer ?: return@GlassButton
                                    if (vp.isPlaying) {
                                        vp.pause()
                                        musicPlayer?.pause()
                                    } else {
                                        vp.play()
                                        if (selectedMusic != null) musicPlayer?.play()
                                    }
                                }
                            )

                            GlassPanel(
                                shape = CircleShape,
                                enableBlur = false,
                                elevation = 8.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Slider(
                                        value = playheadMs.toFloat().coerceIn(0f, editState.totalDurationMs.toFloat()),
                                        onValueChange = { seekTimelineTo(it.toLong()) },
                                        valueRange = 0f..editState.totalDurationMs.toFloat().coerceAtLeast(500f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = glassConfig.accentColor,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("video_scrubber")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = DateGroupingUtils.formatDuration(playheadMs),
                                        style = MaterialTheme.typography.labelMedium
                                            .copy(fontWeight = FontWeight.Bold)
                                            .withGlassLegibility(glassConfig),
                                        color = glassConfig.contentPrimaryColor
                                    )
                                }
                            }
                        }
                    }

                    // First-time beginner tip (skippable, shown once per tab)
                    val tipKey = "video_${activeTab.name}"
                    val (tipTitle, tipBody) = remember(activeTab) { videoTabTip(activeTab) }
                    EditorTooltipBanner(
                        visible = !isSliderDragging && !skipAllEditorTips && tipKey !in seenEditorTips,
                        title = tipTitle,
                        message = tipBody,
                        onDismissTip = { viewModel.markTipSeen(activeTab) },
                        onSkipAllTips = viewModel::skipAllTips
                    )

                    // Multi-Clip Timeline Container rendered as a frosted Glass Strip
                    AnimatedVisibility(
                        visible = !isSliderDragging,
                        enter = motion.bottomSheetEnter(),
                        exit = motion.bottomSheetExit()
                    ) {
                        MultiClipTimelineBar(
                            clips = editState.clips,
                            selectedClipId = editState.selectedClip?.id,
                            playheadMs = playheadMs,
                            totalDurationMs = editState.totalDurationMs,
                            clipThumbnails = clipThumbnails,
                            onSelectClip = viewModel::selectClip,
                            onMoveClip = viewModel::moveClip,
                            onSeekTo = seekTimelineTo,
                            onPreviewTrim = viewModel::previewTrimSelectedClip,
                            onCommitTrim = viewModel::commitSliderChange,
                            onAddClip = {
                                addClipLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        )
                    }

                    // Active Tool Glass Sheet — stays mounted during slider drags so the drag gesture completes smoothly
                    val selectedClip = editState.selectedClip
                    val selectedIndex = editState.clips.indexOfFirst { it.id == selectedClip?.id }.coerceAtLeast(0)

                    GlassPanel(
                        shape = RoundedCornerShape(26.dp),
                        strongFill = !isSliderDragging,
                        enableBlur = true,
                        elevation = if (isSliderDragging) 8.dp else 18.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassSheetMotion()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = if (isSliderDragging) 8.dp else 12.dp
                                )
                        ) {
                            AnimatedContent(
                                targetState = activeTab,
                                transitionSpec = {
                                    if (motion.prefersReducedMotion) {
                                        motion.fadeEnter(MotionTokens.MicroMs) togetherWith motion.fadeExit(MotionTokens.MicroFastMs)
                                    } else {
                                        (motion.fadeEnter(MotionTokens.StandardMs) + slideInVertically(motion.uiTween()) { it / 6 }) togetherWith
                                            (motion.fadeExit(MotionTokens.MicroMs) + slideOutVertically(motion.exitTween()) { -it / 6 })
                                    }
                                },
                                label = "video_editor_panel"
                            ) { tab ->
                                when (tab) {
                                    VideoEditorTab.TRIM -> VideoTrimPanel(
                                        selectedClip = selectedClip,
                                        selectedIndex = selectedIndex,
                                        clipCount = editState.clips.size,
                                        aspectRatio = editState.aspectRatio,
                                        onAddClip = {
                                            addClipLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        onSplitAtPlayhead = viewModel::splitAtPlayhead,
                                        onMoveLeft = { viewModel.moveClip(selectedIndex, (selectedIndex - 1).coerceAtLeast(0)) },
                                        onMoveRight = {
                                            viewModel.moveClip(
                                                selectedIndex,
                                                (selectedIndex + 1).coerceAtMost(editState.clips.lastIndex)
                                            )
                                        },
                                        onDeleteClip = viewModel::deleteSelectedClip,
                                        onSelectAspect = viewModel::setAspectRatio,
                                        onRotate90 = viewModel::rotateSelectedClip,
                                        onFlip = viewModel::flipSelectedClip
                                    )

                                    VideoEditorTab.SPEED -> VideoSpeedPanel(
                                        selectedClip = selectedClip,
                                        isSliderDragging = isSliderDragging,
                                        onSliderDraggingChange = viewModel::setSliderDragging,
                                        onApplyWholeClipSpeed = viewModel::setSelectedClipSpeed,
                                        onApplySegmentSpeed = viewModel::applySpeedToSegment,
                                        onToggleStabilize = viewModel::toggleStabilizeSelectedClip
                                    )

                                    VideoEditorTab.FILTERS -> VideoFiltersPanel(
                                        color = editState.colorAdjustments,
                                        filterThumbnails = filterThumbnails,
                                        isSliderDragging = isSliderDragging,
                                        onSliderDraggingChange = viewModel::setSliderDragging,
                                        onSelectFilter = viewModel::selectFilter,
                                        onPreviewColor = viewModel::previewColorAdjustments,
                                        onCommitColor = viewModel::commitSliderChange,
                                        onResetFilterIntensity = viewModel::resetFilterIntensity,
                                        onResetColorParam = viewModel::resetColorParam,
                                        onApplyTemplate = { preset ->
                                            viewModel.applyTemplate(preset)
                                            glassToastMessage = "${preset.title} template applied"
                                        }
                                    )

                                    VideoEditorTab.TEXT -> VideoTextPanel(
                                        overlays = editState.textOverlays,
                                        selectedOverlayId = selectedTextOverlayId,
                                        totalDurationMs = editState.totalDurationMs,
                                        onAddNewText = {
                                            val fresh = VideoTextOverlay(
                                                text = "",
                                                startMs = playheadMs,
                                                endMs = (playheadMs + 3500L).coerceAtMost(editState.totalDurationMs)
                                            )
                                            overlayTextDraft = ""
                                            editingOverlay = fresh
                                        },
                                        onSelectOverlay = { selectedTextOverlayId = it.id },
                                        onEditOverlayText = {
                                            overlayTextDraft = it.text
                                            editingOverlay = it
                                        },
                                        onUpdateOverlay = viewModel::addOrUpdateTextOverlay,
                                        onDeleteOverlay = viewModel::deleteTextOverlay
                                    )

                                    VideoEditorTab.MUSIC -> VideoMusicPanel(
                                        builtInTracks = builtInTracks,
                                        selectedMusic = editState.selectedMusic,
                                        onSelectMusic = viewModel::selectMusicTrack,
                                        onPickDeviceAudio = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                                        onPreviewMusicTrim = viewModel::previewMusicTrim,
                                        onCommitChanges = viewModel::commitSliderChange
                                    )

                                    VideoEditorTab.AUDIO -> VideoAudioPanel(
                                        hasMusicSelected = editState.selectedMusic != null,
                                        originalVolume = editState.originalVolume,
                                        musicVolume = editState.musicVolume,
                                        isSliderDragging = isSliderDragging,
                                        onSliderDraggingChange = viewModel::setSliderDragging,
                                        onPreviewOriginalVolume = viewModel::previewOriginalVolume,
                                        onPreviewMusicVolume = viewModel::previewMusicVolume,
                                        onResetOriginalVolume = viewModel::resetOriginalVolume,
                                        onResetMusicVolume = viewModel::resetMusicVolume,
                                        onApplyPreset = viewModel::setAudioPreset,
                                        onCommitChanges = viewModel::commitSliderChange
                                    )
                                }
                            }
                        }
                    }

                    // Floating Bottom Pill Tab Bar
                    AnimatedVisibility(
                        visible = !isSliderDragging,
                        enter = motion.bottomSheetEnter(),
                        exit = motion.bottomSheetExit()
                    ) {
                        EditorBottomTabBar(
                            tabs = videoTabs,
                            selectedTab = activeTab,
                            onSelectTab = viewModel::selectTab
                        )
                    }
                }
            }

            if (showDiscardDialog) {
                UnsavedChangesDialog(
                    onKeepEditing = { showDiscardDialog = false },
                    onDiscardChanges = {
                        showDiscardDialog = false
                        onCancel()
                    }
                )
            }

            // Glass Add / Edit Text Overlay Dialog
            editingOverlay?.let { item ->
                GlassDialogShell(
                    onDismissRequest = { editingOverlay = null },
                    title = "Text Overlay"
                ) {
                    OutlinedTextField(
                        value = overlayTextDraft,
                        onValueChange = { overlayTextDraft = it },
                        placeholder = { Text("Enter caption or title", color = glassConfig.contentSecondaryColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = glassConfig.contentPrimaryColor,
                            unfocusedTextColor = glassConfig.contentPrimaryColor,
                            focusedBorderColor = glassConfig.accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                            cursorColor = glassConfig.accentColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editState.textOverlays.any { it.id == item.id }) {
                            IconButton(
                                onClick = {
                                    viewModel.deleteTextOverlay(item.id)
                                    editingOverlay = null
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { editingOverlay = null },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Cancel", color = glassConfig.contentSecondaryColor)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        GlassButton(
                            label = "Apply",
                            active = true,
                            enableBlur = false,
                            onClick = {
                                val trimmed = overlayTextDraft.trim()
                                if (trimmed.isNotEmpty()) {
                                    val updated = item.copy(text = trimmed)
                                    viewModel.addOrUpdateTextOverlay(updated)
                                    selectedTextOverlayId = updated.id
                                }
                                editingOverlay = null
                            }
                        )
                    }
                }
            }

            // Glass Export Resolution Picker & Live Progress Dialog
            if (showExportDialog || exportProgress != null || exportError != null) {
                val prog = exportProgress
                GlassDialogShell(
                    onDismissRequest = {
                        if (exportProgress == null) {
                            showExportDialog = false
                            viewModel.clearExportError()
                        }
                    },
                    title = when {
                        prog != null -> "Exporting Video…"
                        exportError != null -> "Export Error"
                        else -> "Export Quality"
                    }
                ) {
                    when {
                        prog != null -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                LinearProgressIndicator(
                                    progress = { prog.coerceIn(0f, 1f) },
                                    color = glassConfig.accentColor,
                                    trackColor = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .testTag("video_export_progress")
                                )
                                Text(
                                    text = "${(prog * 100f).roundToInt()}%  •  Rendering effects & audio",
                                    style = MaterialTheme.typography.bodySmall.withGlassLegibility(glassConfig),
                                    color = glassConfig.contentSecondaryColor
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.cancelExport() },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("video_export_cancel")
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        exportError != null -> {
                            Text(
                                text = exportError ?: "Export failed",
                                style = MaterialTheme.typography.bodyMedium.withGlassLegibility(glassConfig),
                                color = glassConfig.contentSecondaryColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        showExportDialog = false
                                        viewModel.clearExportError()
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Close", color = glassConfig.contentSecondaryColor)
                                }
                            }
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VideoExportResolution.entries.forEach { res ->
                                    val isRecommended = res.targetHeight == 1080
                                    val resInteraction = remember { MutableInteractionSource() }
                                    GlassPanel(
                                        shape = RoundedCornerShape(16.dp),
                                        active = isRecommended,
                                        strongFill = isRecommended,
                                        enableBlur = false,
                                        elevation = if (isRecommended) 8.dp else 4.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 54.dp)
                                            .tactilePressMotion(resInteraction, squashAmount = 0.035f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable(
                                                interactionSource = resInteraction,
                                                indication = LocalIndication.current,
                                                role = Role.Button
                                            ) {
                                                showExportDialog = false
                                                viewModel.exportVideo(res) { file ->
                                                    glassToastMessage = "Saved video to gallery"
                                                    onPublishExportedVideo(file, displayName) {
                                                        runCatching { file.delete() }
                                                    }
                                                }
                                            }
                                            .testTag("export_res_${res.targetHeight}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = res.label,
                                                    style = MaterialTheme.typography.titleSmall
                                                        .copy(fontWeight = FontWeight.Bold)
                                                        .withGlassLegibility(glassConfig),
                                                    color = glassConfig.contentPrimaryColor
                                                )
                                                if (isRecommended) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(glassConfig.accentColor.copy(alpha = 0.24f))
                                                            .border(1.dp, glassConfig.accentColor.copy(alpha = 0.70f), CircleShape)
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Recommended",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = glassConfig.accentColor
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = res.bitrateHint,
                                                style = MaterialTheme.typography.labelMedium
                                                    .copy(fontWeight = FontWeight.Bold)
                                                    .withGlassLegibility(glassConfig),
                                                color = if (isRecommended) glassConfig.contentPrimaryColor else glassConfig.accentColor
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        showExportDialog = false
                                        viewModel.clearExportError()
                                    },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Close", color = glassConfig.contentSecondaryColor)
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun videoTabTip(tab: VideoEditorTab): Pair<String, String> = when (tab) {
    VideoEditorTab.TRIM -> "Trim & Arrange Clips" to "Drag the blue clip handles to trim with magnetic snap, pinch to zoom the timeline, or tap Split."
    VideoEditorTab.SPEED -> "Speed & Stabilization" to "Slow down or speed up your clip from 0.25x to 4x, or turn on Smooth Shaky Video."
    VideoEditorTab.FILTERS -> "Looks, Color & Templates" to "Try one-tap Filters with live thumbnails, fine-tune Color, or apply an instant Template."
    VideoEditorTab.TEXT -> "Animated Titles" to "Add captions, drag them directly on the video, and set when they appear."
    VideoEditorTab.MUSIC -> "Add Soundtrack" to "Pick a built-in track or import music from your phone, then trim the best part."
    VideoEditorTab.AUDIO -> "Balance Sound Levels" to "Mix original video sound and background music, or mute the original clip in one tap."
}
