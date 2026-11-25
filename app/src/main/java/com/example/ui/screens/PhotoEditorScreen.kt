package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.editor.photo.AdjustPanel
import com.example.ui.editor.photo.CropPanel
import com.example.ui.editor.photo.FiltersPanel
import com.example.ui.editor.photo.MarkupFontStyle
import com.example.ui.editor.photo.MarkupPanel
import com.example.ui.editor.photo.MarkupTextItem
import com.example.ui.editor.photo.MarkupTool
import com.example.ui.editor.photo.NormalizedPoint
import com.example.ui.editor.photo.PhotoCropState
import com.example.ui.editor.photo.PhotoEditorTab
import com.example.ui.editor.photo.PhotoEditorViewModel
import com.example.ui.editor.photo.PhotoGpuRenderer
import com.example.ui.editor.photo.SuggestionsPanel
import com.example.ui.editor.photo.ToolsPanel
import com.example.ui.editor.photo.toComposeColor
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.MotionTokens
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PhotoEditorScreen(
    uri: Uri,
    displayName: String,
    onCancel: () -> Unit,
    onSaveCopy: (Bitmap, String, () -> Unit) -> Unit,
    onOverwriteOriginal: (Uri, Bitmap, () -> Unit) -> Unit,
    viewModel: PhotoEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val backdropLuminance by viewModel.backdropLuminance.collectAsStateWithLifecycle()
    val isSliderDragging by viewModel.isSliderDragging.collectAsStateWithLifecycle()

    val sourceBitmap by viewModel.sourceBitmap.collectAsStateWithLifecycle()
    val toolsProcessedBitmap by viewModel.toolsProcessedBitmap.collectAsStateWithLifecycle()
    val loadFailed by viewModel.loadFailed.collectAsStateWithLifecycle()
    val filterThumbnails by viewModel.filterThumbnails.collectAsStateWithLifecycle()
    val adjustThumbnails by viewModel.adjustThumbnails.collectAsStateWithLifecycle()
    val suggestionThumbnails by viewModel.suggestionThumbnails.collectAsStateWithLifecycle()
    val editState by viewModel.state.collectAsStateWithLifecycle()
    val renderState by viewModel.debouncedRenderState.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedAdjustParam by viewModel.selectedAdjustParam.collectAsStateWithLifecycle()
    val isComparingOriginal by viewModel.isComparingOriginal.collectAsStateWithLifecycle()
    val seenEditorTips by viewModel.seenEditorTips.collectAsStateWithLifecycle()
    val skipAllEditorTips by viewModel.skipAllEditorTips.collectAsStateWithLifecycle()

    val markupTool by viewModel.markupTool.collectAsStateWithLifecycle()
    val markupColor by viewModel.markupColor.collectAsStateWithLifecycle()
    val markupWidthFraction by viewModel.markupWidthFraction.collectAsStateWithLifecycle()
    val markupFontStyle by viewModel.markupFontStyle.collectAsStateWithLifecycle()
    val markupTextSizeSp by viewModel.markupTextSizeSp.collectAsStateWithLifecycle()

    val magicEraserMode by viewModel.magicEraserMode.collectAsStateWithLifecycle()
    val eraserRadiusFraction by viewModel.eraserRadiusFraction.collectAsStateWithLifecycle()

    var isSaving by remember { mutableStateOf(false) }
    var showSaveChoiceDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var editingTextItem by remember { mutableStateOf<MarkupTextItem?>(null) }
    var textInputDraft by remember { mutableStateOf("") }
    var glassToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(glassToastMessage) {
        if (glassToastMessage != null) {
            delay(2000L)
            glassToastMessage = null
        }
    }

    LaunchedEffect(uri) {
        viewModel.loadImage(uri)
    }

    val handleExitRequest = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onCancel()
        }
    }

    BackHandler(enabled = !isSaving) {
        handleExitRequest()
    }

    // Apply discrete 90° rotation & flip off Main thread; straighten is handled on GPU via graphicsLayer.
    val orientedBase by produceState<Bitmap?>(
        initialValue = toolsProcessedBitmap,
        toolsProcessedBitmap,
        editState.crop.rotation90,
        editState.crop.flipHorizontal
    ) {
        val bmp = toolsProcessedBitmap
        if (bmp == null) {
            value = null
            return@produceState
        }
        val discreteCrop = editState.crop.copy(straightenDegrees = 0f)
        value = withContext(Dispatchers.Default) {
            PhotoGpuRenderer.applyGeometryAndCrop(bmp, discreteCrop, applyCropBounds = false)
        }
    }

    val isCropTab = activeTab == PhotoEditorTab.CROP
    val croppedBitmap by produceState<Bitmap?>(
        initialValue = orientedBase,
        orientedBase,
        isCropTab,
        editState.crop.left,
        editState.crop.top,
        editState.crop.right,
        editState.crop.bottom
    ) {
        val bmp = orientedBase
        if (bmp == null) {
            value = null
            return@produceState
        }
        if (isCropTab || !editState.crop.hasActiveBoundsCrop) {
            value = bmp
        } else {
            val crop = editState.crop
            value = withContext(Dispatchers.Default) {
                val x = (crop.left * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                val y = (crop.top * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                val w = ((crop.right - crop.left) * bmp.width).toInt().coerceIn(1, bmp.width - x)
                val h = ((crop.bottom - crop.top) * bmp.height).toInt().coerceIn(1, bmp.height - y)
                Bitmap.createBitmap(bmp, x, y, w, h)
            }
        }
    }

    val displayedBitmap = if (isComparingOriginal) sourceBitmap else croppedBitmap

    val fallbackColorMatrix = remember(
        renderState.adjustments,
        renderState.filter,
        renderState.filterIntensity,
        isComparingOriginal
    ) {
        if (isComparingOriginal) null
        else PhotoGpuRenderer.toComposeColorMatrix(
            PhotoGpuRenderer.buildFallbackColorMatrixValues(
                renderState.adjustments,
                renderState.filter,
                renderState.filterIntensity
            )
        )
    }

    val photoTabs = remember {
        listOf(
            EditorTabUi(PhotoEditorTab.SUGGESTIONS, "Suggestions", Icons.Rounded.AutoAwesome, "photo_tab_suggestions"),
            EditorTabUi(PhotoEditorTab.CROP, "Crop", Icons.Rounded.Crop, "photo_tab_crop"),
            EditorTabUi(PhotoEditorTab.ADJUST, "Adjust", Icons.Rounded.Tune, "photo_tab_adjust"),
            EditorTabUi(PhotoEditorTab.FILTERS, "Filters", Icons.Rounded.FilterVintage, "photo_tab_filters"),
            EditorTabUi(PhotoEditorTab.MARKUP, "Markup", Icons.Rounded.Brush, "photo_tab_markup"),
            EditorTabUi(PhotoEditorTab.TOOLS, "Tools", Icons.Rounded.WbSunny, "photo_tab_tools")
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
        with(density) { navBottomPx.toDp() } + 230.dp
    }
    val topStagePadding by animateDpAsState(
        targetValue = targetTopStagePadding,
        animationSpec = if (motion.prefersReducedMotion) snap() else MotionTokens.SnappyDpSpring,
        label = "photo_stage_top_pad"
    )
    val bottomStagePadding by animateDpAsState(
        targetValue = targetBottomStagePadding,
        animationSpec = if (motion.prefersReducedMotion) snap() else MotionTokens.SnappyDpSpring,
        label = "photo_stage_bottom_pad"
    )

    CompositionLocalProvider(LocalEditorGlassConfig provides glassConfig) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(canvasBackground)
            ) {
                // Layer 0: Photo Canvas framed in the unobstructed viewport above the edit tools
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = editorHazeState, zIndex = 0f)
                        .padding(
                            top = topStagePadding,
                            bottom = bottomStagePadding,
                            start = 14.dp,
                            end = 14.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = displayedBitmap
                    when {
                        loadFailed -> {
                            GlassPanel(
                                shape = RoundedCornerShape(24.dp),
                                strongFill = true,
                                enableBlur = false,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Unable to open this photo",
                                        color = glassConfig.contentPrimaryColor,
                                        style = MaterialTheme.typography.titleMedium
                                            .copy(fontWeight = FontWeight.Bold)
                                            .withGlassLegibility(glassConfig)
                                    )
                                    GlassButton(
                                        label = "Try again",
                                        active = true,
                                        onClick = { viewModel.loadImage(uri, forceReload = true) }
                                    )
                                }
                            }
                        }

                        bmp == null -> {
                            CircularProgressIndicator(color = glassConfig.accentColor)
                        }

                        else -> {
                            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                            val activeDrawStroke = remember { mutableStateListOf<NormalizedPoint>() }
                            val activeEraserTrail = remember { mutableStateListOf<NormalizedPoint>() }
                            val activeDrawPath = remember { Path() }
                            val activeEraserPath = remember { Path() }

                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val density = LocalDensity.current
                                val scale = min(
                                    maxWidth.value / bmp.width.toFloat(),
                                    maxHeight.value / bmp.height.toFloat()
                                )
                                val dispW = (bmp.width * scale).dp
                                val dispH = (bmp.height * scale).dp
                                val dispWPx = with(density) { dispW.toPx() }
                                val dispHPx = with(density) { dispH.toPx() }

                                val gpuEffect = remember(
                                    dispWPx,
                                    dispHPx,
                                    renderState.adjustments,
                                    renderState.filter,
                                    renderState.filterIntensity,
                                    isComparingOriginal
                                ) {
                                    if (!isComparingOriginal && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        PhotoGpuRenderer.buildGpuRenderEffect(
                                            widthPx = dispWPx,
                                            heightPx = dispHPx,
                                            adjustments = renderState.adjustments,
                                            filter = renderState.filter,
                                            filterIntensity = renderState.filterIntensity
                                        )?.asComposeRenderEffect()
                                    } else {
                                        null
                                    }
                                }

                                val straightenDeg = if (isComparingOriginal) 0f else editState.crop.straightenDegrees
                                val straightenZoom = remember(straightenDeg, bmp.width, bmp.height) {
                                    if (abs(straightenDeg) > 0.25f) {
                                        val rad = Math.toRadians(abs(straightenDeg.toDouble()))
                                        val aspectMax = maxOf(
                                            bmp.width.toFloat() / bmp.height.toFloat(),
                                            bmp.height.toFloat() / bmp.width.toFloat()
                                        )
                                        (cos(rad) + sin(rad) * aspectMax).toFloat().coerceIn(1f, 1.75f)
                                    } else {
                                        1f
                                    }
                                }

                                val cachedStrokePaths = remember(editState.markupStrokes, dispWPx, dispHPx) {
                                    editState.markupStrokes.map { stroke ->
                                        if (stroke.points.isEmpty()) null
                                        else Path().apply {
                                            moveTo(stroke.points.first().x * dispWPx, stroke.points.first().y * dispHPx)
                                            for (i in 1 until stroke.points.size) {
                                                lineTo(stroke.points[i].x * dispWPx, stroke.points[i].y * dispHPx)
                                            }
                                        }
                                    }
                                }

                                val allowPressHoldCompare = activeTab != PhotoEditorTab.CROP &&
                                    !(activeTab == PhotoEditorTab.MARKUP && markupTool != MarkupTool.TEXT) &&
                                    !(activeTab == PhotoEditorTab.TOOLS && magicEraserMode)

                                Box(
                                    modifier = Modifier
                                        .size(width = dispW, height = dispH)
                                        .then(
                                            if (allowPressHoldCompare) {
                                                Modifier.pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            viewModel.setComparingOriginal(true)
                                                            tryAwaitRelease()
                                                            viewModel.setComparingOriginal(false)
                                                        }
                                                    )
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Photo preview",
                                        contentScale = ContentScale.FillBounds,
                                        colorFilter = if (gpuEffect == null && fallbackColorMatrix != null) {
                                            ColorFilter.colorMatrix(fallbackColorMatrix)
                                        } else null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clipToBounds()
                                            .graphicsLayer {
                                                rotationZ = straightenDeg
                                                scaleX = straightenZoom
                                                scaleY = straightenZoom
                                                if (gpuEffect != null) {
                                                    renderEffect = gpuEffect
                                                }
                                            }
                                    )

                                    if (gpuEffect == null && !isComparingOriginal && abs(renderState.adjustments.vignette) > 1f) {
                                        val strength = (renderState.adjustments.vignette / 100f).coerceIn(-1f, 1f)
                                        val vigColor = if (strength >= 0f) {
                                            Color.Black.copy(alpha = strength * 0.72f)
                                        } else {
                                            Color.White.copy(alpha = -strength * 0.58f)
                                        }
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawRect(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(Color.Transparent, Color.Transparent, vigColor),
                                                    center = Offset(size.width / 2f, size.height / 2f),
                                                    radius = maxOf(size.width, size.height) * 0.72f
                                                )
                                            )
                                        }
                                    }

                                    if (!isComparingOriginal && (editState.markupStrokes.isNotEmpty() || activeDrawStroke.isNotEmpty())) {
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                        ) {
                                            val baseDim = min(size.width, size.height)
                                            editState.markupStrokes.forEachIndexed { idx, stroke ->
                                                val path = cachedStrokePaths.getOrNull(idx) ?: return@forEachIndexed
                                                drawPath(
                                                    path = path,
                                                    color = if (stroke.isEraser) Color.Transparent else stroke.colorArgb.toComposeColor().copy(alpha = stroke.alpha),
                                                    style = Stroke(
                                                        width = (stroke.widthFraction * baseDim).coerceAtLeast(3f),
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    ),
                                                    blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
                                                )
                                            }

                                            if (activeDrawStroke.isNotEmpty()) {
                                                activeDrawPath.reset()
                                                activeDrawPath.moveTo(activeDrawStroke.first().x * size.width, activeDrawStroke.first().y * size.height)
                                                for (i in 1 until activeDrawStroke.size) {
                                                    activeDrawPath.lineTo(activeDrawStroke[i].x * size.width, activeDrawStroke[i].y * size.height)
                                                }
                                                val wFrac = when (markupTool) {
                                                    MarkupTool.HIGHLIGHTER -> markupWidthFraction * 2.4f
                                                    MarkupTool.ERASER -> markupWidthFraction * 2.6f
                                                    else -> markupWidthFraction
                                                }
                                                drawPath(
                                                    path = activeDrawPath,
                                                    color = if (markupTool == MarkupTool.ERASER) {
                                                        Color.Transparent
                                                    } else {
                                                        markupColor.toComposeColor().copy(
                                                            alpha = if (markupTool == MarkupTool.HIGHLIGHTER) 0.42f else 1f
                                                        )
                                                    },
                                                    style = Stroke(
                                                        width = (wFrac * baseDim).coerceAtLeast(3f),
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    ),
                                                    blendMode = if (markupTool == MarkupTool.ERASER) BlendMode.Clear else BlendMode.SrcOver
                                                )
                                            }
                                        }
                                    }

                                    if (activeTab == PhotoEditorTab.MARKUP && markupTool != MarkupTool.TEXT) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(markupTool, markupColor, markupWidthFraction) {
                                                    detectDragGestures(
                                                        onDragStart = { start ->
                                                            activeDrawStroke.clear()
                                                            activeDrawStroke.add(
                                                                NormalizedPoint(
                                                                    (start.x / size.width).coerceIn(0f, 1f),
                                                                    (start.y / size.height).coerceIn(0f, 1f)
                                                                )
                                                            )
                                                        },
                                                        onDrag = { change, _ ->
                                                            change.consume()
                                                            activeDrawStroke.add(
                                                                NormalizedPoint(
                                                                    (change.position.x / size.width).coerceIn(0f, 1f),
                                                                    (change.position.y / size.height).coerceIn(0f, 1f)
                                                                )
                                                            )
                                                        },
                                                        onDragEnd = {
                                                            viewModel.addMarkupStroke(activeDrawStroke.toList())
                                                            activeDrawStroke.clear()
                                                        },
                                                        onDragCancel = { activeDrawStroke.clear() }
                                                    )
                                                }
                                        )
                                    }

                                    if (!isComparingOriginal) {
                                        val latestDispWPx by rememberUpdatedState(dispWPx)
                                        val latestDispHPx by rememberUpdatedState(dispHPx)
                                        editState.markupTexts.forEach { item ->
                                            key(item.id) {
                                                val latestItem by rememberUpdatedState(item)
                                                val fontFam = when (item.fontStyle) {
                                                    MarkupFontStyle.SANS -> FontFamily.SansSerif
                                                    MarkupFontStyle.SERIF -> FontFamily.Serif
                                                    MarkupFontStyle.MONO -> FontFamily.Monospace
                                                    MarkupFontStyle.BOLD -> FontFamily.Default
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .offset {
                                                            IntOffset(
                                                                (item.normX * dispWPx).roundToInt() - 48,
                                                                (item.normY * dispHPx).roundToInt() - 24
                                                            )
                                                        }
                                                        .pointerInput(item.id) {
                                                            detectDragGestures(
                                                                onDrag = { change, drag ->
                                                                    change.consume()
                                                                    val cur = latestItem
                                                                    viewModel.moveMarkupText(
                                                                        cur.id,
                                                                        cur.normX + drag.x / latestDispWPx.coerceAtLeast(1f),
                                                                        cur.normY + drag.y / latestDispHPx.coerceAtLeast(1f)
                                                                    )
                                                                },
                                                                onDragEnd = { viewModel.commitMoveMarkupText() }
                                                            )
                                                        }
                                                        .clickable(enabled = activeTab == PhotoEditorTab.MARKUP) {
                                                            textInputDraft = latestItem.text
                                                            editingTextItem = latestItem
                                                        }
                                                        .padding(4.dp)
                                                ) {
                                                    Text(
                                                        text = item.text,
                                                        color = item.colorArgb.toComposeColor(),
                                                        fontSize = item.sizeSp.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = fontFam
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (activeTab == PhotoEditorTab.TOOLS && magicEraserMode) {
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(eraserRadiusFraction) {
                                                    detectDragGestures(
                                                        onDragStart = { start ->
                                                            activeEraserTrail.clear()
                                                            activeEraserTrail.add(
                                                                NormalizedPoint(
                                                                    (start.x / size.width).coerceIn(0f, 1f),
                                                                    (start.y / size.height).coerceIn(0f, 1f)
                                                                )
                                                            )
                                                        },
                                                        onDrag = { change, _ ->
                                                            change.consume()
                                                            activeEraserTrail.add(
                                                                NormalizedPoint(
                                                                    (change.position.x / size.width).coerceIn(0f, 1f),
                                                                    (change.position.y / size.height).coerceIn(0f, 1f)
                                                                )
                                                            )
                                                        },
                                                        onDragEnd = {
                                                            viewModel.addMagicEraserStroke(activeEraserTrail.toList())
                                                            activeEraserTrail.clear()
                                                        },
                                                        onDragCancel = { activeEraserTrail.clear() }
                                                    )
                                                }
                                        ) {
                                            if (activeEraserTrail.isNotEmpty()) {
                                                val baseDim = min(size.width, size.height)
                                                activeEraserPath.reset()
                                                activeEraserPath.moveTo(activeEraserTrail.first().x * size.width, activeEraserTrail.first().y * size.height)
                                                for (i in 1 until activeEraserTrail.size) {
                                                    activeEraserPath.lineTo(activeEraserTrail[i].x * size.width, activeEraserTrail[i].y * size.height)
                                                }
                                                drawPath(
                                                    path = activeEraserPath,
                                                    color = EditorPalette.Accent.copy(alpha = 0.55f),
                                                    style = Stroke(
                                                        width = (eraserRadiusFraction * baseDim * 2f).coerceAtLeast(16f),
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    if (activeTab == PhotoEditorTab.CROP && !isComparingOriginal) {
                                        PhotoCropOverlay(
                                            crop = editState.crop,
                                            onPreviewCrop = viewModel::previewCropBounds,
                                            onCommitCrop = viewModel::commitCropBounds
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Layer 1: Floating Top Glass Controls & Compare Original Pill
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = !isComparingOriginal && !isSliderDragging,
                        enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter(MotionTokens.StandardMs) + slideInVertically(motion.uiTween()) { -it / 2 },
                        exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit(MotionTokens.MicroMs) + slideOutVertically(motion.exitTween()) { -it / 2 },
                        modifier = Modifier.onSizeChanged { size ->
                            if (!isComparingOriginal && !isSliderDragging && size.height > 0) {
                                measuredTopBarPx = size.height
                            }
                        }
                    ) {
                        EditorTopBar(
                            canUndo = canUndo,
                            canRedo = canRedo,
                            primaryActionLabel = "Save",
                            primaryActionIcon = Icons.Rounded.Check,
                            primaryEnabled = sourceBitmap != null && !isSaving,
                            isBusy = isSaving,
                            onCancel = handleExitRequest,
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            onPrimaryAction = { showSaveChoiceDialog = true },
                            autoEnhanceActive = editState.tools.autoEnhanced,
                            onAutoEnhanceClick = {
                                val turningOn = !editState.tools.autoEnhanced
                                viewModel.toggleAutoEnhance()
                                glassToastMessage = if (turningOn) "Auto Enhance applied" else "Auto Enhance removed"
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isComparingOriginal,
                        enter = motion.fadeEnter(MotionTokens.MicroMs),
                        exit = motion.fadeExit(MotionTokens.MicroFastMs)
                    ) {
                        GlassPanel(
                            shape = CircleShape,
                            active = true,
                            strongFill = true,
                            enableBlur = true,
                            elevation = 12.dp,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Compare,
                                    contentDescription = null,
                                    tint = glassConfig.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Original",
                                    style = MaterialTheme.typography.labelLarge
                                        .copy(fontWeight = FontWeight.Bold)
                                        .withGlassLegibility(glassConfig),
                                    color = glassConfig.contentPrimaryColor
                                )
                            }
                        }
                    }

                    GlassToastBanner(
                        message = glassToastMessage,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Layer 2: Floating Glass Bottom Dock (Compare hint, Tooltip, Tool Glass Sheet, Pill Tab Bar)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (!isComparingOriginal && !isSliderDragging && size.height > 0) {
                                measuredBottomDockPx = size.height
                            }
                        }
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = !isComparingOriginal && !isSliderDragging &&
                            hasUnsavedChanges && activeTab != PhotoEditorTab.CROP && !magicEraserMode,
                        enter = motion.fadeEnter(MotionTokens.MicroMs),
                        exit = motion.fadeExit(MotionTokens.MicroFastMs)
                    ) {
                        GlassPanel(
                            shape = CircleShape,
                            enableBlur = false,
                            elevation = 6.dp
                        ) {
                            Text(
                                text = "Hold photo to compare",
                                style = MaterialTheme.typography.labelSmall.withGlassLegibility(glassConfig),
                                color = glassConfig.contentSecondaryColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    val tipKey = "photo_${activeTab.name}"
                    val (tipTitle, tipBody) = remember(activeTab) { photoTabTip(activeTab) }
                    EditorTooltipBanner(
                        visible = !isComparingOriginal && !isSliderDragging && !skipAllEditorTips && tipKey !in seenEditorTips,
                        title = tipTitle,
                        message = tipBody,
                        onDismissTip = { viewModel.markTipSeen(activeTab) },
                        onSkipAllTips = viewModel::skipAllTips
                    )

                    // Floating Tool Sheet — remains mounted while dragging slider so drag gesture never breaks;
                    // inner non-slider controls collapse cleanly when `isSliderDragging` is true.
                    AnimatedVisibility(
                        visible = !isComparingOriginal,
                        enter = motion.bottomSheetEnter(),
                        exit = motion.bottomSheetExit()
                    ) {
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
                                    label = "photo_editor_panel"
                                ) { tab ->
                                    when (tab) {
                                        PhotoEditorTab.SUGGESTIONS -> SuggestionsPanel(
                                            activeSuggestion = editState.activeSuggestion,
                                            thumbnails = suggestionThumbnails,
                                            onSelectSuggestion = { preset ->
                                                viewModel.applySuggestion(preset)
                                                glassToastMessage = "${preset.title} applied"
                                            }
                                        )

                                        PhotoEditorTab.CROP -> CropPanel(
                                            crop = editState.crop,
                                            isSliderDragging = isSliderDragging,
                                            onSliderDraggingChange = viewModel::setSliderDragging,
                                            onRotate90 = viewModel::rotate90,
                                            onFlip = viewModel::flipHorizontal,
                                            onSelectAspect = { aspect ->
                                                val bmp = orientedBase
                                                viewModel.selectAspectRatio(aspect, bmp?.width ?: 0, bmp?.height ?: 0)
                                            },
                                            onPreviewStraighten = viewModel::previewStraighten,
                                            onCommitStraighten = viewModel::commitStraighten,
                                            onResetStraighten = viewModel::resetStraighten
                                        )

                                        PhotoEditorTab.ADJUST -> AdjustPanel(
                                            adjustments = editState.adjustments,
                                            selectedParam = selectedAdjustParam,
                                            thumbnails = adjustThumbnails,
                                            isSliderDragging = isSliderDragging,
                                            onSliderDraggingChange = viewModel::setSliderDragging,
                                            onSelectParam = viewModel::selectAdjustParam,
                                            onPreviewValue = viewModel::previewAdjustment,
                                            onCommitValue = viewModel::commitAdjustment,
                                            onResetParam = viewModel::resetAdjustment
                                        )

                                        PhotoEditorTab.FILTERS -> FiltersPanel(
                                            selectedFilter = editState.filter,
                                            intensity = editState.filterIntensity,
                                            thumbnails = filterThumbnails,
                                            isSliderDragging = isSliderDragging,
                                            onSliderDraggingChange = viewModel::setSliderDragging,
                                            onSelectFilter = viewModel::selectFilter,
                                            onPreviewIntensity = viewModel::previewFilterIntensity,
                                            onCommitIntensity = viewModel::commitFilterIntensity,
                                            onResetIntensity = viewModel::resetFilterIntensity
                                        )

                                        PhotoEditorTab.MARKUP -> MarkupPanel(
                                            activeTool = markupTool,
                                            activeColor = markupColor,
                                            widthFraction = markupWidthFraction,
                                            fontStyle = markupFontStyle,
                                            textSizeSp = markupTextSizeSp,
                                            canUndo = canUndo,
                                            canRedo = canRedo,
                                            isSliderDragging = isSliderDragging,
                                            onSliderDraggingChange = viewModel::setSliderDragging,
                                            onSelectTool = viewModel::selectMarkupTool,
                                            onSelectColor = viewModel::selectMarkupColor,
                                            onWidthChange = viewModel::setMarkupWidthFraction,
                                            onSelectFont = viewModel::selectMarkupFont,
                                            onTextSizeChange = viewModel::setMarkupTextSizeSp,
                                            onAddTextClick = {
                                                val fresh = viewModel.createNewMarkupText("")
                                                textInputDraft = ""
                                                editingTextItem = fresh
                                            },
                                            onUndo = viewModel::undo,
                                            onRedo = viewModel::redo
                                        )

                                        PhotoEditorTab.TOOLS -> ToolsPanel(
                                            tools = editState.tools,
                                            magicEraserMode = magicEraserMode,
                                            eraserRadiusFraction = eraserRadiusFraction,
                                            isSliderDragging = isSliderDragging,
                                            onSliderDraggingChange = viewModel::setSliderDragging,
                                            onToggleAutoEnhance = {
                                                val turningOn = !editState.tools.autoEnhanced
                                                viewModel.toggleAutoEnhance()
                                                glassToastMessage = if (turningOn) "Auto Enhance applied" else "Auto Enhance removed"
                                            },
                                            onPreviewBlur = viewModel::previewBackgroundBlur,
                                            onCommitBlur = viewModel::commitBackgroundBlur,
                                            onResetBlur = viewModel::resetBackgroundBlur,
                                            onPreviewDenoise = viewModel::previewDenoiseUnblur,
                                            onCommitDenoise = viewModel::commitDenoiseUnblur,
                                            onResetDenoise = viewModel::resetDenoiseUnblur,
                                            onToggleMagicEraser = { viewModel.setMagicEraserMode(!magicEraserMode) },
                                            onEraserRadiusChange = viewModel::setEraserRadiusFraction,
                                            onClearMagicEraser = viewModel::clearMagicEraser
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !isComparingOriginal && !isSliderDragging,
                        enter = motion.bottomSheetEnter(),
                        exit = motion.bottomSheetExit()
                    ) {
                        EditorBottomTabBar(
                            tabs = photoTabs,
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

            // Glass Add / Edit Markup Text Dialog
            editingTextItem?.let { item ->
                GlassDialogShell(
                    onDismissRequest = { editingTextItem = null },
                    title = "Add Text"
                ) {
                    OutlinedTextField(
                        value = textInputDraft,
                        onValueChange = { textInputDraft = it },
                        placeholder = { Text("Type caption…", color = glassConfig.contentSecondaryColor) },
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
                        if (editState.markupTexts.any { it.id == item.id }) {
                            IconButton(
                                onClick = {
                                    viewModel.deleteMarkupText(item.id)
                                    editingTextItem = null
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete text", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { editingTextItem = null },
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
                                val trimmed = textInputDraft.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.addOrUpdateMarkupText(
                                        item.copy(
                                            text = trimmed,
                                            colorArgb = markupColor,
                                            fontStyle = markupFontStyle,
                                            sizeSp = markupTextSizeSp
                                        )
                                    )
                                }
                                editingTextItem = null
                            }
                        )
                    }
                }
            }

            // Glass Save As Copy vs Overwrite Original Dialog
            if (showSaveChoiceDialog) {
                GlassDialogShell(
                    onDismissRequest = { if (!isSaving) showSaveChoiceDialog = false },
                    title = "Save Photo"
                ) {
                    Text(
                        text = "Save a new edited copy or replace the original photo?",
                        style = MaterialTheme.typography.bodyMedium.withGlassLegibility(glassConfig),
                        color = glassConfig.contentSecondaryColor
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(
                            icon = Icons.Rounded.ContentCopy,
                            label = "Save Copy",
                            active = true,
                            enabled = !isSaving,
                            enableBlur = false,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_save_copy"),
                            onClick = {
                                showSaveChoiceDialog = false
                                isSaving = true
                                scope.launch {
                                    val bmp = viewModel.renderHighResForSave()
                                    if (bmp != null) {
                                        onSaveCopy(bmp, displayName) {
                                            isSaving = false
                                        }
                                    } else {
                                        isSaving = false
                                        glassToastMessage = "Unable to render photo"
                                    }
                                }
                            }
                        )
                        GlassButton(
                            icon = Icons.Rounded.Save,
                            label = "Overwrite Original",
                            active = false,
                            enabled = !isSaving,
                            enableBlur = false,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_overwrite_original"),
                            onClick = {
                                showSaveChoiceDialog = false
                                isSaving = true
                                scope.launch {
                                    val bmp = viewModel.renderHighResForSave()
                                    if (bmp != null) {
                                        onOverwriteOriginal(uri, bmp) {
                                            isSaving = false
                                        }
                                    } else {
                                        isSaving = false
                                        glassToastMessage = "Unable to render photo"
                                    }
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showSaveChoiceDialog = false },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Cancel", color = glassConfig.contentSecondaryColor)
                        }
                    }
                }
            }
        }
}

private fun photoTabTip(tab: PhotoEditorTab): Pair<String, String> = when (tab) {
    PhotoEditorTab.SUGGESTIONS -> "One-tap Suggestions" to "Try Enhance for an instant fix, or hold the photo anytime to compare with the original."
    PhotoEditorTab.CROP -> "Crop & Straighten" to "Drag corners to frame your shot, or slide Straighten and double-tap to reset to 0°."
    PhotoEditorTab.ADJUST -> "Fine-tune Light & Color" to "Pick a thumbnail, drag the slider, and double-tap the slider anytime to reset."
    PhotoEditorTab.FILTERS -> "Visual Filters" to "Tap any look to preview it live, then adjust its strength to keep things natural."
    PhotoEditorTab.MARKUP -> "Draw & Add Text" to "Sketch directly on the photo with Pen or Highlight, or drop movable text captions."
    PhotoEditorTab.TOOLS -> "Smart Photo Tools" to "Blur backgrounds for portrait depth, clean blur, or brush objects away with Magic Eraser."
}

@Composable
private fun PhotoCropOverlay(
    crop: PhotoCropState,
    onPreviewCrop: (Float, Float, Float, Float) -> Unit,
    onCommitCrop: () -> Unit
) {
    val motion = LocalMotionConfig.current
    val latestCrop by rememberUpdatedState(crop)
    val latestPreviewCrop by rememberUpdatedState(onPreviewCrop)
    val latestCommitCrop by rememberUpdatedState(onCommitCrop)
    var isDragging by remember { mutableStateOf(false) }

    val animSpec = if (isDragging || motion.prefersReducedMotion) snap<Float>() else MotionTokens.SnappySpring
    val animL by animateFloatAsState(crop.left, animSpec, label = "crop_l")
    val animT by animateFloatAsState(crop.top, animSpec, label = "crop_t")
    val animR by animateFloatAsState(crop.right, animSpec, label = "crop_r")
    val animB by animateFloatAsState(crop.bottom, animSpec, label = "crop_b")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var activeHandle = -1
                detectDragGestures(
                    onDragStart = { start ->
                        isDragging = true
                        val c = latestCrop
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val corners = listOf(
                            Offset(c.left * w, c.top * h),
                            Offset(c.right * w, c.top * h),
                            Offset(c.left * w, c.bottom * h),
                            Offset(c.right * w, c.bottom * h)
                        )
                        activeHandle = corners.indices.minByOrNull { idx ->
                            (corners[idx] - start).getDistance()
                        } ?: -1
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        val c = latestCrop
                        val dx = drag.x / size.width.toFloat().coerceAtLeast(1f)
                        val dy = drag.y / size.height.toFloat().coerceAtLeast(1f)
                        val minGap = 0.15f
                        var l = c.left
                        var t = c.top
                        var r = c.right
                        var b = c.bottom
                        when (activeHandle) {
                            0 -> { l = (l + dx).coerceIn(0f, r - minGap); t = (t + dy).coerceIn(0f, b - minGap) }
                            1 -> { r = (r + dx).coerceIn(l + minGap, 1f); t = (t + dy).coerceIn(0f, b - minGap) }
                            2 -> { l = (l + dx).coerceIn(0f, r - minGap); b = (b + dy).coerceIn(t + minGap, 1f) }
                            3 -> { r = (r + dx).coerceIn(l + minGap, 1f); b = (b + dy).coerceIn(t + minGap, 1f) }
                        }
                        latestPreviewCrop(l, t, r, b)
                    },
                    onDragEnd = {
                        activeHandle = -1
                        isDragging = false
                        latestCommitCrop()
                    },
                    onDragCancel = {
                        activeHandle = -1
                        isDragging = false
                    }
                )
            }
    ) {
        val l = animL * size.width
        val t = animT * size.height
        val r = animR * size.width
        val b = animB * size.height

        val dim = Color.Black.copy(alpha = 0.56f)
        drawRect(dim, Offset.Zero, Size(size.width, t))
        drawRect(dim, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(dim, Offset(0f, t), Size(l, b - t))
        drawRect(dim, Offset(r, t), Size(size.width - r, b - t))

        val thirdW = (r - l) / 3f
        val thirdH = (b - t) / 3f
        for (i in 1..2) {
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(l + thirdW * i, t),
                end = Offset(l + thirdW * i, b),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(l, t + thirdH * i),
                end = Offset(r, t + thirdH * i),
                strokeWidth = 1.dp.toPx()
            )
        }

        drawRect(
            color = Color.White,
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            style = Stroke(width = 2.dp.toPx())
        )

        val handleRadius = 9.dp.toPx()
        listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { pt ->
            drawCircle(color = EditorPalette.Accent, radius = handleRadius + 2.dp.toPx(), center = pt)
            drawCircle(color = Color.White, radius = handleRadius - 1.dp.toPx(), center = pt)
        }
    }
}
