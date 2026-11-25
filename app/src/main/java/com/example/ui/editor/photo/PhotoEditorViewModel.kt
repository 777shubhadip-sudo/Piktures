package com.example.ui.editor.photo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.ui.editor.common.sampleBitmapLuminance
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var loadedUri: Uri? = null

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    private val _toolsProcessedBitmap = MutableStateFlow<Bitmap?>(null)
    val toolsProcessedBitmap: StateFlow<Bitmap?> = _toolsProcessedBitmap.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    private val _filterThumbnails = MutableStateFlow<Map<PhotoFilterPreset, Bitmap>>(emptyMap())
    val filterThumbnails: StateFlow<Map<PhotoFilterPreset, Bitmap>> = _filterThumbnails.asStateFlow()

    private val _adjustThumbnails = MutableStateFlow<Map<AdjustParameter, Bitmap>>(emptyMap())
    val adjustThumbnails: StateFlow<Map<AdjustParameter, Bitmap>> = _adjustThumbnails.asStateFlow()

    private val _suggestionThumbnails = MutableStateFlow<Map<PhotoSuggestionPreset, Bitmap>>(emptyMap())
    val suggestionThumbnails: StateFlow<Map<PhotoSuggestionPreset, Bitmap>> = _suggestionThumbnails.asStateFlow()

    private val _state = MutableStateFlow(PhotoEditState())
    val state: StateFlow<PhotoEditState> = _state.asStateFlow()

    val hasUnsavedChanges: StateFlow<Boolean> = _state
        .map { it.hasChanges }
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

    @OptIn(FlowPreview::class)
    val debouncedRenderState: StateFlow<PhotoEditState> = _state
        .debounce { if (sliderDragBaseState != null) 24L else 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PhotoEditState())

    private val _undoStack = MutableStateFlow<List<PhotoEditState>>(emptyList())
    private val _redoStack = MutableStateFlow<List<PhotoEditState>>(emptyList())

    val canUndo: StateFlow<Boolean> = combine(_undoStack) { it.first().isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val canRedo: StateFlow<Boolean> = combine(_redoStack) { it.first().isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _activeTab = MutableStateFlow(PhotoEditorTab.SUGGESTIONS)
    val activeTab: StateFlow<PhotoEditorTab> = _activeTab.asStateFlow()

    private val _selectedAdjustParam = MutableStateFlow(AdjustParameter.BRIGHTNESS)
    val selectedAdjustParam: StateFlow<AdjustParameter> = _selectedAdjustParam.asStateFlow()

    private val _isComparingOriginal = MutableStateFlow(false)
    val isComparingOriginal: StateFlow<Boolean> = _isComparingOriginal.asStateFlow()

    // Markup sub-controls
    private val _markupTool = MutableStateFlow(MarkupTool.PEN)
    val markupTool: StateFlow<MarkupTool> = _markupTool.asStateFlow()

    private val _markupColor = MutableStateFlow(MARKUP_PALETTE_COLORS[2])
    val markupColor: StateFlow<Long> = _markupColor.asStateFlow()

    private val _markupWidthFraction = MutableStateFlow(0.012f)
    val markupWidthFraction: StateFlow<Float> = _markupWidthFraction.asStateFlow()

    private val _markupFontStyle = MutableStateFlow(MarkupFontStyle.SANS)
    val markupFontStyle: StateFlow<MarkupFontStyle> = _markupFontStyle.asStateFlow()

    private val _markupTextSizeSp = MutableStateFlow(28f)
    val markupTextSizeSp: StateFlow<Float> = _markupTextSizeSp.asStateFlow()

    // Tools sub-controls
    private val _magicEraserMode = MutableStateFlow(false)
    val magicEraserMode: StateFlow<Boolean> = _magicEraserMode.asStateFlow()

    private val _eraserRadiusFraction = MutableStateFlow(0.045f)
    val eraserRadiusFraction: StateFlow<Float> = _eraserRadiusFraction.asStateFlow()

    private var sliderDragBaseState: PhotoEditState? = null
    private var loadJob: Job? = null
    private var toolsJob: Job? = null

    fun loadImage(uri: Uri, forceReload: Boolean = false) {
        if (!forceReload && loadedUri == uri && _sourceBitmap.value != null) return
        loadJob?.cancel()
        toolsJob?.cancel()
        loadedUri = uri
        _loadFailed.value = false
        _sourceBitmap.value = null
        _toolsProcessedBitmap.value = null
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        _state.value = PhotoEditState()

        loadJob = viewModelScope.launch {
            // Decode downscaled working preview (~1280px max edge) for instant GPU/tool editing;
            // full resolution (4096px max edge) is decoded only on Save.
            val loaded = withContext(Dispatchers.IO) {
                PhotoGpuRenderer.loadOrientedBitmap(context, uri, maxEdge = 1280)
            }
            if (loaded != null) {
                _sourceBitmap.value = loaded
                _toolsProcessedBitmap.value = loaded
                val filtersDeferred = async(Dispatchers.Default) {
                    PhotoGpuRenderer.generateFilterThumbnails(loaded)
                }
                val adjustDeferred = async(Dispatchers.Default) {
                    PhotoGpuRenderer.generateAdjustThumbnails(loaded)
                }
                val suggestDeferred = async(Dispatchers.Default) {
                    PhotoGpuRenderer.generateSuggestionThumbnails(loaded)
                }
                val lumDeferred = async(Dispatchers.Default) {
                    sampleBitmapLuminance(loaded)
                }
                _filterThumbnails.value = filtersDeferred.await()
                _adjustThumbnails.value = adjustDeferred.await()
                _suggestionThumbnails.value = suggestDeferred.await()
                _backdropLuminance.value = lumDeferred.await()
            } else {
                _loadFailed.value = true
            }
        }
    }

    fun selectTab(tab: PhotoEditorTab) {
        _activeTab.value = tab
        if (tab != PhotoEditorTab.TOOLS) {
            _magicEraserMode.value = false
        }
    }

    fun markTipSeen(tab: PhotoEditorTab) {
        viewModelScope.launch { settingsRepository.markEditorTipSeen("photo_${tab.name}") }
    }

    fun skipAllTips() {
        viewModelScope.launch { settingsRepository.skipAllEditorTips() }
    }

    fun setComparingOriginal(comparing: Boolean) {
        _isComparingOriginal.value = comparing
    }

    fun setSliderDragging(dragging: Boolean) {
        _isSliderDragging.value = dragging
    }

    private fun commitNewState(newState: PhotoEditState, baseForUndo: PhotoEditState = _state.value) {
        if (newState == baseForUndo) return
        val nextUndo = (_undoStack.value + baseForUndo).takeLast(40)
        _undoStack.value = nextUndo
        _redoStack.value = emptyList()
        _state.value = newState
        if (newState.tools != baseForUndo.tools) {
            recomputeToolsBitmap(newState.tools, debounceMs = 0L)
        }
    }

    fun undo() {
        val stack = _undoStack.value
        if (stack.isEmpty()) return
        val previous = stack.last()
        val current = _state.value
        _undoStack.value = stack.dropLast(1)
        _redoStack.value = (_redoStack.value + current).takeLast(40)
        _state.value = previous
        if (previous.tools != current.tools) {
            recomputeToolsBitmap(previous.tools, debounceMs = 0L)
        }
    }

    fun redo() {
        val stack = _redoStack.value
        if (stack.isEmpty()) return
        val next = stack.last()
        val current = _state.value
        _redoStack.value = stack.dropLast(1)
        _undoStack.value = (_undoStack.value + current).takeLast(40)
        _state.value = next
        if (next.tools != current.tools) {
            recomputeToolsBitmap(next.tools, debounceMs = 0L)
        }
    }

    private fun recomputeToolsBitmap(tools: PhotoToolsState, debounceMs: Long = 0L) {
        val src = _sourceBitmap.value ?: return
        toolsJob?.cancel()
        toolsJob = viewModelScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            val updated = withContext(Dispatchers.Default) {
                PhotoGpuRenderer.applyPixelTools(src, tools)
            }
            _toolsProcessedBitmap.value = updated
        }
    }

    // --- Suggestions ---
    fun applySuggestion(preset: PhotoSuggestionPreset) {
        val current = _state.value
        val next = current.copy(
            activeSuggestion = preset,
            adjustments = preset.adjustments,
            filter = preset.filter,
            filterIntensity = preset.filterIntensity,
            tools = current.tools.copy(autoEnhanced = preset.autoEnhanced)
        )
        commitNewState(next)
    }

    // --- Crop & Transform ---
    fun rotate90() {
        val current = _state.value
        val nextCrop = current.crop.copy(
            rotation90 = (current.crop.rotation90 + 90) % 360,
            left = 0f,
            top = 0f,
            right = 1f,
            bottom = 1f,
            aspectRatio = PhotoAspectRatio.FREE
        )
        commitNewState(current.copy(crop = nextCrop))
    }

    fun flipHorizontal() {
        val current = _state.value
        val c = current.crop
        val nextCrop = c.copy(
            flipHorizontal = !c.flipHorizontal,
            left = 1f - c.right,
            right = 1f - c.left
        )
        commitNewState(current.copy(crop = nextCrop))
    }

    fun selectAspectRatio(aspect: PhotoAspectRatio, previewWidth: Int, previewHeight: Int) {
        val current = _state.value
        val ratio = aspect.ratio
        val nextCrop = if (ratio == null || previewWidth <= 0 || previewHeight <= 0) {
            current.crop.copy(aspectRatio = aspect, left = 0f, top = 0f, right = 1f, bottom = 1f)
        } else {
            val imgRatio = previewWidth.toFloat() / previewHeight.toFloat()
            if (ratio > imgRatio) {
                val hFrac = (imgRatio / ratio).coerceIn(0.2f, 1f)
                val t = (1f - hFrac) / 2f
                current.crop.copy(aspectRatio = aspect, left = 0f, right = 1f, top = t, bottom = 1f - t)
            } else {
                val wFrac = (ratio / imgRatio).coerceIn(0.2f, 1f)
                val l = (1f - wFrac) / 2f
                current.crop.copy(aspectRatio = aspect, top = 0f, bottom = 1f, left = l, right = 1f - l)
            }
        }
        commitNewState(current.copy(crop = nextCrop))
    }

    fun previewCropBounds(l: Float, t: Float, r: Float, b: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        val c = _state.value.crop
        _state.value = _state.value.copy(crop = c.copy(left = l, top = t, right = r, bottom = b))
    }

    fun commitCropBounds() {
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun previewStraighten(degrees: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        _state.value = _state.value.copy(crop = _state.value.crop.copy(straightenDegrees = degrees.coerceIn(-45f, 45f)))
    }

    fun commitStraighten() {
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun resetStraighten() {
        val current = _state.value
        if (current.crop.straightenDegrees == 0f) return
        commitNewState(current.copy(crop = current.crop.copy(straightenDegrees = 0f)))
    }

    // --- Adjust ---
    fun selectAdjustParam(param: AdjustParameter) {
        _selectedAdjustParam.value = param
    }

    fun previewAdjustment(param: AdjustParameter, value: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        _state.value = _state.value.copy(
            adjustments = _state.value.adjustments.set(param, value),
            activeSuggestion = PhotoSuggestionPreset.NONE
        )
    }

    fun commitAdjustment() {
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun resetAdjustment(param: AdjustParameter) {
        val current = _state.value
        if (current.adjustments.get(param) == 0f) return
        commitNewState(current.copy(adjustments = current.adjustments.set(param, 0f)))
    }

    // --- Filters ---
    fun selectFilter(preset: PhotoFilterPreset) {
        val current = _state.value
        commitNewState(
            current.copy(
                filter = preset,
                filterIntensity = if (preset == PhotoFilterPreset.ORIGINAL) 1f else current.filterIntensity,
                activeSuggestion = PhotoSuggestionPreset.NONE
            )
        )
    }

    fun previewFilterIntensity(intensity: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        _state.value = _state.value.copy(filterIntensity = intensity.coerceIn(0f, 1f))
    }

    fun commitFilterIntensity() {
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun resetFilterIntensity() {
        val current = _state.value
        if (current.filterIntensity == 1f) return
        commitNewState(current.copy(filterIntensity = 1f))
    }

    // --- Markup ---
    fun selectMarkupTool(tool: MarkupTool) {
        _markupTool.value = tool
    }

    fun selectMarkupColor(colorArgb: Long) {
        _markupColor.value = colorArgb
    }

    fun setMarkupWidthFraction(fraction: Float) {
        _markupWidthFraction.value = fraction.coerceIn(0.004f, 0.045f)
    }

    fun selectMarkupFont(font: MarkupFontStyle) {
        _markupFontStyle.value = font
    }

    fun setMarkupTextSizeSp(sizeSp: Float) {
        _markupTextSizeSp.value = sizeSp.coerceIn(14f, 54f)
    }

    fun addMarkupStroke(points: List<NormalizedPoint>) {
        if (points.isEmpty()) return
        val tool = _markupTool.value
        val stroke = MarkupStroke(
            points = points,
            colorArgb = _markupColor.value,
            widthFraction = when (tool) {
                MarkupTool.HIGHLIGHTER -> _markupWidthFraction.value * 2.4f
                MarkupTool.ERASER -> _markupWidthFraction.value * 2.6f
                else -> _markupWidthFraction.value
            },
            alpha = if (tool == MarkupTool.HIGHLIGHTER) 0.42f else 1f,
            isEraser = tool == MarkupTool.ERASER
        )
        val current = _state.value
        commitNewState(current.copy(markupStrokes = current.markupStrokes + stroke))
    }

    fun addOrUpdateMarkupText(item: MarkupTextItem) {
        val current = _state.value
        val existingIdx = current.markupTexts.indexOfFirst { it.id == item.id }
        val updatedList = if (existingIdx >= 0) {
            current.markupTexts.toMutableList().apply { set(existingIdx, item) }
        } else {
            current.markupTexts + item
        }
        commitNewState(current.copy(markupTexts = updatedList))
    }

    fun moveMarkupText(id: String, normX: Float, normY: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        val updated = _state.value.markupTexts.map {
            if (it.id == id) it.copy(normX = normX.coerceIn(0.05f, 0.95f), normY = normY.coerceIn(0.05f, 0.95f))
            else it
        }
        _state.value = _state.value.copy(markupTexts = updated)
    }

    fun commitMoveMarkupText() {
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun deleteMarkupText(id: String) {
        val current = _state.value
        commitNewState(current.copy(markupTexts = current.markupTexts.filterNot { it.id == id }))
    }

    fun createNewMarkupText(text: String): MarkupTextItem = MarkupTextItem(
        id = UUID.randomUUID().toString(),
        text = text,
        normX = 0.5f,
        normY = 0.5f,
        colorArgb = _markupColor.value,
        sizeSp = _markupTextSizeSp.value,
        fontStyle = _markupFontStyle.value
    )

    // --- Tools (Auto Enhance, Blur Background, Magic Eraser, Unblur/Denoise) ---
    fun toggleAutoEnhance() {
        val current = _state.value
        val nextAuto = !current.tools.autoEnhanced
        val preset = PhotoSuggestionPreset.AUTO_ENHANCE
        val next = if (nextAuto) {
            current.copy(
                activeSuggestion = preset,
                adjustments = preset.adjustments,
                filter = preset.filter,
                filterIntensity = preset.filterIntensity,
                tools = current.tools.copy(autoEnhanced = true)
            )
        } else {
            current.copy(
                activeSuggestion = PhotoSuggestionPreset.NONE,
                adjustments = PhotoAdjustments(),
                filter = PhotoFilterPreset.ORIGINAL,
                filterIntensity = 1f,
                tools = current.tools.copy(autoEnhanced = false)
            )
        }
        commitNewState(next)
    }

    fun previewBackgroundBlur(amount: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        val nextTools = _state.value.tools.copy(backgroundBlurAmount = amount.coerceIn(0f, 1f))
        _state.value = _state.value.copy(tools = nextTools)
        recomputeToolsBitmap(nextTools, debounceMs = 85L)
    }

    fun commitBackgroundBlur() {
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun resetBackgroundBlur() {
        val current = _state.value
        if (current.tools.backgroundBlurAmount == 0f) return
        commitNewState(current.copy(tools = current.tools.copy(backgroundBlurAmount = 0f)))
    }

    fun previewDenoiseUnblur(strength: Float) {
        if (sliderDragBaseState == null) sliderDragBaseState = _state.value
        val nextTools = _state.value.tools.copy(denoiseUnblurStrength = strength.coerceIn(0f, 1f))
        _state.value = _state.value.copy(tools = nextTools)
        recomputeToolsBitmap(nextTools, debounceMs = 85L)
    }

    fun commitDenoiseUnblur() {
        val base = sliderDragBaseState ?: return
        sliderDragBaseState = null
        commitNewState(_state.value, baseForUndo = base)
    }

    fun resetDenoiseUnblur() {
        val current = _state.value
        if (current.tools.denoiseUnblurStrength == 0f) return
        commitNewState(current.copy(tools = current.tools.copy(denoiseUnblurStrength = 0f)))
    }

    fun setMagicEraserMode(enabled: Boolean) {
        _magicEraserMode.value = enabled
    }

    fun setEraserRadiusFraction(radius: Float) {
        _eraserRadiusFraction.value = radius.coerceIn(0.02f, 0.09f)
    }

    fun addMagicEraserStroke(points: List<NormalizedPoint>) {
        if (points.isEmpty()) return
        val current = _state.value
        val stroke = MagicEraserStroke(
            points = points,
            radiusFraction = _eraserRadiusFraction.value
        )
        commitNewState(
            current.copy(tools = current.tools.copy(eraserStrokes = current.tools.eraserStrokes + stroke))
        )
    }

    fun clearMagicEraser() {
        val current = _state.value
        if (current.tools.eraserStrokes.isEmpty()) return
        commitNewState(current.copy(tools = current.tools.copy(eraserStrokes = emptyList())))
    }

    suspend fun renderHighResForSave(): Bitmap? {
        val uri = loadedUri ?: return null
        val currentState = _state.value
        return withContext(Dispatchers.Default) {
            val full = PhotoGpuRenderer.loadOrientedBitmap(context, uri, maxEdge = 4096)
            val sourceForRender = full ?: _sourceBitmap.value ?: return@withContext null
            val rendered = PhotoGpuRenderer.renderFinalBitmap(sourceForRender, currentState)
            if (full != null && rendered !== full) {
                full.recycle()
            }
            rendered
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        toolsJob?.cancel()
        _filterThumbnails.value = emptyMap()
        _adjustThumbnails.value = emptyMap()
        _suggestionThumbnails.value = emptyMap()
        _toolsProcessedBitmap.value = null
        _sourceBitmap.value = null
        super.onCleared()
    }
}
