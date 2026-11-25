package com.example.ui.screens

import android.app.WallpaperManager
import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.R
import com.example.data.repository.VaultImageRequest
import com.example.data.repository.VaultMediaDataSource
import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaDetails
import com.example.domain.model.MediaItemRef
import com.example.domain.model.MediaType
import com.example.domain.model.ThemeMode
import com.example.domain.model.VaultMedia
import com.example.security.VaultCryptoManager
import com.example.ui.components.CircularGlassIconButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassModalButton
import com.example.ui.components.LiquidGlassModalSecondaryButton
import com.example.ui.components.LocalHazeState
import com.example.ui.components.LocalReduceTransparency
import com.example.ui.components.gridThumbCacheKey
import com.example.ui.components.liquidGlassSettle
import com.example.ui.components.rememberSupportsLiquidBlur
import com.example.ui.theme.AnimatedFavoriteHeartIcon
import com.example.ui.theme.DestructiveCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.LuminaTheme
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.PlayfairFontFamily
import com.example.ui.theme.VaultAtmosphere
import com.example.ui.theme.originAwareEntrance
import com.example.ui.theme.tactilePressMotion
import com.example.ui.viewmodel.DateGroupingUtils
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tanh

@kotlin.OptIn(ExperimentalSharedTransitionApi::class)
private val ViewerSharedBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

private enum class ViewerSheetMode {
    SLIM_PILL,
    SHEET,
    DETAILS
}

@kotlin.OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaViewerScreen(
    items: List<MediaItemRef>,
    initialIndex: Int,
    imageLoader: ImageLoader,
    cryptoManager: VaultCryptoManager,
    vaultFileProvider: (VaultMedia) -> File,
    vaultLevel: Int? = null,
    dynamicAccent: Color? = null,
    onClose: () -> Unit,
    onCurrentItemChanged: (MediaItemRef?) -> Unit,
    onShareGallery: (GalleryMedia) -> Unit = {},
    onToggleFavorite: (GalleryMedia) -> Unit = {},
    onEditPhoto: (GalleryMedia) -> Unit = {},
    onEditVideo: (GalleryMedia) -> Unit = {},
    onTrashGallery: (GalleryMedia) -> Unit = {},
    onRenameGallery: (GalleryMedia, String) -> Unit = { _, _ -> },
    onSetWallpaper: (GalleryMedia, Int) -> Unit = { _, _ -> },
    onHideToSecretFolder: (GalleryMedia) -> Unit = {},
    onLoadGalleryDetails: suspend (GalleryMedia) -> MediaDetails,
    onUnhideVault: (VaultMedia) -> Unit = {},
    onMoveToDoubleHidden: (VaultMedia) -> Unit = {},
    onDeleteVaultPermanently: (VaultMedia) -> Unit = {},
    sharedElementPrefix: String = "",
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val scope = rememberCoroutineScope()
    val safeStart = initialIndex.coerceIn(0, items.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeStart) { items.size }
    val supportsBlur = rememberSupportsLiquidBlur()
    val viewerHazeState = rememberHazeState(blurEnabled = supportsBlur)
    var viewerSettled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(220L)
        viewerSettled = true
    }

    var sheetMode by remember { mutableStateOf(ViewerSheetMode.SHEET) }
    var currentZoomScale by remember { mutableFloatStateOf(1f) }
    val dismissDragY = remember { Animatable(0f) }

    var overflowExpanded by remember { mutableStateOf(false) }
    var activeDetails by remember { mutableStateOf<MediaDetails?>(null) }
    var renameTarget by remember { mutableStateOf<GalleryMedia?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var wallpaperTarget by remember { mutableStateOf<GalleryMedia?>(null) }
    var deleteVaultTarget by remember { mutableStateOf<VaultMedia?>(null) }

    val currentItem = items.getOrNull(pagerState.currentPage)
    LaunchedEffect(currentItem) {
        currentZoomScale = 1f
        onCurrentItemChanged(currentItem)
        activeDetails = when (currentItem) {
            is MediaItemRef.Gallery -> {
                val g = currentItem.item
                MediaDetails(
                    title = g.displayName,
                    path = "${g.relativePath}${g.displayName}".ifBlank { g.bucketName },
                    sizeBytes = g.sizeBytes,
                    dateTakenMs = g.effectiveDateMs,
                    resolution = if (g.width > 0 && g.height > 0) "${g.width} × ${g.height}" else "Unknown",
                    mimeType = g.mimeType,
                    durationMs = g.durationMs
                )
            }
            is MediaItemRef.Vault -> {
                val v = currentItem.item
                MediaDetails(
                    title = v.originalName,
                    path = "Encrypted Vault (Level ${v.level})",
                    sizeBytes = v.sizeBytes,
                    dateTakenMs = v.dateTakenMs,
                    resolution = if (v.width > 0 && v.height > 0) "${v.width} × ${v.height}" else "Encrypted",
                    mimeType = v.mimeType,
                    durationMs = v.durationMs
                )
            }
            null -> null
        }
    }

    // Load full EXIF metadata only after open transition settles or immediately when Details sheet opens
    LaunchedEffect(currentItem?.key, sheetMode == ViewerSheetMode.DETAILS) {
        val g = (currentItem as? MediaItemRef.Gallery)?.item ?: return@LaunchedEffect
        if (sheetMode != ViewerSheetMode.DETAILS) {
            delay(260L)
        }
        runCatching { onLoadGalleryDetails(g) }.getOrNull()?.let { loaded ->
            activeDetails = loaded
        }
    }

    // Prefetch adjacent neighbor pages strictly after the initial open animation settles
    val displayMetrics = context.resources.displayMetrics
    val viewerMaxW = remember(displayMetrics.widthPixels) { displayMetrics.widthPixels.coerceIn(720, 1440) }
    val viewerMaxH = remember(displayMetrics.heightPixels) { displayMetrics.heightPixels.coerceIn(720, 1440) }
    LaunchedEffect(pagerState.currentPage, items.size, viewerSettled) {
        if (!viewerSettled) return@LaunchedEffect
        delay(80L)
        val neighbors = listOf(pagerState.currentPage - 1, pagerState.currentPage + 1)
        for (p in neighbors) {
            val neighbor = items.getOrNull(p) ?: continue
            if (neighbor.mediaType == MediaType.IMAGE) {
                val data: Any = when (neighbor) {
                    is MediaItemRef.Gallery -> neighbor.item.uri
                    is MediaItemRef.Vault -> VaultImageRequest(
                        file = vaultFileProvider(neighbor.item),
                        mimeType = neighbor.item.mimeType,
                        isThumbnail = false
                    )
                }
                val isVault = data is VaultImageRequest
                val req = ImageRequest.Builder(context)
                    .data(data)
                    .size(viewerMaxW, viewerMaxH)
                    .precision(Precision.INEXACT)
                    .scale(Scale.FIT)
                    .apply {
                        if (isVault) diskCachePolicy(CachePolicy.DISABLED)
                    }
                    .build()
                imageLoader.enqueue(req)
            }
        }
    }

    val toggleChrome = {
        sheetMode = when (sheetMode) {
            ViewerSheetMode.SLIM_PILL -> ViewerSheetMode.SHEET
            ViewerSheetMode.SHEET, ViewerSheetMode.DETAILS -> ViewerSheetMode.SLIM_PILL
        }
    }

    val dragVal = dismissDragY.value
    val backdropAlpha = (1f - (abs(dragVal) / 700f)).coerceIn(0.25f, 1f)
    val dragScale = (1f - (abs(dragVal) / 1800f)).coerceIn(0.84f, 1f)
    val enableViewerChromeBlur = supportsBlur && viewerSettled && !pagerState.isScrollInProgress && abs(dragVal) < 4f
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val sheetReserve = if (items.size > 1) 236.dp else 178.dp
    val scrubberTargetBottom = when (sheetMode) {
        ViewerSheetMode.SLIM_PILL -> 80.dp
        ViewerSheetMode.SHEET -> sheetReserve + 6.dp
        ViewerSheetMode.DETAILS -> 276.dp
    }

    val viewerAtmosphere = when (vaultLevel) {
        1 -> VaultAtmosphere.VAULT_LEVEL_1
        2 -> VaultAtmosphere.VAULT_LEVEL_2
        else -> VaultAtmosphere.GALLERY
    }

    LuminaTheme(
        themeMode = ThemeMode.DARK,
        atmosphere = viewerAtmosphere,
        dynamicAccent = if (vaultLevel == null) (dynamicAccent ?: glass.accentGlow) else null
    ) {
        CompositionLocalProvider(LocalHazeState provides viewerHazeState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = backdropAlpha))
            ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentZoomScale <= 1.03f && abs(dragVal) < 12f,
                beyondViewportPageCount = if (viewerSettled) 1 else 0,
                key = { index -> items.getOrNull(index)?.key ?: "empty_$index" },
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = viewerHazeState, zIndex = 1f)
                    .graphicsLayer {
                        translationY = dismissDragY.value
                        scaleX = dragScale
                        scaleY = dragScale
                    }
                    .pointerInput(currentZoomScale) {
                        if (currentZoomScale <= 1.03f) {
                            var rawY = 0f
                            val velocityTracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = {
                                    velocityTracker.resetTracking()
                                    scope.launch { dismissDragY.stop() }
                                    rawY = dismissDragY.value
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    rawY += dragAmount
                                    // Downward dismiss tracks 1:1; upward edge rubber-bands with tanh (#11)
                                    val targetY = if (rawY >= 0f) {
                                        rawY
                                    } else {
                                        -(56f * tanh((-rawY) / 140f))
                                    }
                                    scope.launch { dismissDragY.snapTo(targetY) }
                                },
                                onDragEnd = {
                                    val velY = velocityTracker.calculateVelocity().y
                                    val projectedY = dismissDragY.value + velY * 0.16f
                                    if (projectedY > 240f) {
                                        onClose()
                                    } else {
                                        rawY = 0f
                                        scope.launch {
                                            dismissDragY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = motion.fluidFloatSpring(),
                                                initialVelocity = velY
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    rawY = 0f
                                    scope.launch {
                                        dismissDragY.animateTo(0f, motion.fluidFloatSpring())
                                    }
                                }
                            )
                        }
                    }
            ) { page ->
                val ref = items.getOrNull(page) ?: return@HorizontalPager
                val isActivePage = page == pagerState.currentPage
                val sharedMod = if (
                    isActivePage &&
                    sharedElementPrefix.isNotBlank() &&
                    sharedTransitionScope != null &&
                    animatedVisibilityScope != null
                ) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            state = rememberSharedContentState(key = "${sharedElementPrefix}_${ref.key}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = ViewerSharedBoundsTransform
                        )
                    }
                } else {
                    Modifier
                }

                if (ref.mediaType == MediaType.VIDEO) {
                    ViewerVideoPage(
                        ref = ref,
                        isActive = isActivePage,
                        imageLoader = imageLoader,
                        cryptoManager = cryptoManager,
                        vaultFileProvider = vaultFileProvider,
                        chromeVisible = sheetMode != ViewerSheetMode.DETAILS,
                        scrubberTargetBottom = scrubberTargetBottom,
                        onToggleChrome = toggleChrome,
                        modifier = sharedMod
                    )
                } else {
                    ViewerPhotoPage(
                        ref = ref,
                        imageLoader = imageLoader,
                        vaultFileProvider = vaultFileProvider,
                        onScaleChanged = { if (isActivePage) currentZoomScale = it },
                        onTap = toggleChrome,
                        modifier = sharedMod
                    )
                }
            }

            // Subtle darkroom viewfinder corner ticks when chrome is visible
            if (sheetMode != ViewerSheetMode.SLIM_PILL && currentZoomScale <= 1.05f) {
                val tickColor = glass.editorialGold.copy(alpha = 0.28f)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = statusTop + 78.dp,
                            bottom = navBottom + sheetReserve + 10.dp,
                            start = 14.dp,
                            end = 14.dp
                        )
                ) {
                    val arm = 14.dp.toPx()
                    val stroke = 1.2.dp.toPx()
                    val w = size.width
                    val h = size.height
                    if (w > arm * 3 && h > arm * 3) {
                        drawLine(tickColor, Offset(0f, 0f), Offset(arm, 0f), stroke, StrokeCap.Round)
                        drawLine(tickColor, Offset(0f, 0f), Offset(0f, arm), stroke, StrokeCap.Round)
                        drawLine(tickColor, Offset(w - arm, 0f), Offset(w, 0f), stroke, StrokeCap.Round)
                        drawLine(tickColor, Offset(w, 0f), Offset(w, arm), stroke, StrokeCap.Round)
                        drawLine(tickColor, Offset(0f, h), Offset(arm, h), stroke, StrokeCap.Round)
                        drawLine(tickColor, Offset(0f, h - arm), Offset(0f, h), stroke, StrokeCap.Round)
                        drawLine(tickColor, Offset(w - arm, h), Offset(w, h), stroke, StrokeCap.Round)
                        drawLine(tickColor, Offset(w, h - arm), Offset(w, h), stroke, StrokeCap.Round)
                    }
                }
            }

            // Floating Liquid Glass Top Chrome (no gradient scrim)
            AnimatedVisibility(
                visible = sheetMode != ViewerSheetMode.SLIM_PILL,
                enter = motion.fadeEnter(MotionTokens.MicroMs),
                exit = motion.fadeExit(MotionTokens.MicroFastMs),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .liquidGlassSettle(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularGlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onClose,
                        size = 48.dp,
                        enableBackdropBlur = enableViewerChromeBlur,
                        tint = Color.White,
                        modifier = Modifier.testTag("viewer_back_button")
                    )

                    GlassSurface(
                        shape = CircleShape,
                        strongFill = true,
                        elevation = 10.dp,
                        enableBackdropBlur = enableViewerChromeBlur,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentItem?.displayName.orEmpty(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = PlayfairFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(glass.editorialGold)
                                )
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${items.size} · ${if (currentItem?.mediaType == MediaType.VIDEO) "VIDEO" else "FRAME"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        letterSpacing = 0.7.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.88f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Box {
                        CircularGlassIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = "More actions",
                            onClick = { overflowExpanded = true },
                            size = 48.dp,
                            enableBackdropBlur = enableViewerChromeBlur,
                            tint = Color.White,
                            modifier = Modifier.testTag("viewer_more_button")
                        )
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                            shape = RoundedCornerShape(22.dp),
                            shadowElevation = 18.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (LocalReduceTransparency.current) glass.glassBorder else glass.prismBorderStart.copy(alpha = 0.72f)
                            ),
                            containerColor = if (LocalReduceTransparency.current) glass.solidFallbackElevated else glass.glassStrongFill
                        ) {
                            Column(
                                modifier = Modifier.originAwareEntrance(TransformOrigin(0.92f, 0f))
                            ) {
                                when (currentItem) {
                                    is MediaItemRef.Gallery -> {
                                        val g = currentItem.item
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (g.mediaType == MediaType.VIDEO) "Edit video" else "Edit photo")
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                overflowExpanded = false
                                                if (g.mediaType == MediaType.VIDEO) onEditVideo(g) else onEditPhoto(g)
                                            },
                                            modifier = Modifier.testTag("viewer_menu_edit")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Hide to Secret Folder") },
                                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                            onClick = {
                                                overflowExpanded = false
                                                onHideToSecretFolder(g)
                                            },
                                            modifier = Modifier.testTag("viewer_menu_hide")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                            onClick = {
                                                overflowExpanded = false
                                                renameInput = g.displayName.substringBeforeLast('.')
                                                renameTarget = g
                                            },
                                            modifier = Modifier.testTag("viewer_menu_rename")
                                        )
                                        if (g.mediaType == MediaType.IMAGE) {
                                            DropdownMenuItem(
                                                text = { Text("Set as Wallpaper") },
                                                leadingIcon = { Icon(Icons.Rounded.Wallpaper, contentDescription = null) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    wallpaperTarget = g
                                                },
                                                modifier = Modifier.testTag("viewer_menu_wallpaper")
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Details") },
                                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                            onClick = {
                                                overflowExpanded = false
                                                sheetMode = ViewerSheetMode.DETAILS
                                            },
                                            modifier = Modifier.testTag("viewer_menu_details")
                                        )
                                    }

                                    is MediaItemRef.Vault -> {
                                        val v = currentItem.item
                                        if (vaultLevel == 1) {
                                            DropdownMenuItem(
                                                text = { Text("Move to Double Hidden Folder") },
                                                leadingIcon = { Icon(Icons.Rounded.DriveFileMove, contentDescription = null) },
                                                onClick = {
                                                    overflowExpanded = false
                                                    onMoveToDoubleHidden(v)
                                                },
                                                modifier = Modifier.testTag("viewer_menu_double_hide")
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Unhide / Restore") },
                                            leadingIcon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                                            onClick = {
                                                overflowExpanded = false
                                                onUnhideVault(v)
                                            },
                                            modifier = Modifier.testTag("viewer_menu_unhide")
                                        )
                                    }

                                    null -> Unit
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Liquid Glass Sheet (or collapsed Slim Action Pill)
            if (currentItem != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (sheetMode == ViewerSheetMode.SLIM_PILL) {
                        ViewerSlimActionPill(
                            currentItem = currentItem,
                            vaultLevel = vaultLevel,
                            onExpandSheet = { sheetMode = ViewerSheetMode.SHEET },
                            onShareGallery = onShareGallery,
                            onToggleFavorite = { g ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleFavorite(g)
                            },
                            onEditPhoto = onEditPhoto,
                            onEditVideo = onEditVideo,
                            onOpenDetails = { sheetMode = ViewerSheetMode.DETAILS },
                            onTrashGallery = onTrashGallery,
                            onUnhideVault = onUnhideVault,
                            onMoveToDoubleHidden = onMoveToDoubleHidden,
                            onRequestDeleteVault = { deleteVaultTarget = it }
                        )
                    } else {
                        ViewerGlassBottomSheet(
                            items = items,
                            currentIndex = pagerState.currentPage,
                            currentItem = currentItem,
                            details = activeDetails,
                            vaultLevel = vaultLevel,
                            isExpandedDetails = sheetMode == ViewerSheetMode.DETAILS,
                            enableBlur = enableViewerChromeBlur,
                            imageLoader = imageLoader,
                            vaultFileProvider = vaultFileProvider,
                            onSelectPage = { targetPage ->
                                scope.launch { pagerState.animateScrollToPage(targetPage) }
                            },
                            onSwipeUpExpand = { sheetMode = ViewerSheetMode.DETAILS },
                            onSwipeDownCollapse = {
                                sheetMode = if (sheetMode == ViewerSheetMode.DETAILS) {
                                    ViewerSheetMode.SHEET
                                } else {
                                    ViewerSheetMode.SLIM_PILL
                                }
                            },
                            onShareGallery = onShareGallery,
                            onToggleFavorite = { g ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleFavorite(g)
                            },
                            onEditPhoto = onEditPhoto,
                            onEditVideo = onEditVideo,
                            onToggleDetails = {
                                sheetMode = if (sheetMode == ViewerSheetMode.DETAILS) {
                                    ViewerSheetMode.SHEET
                                } else {
                                    ViewerSheetMode.DETAILS
                                }
                            },
                            onTrashGallery = onTrashGallery,
                            onUnhideVault = onUnhideVault,
                            onMoveToDoubleHidden = onMoveToDoubleHidden,
                            onRequestDeleteVault = { deleteVaultTarget = it }
                        )
                    }
                }
            }
        }
        }
    }

    // Rename Liquid Glass Modal
    renameTarget?.let { target ->
        LiquidGlassDialog(
            onDismissRequest = { renameTarget = null },
            eyebrow = "FILE NAME",
            title = "Rename File",
            icon = Icons.Rounded.Edit
        ) {
            OutlinedTextField(
                value = renameInput,
                onValueChange = { renameInput = it },
                singleLine = true,
                label = { Text("File name") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = glass.glassBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassModalSecondaryButton(
                    label = "Cancel",
                    onClick = { renameTarget = null },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassModalButton(
                    label = "Rename",
                    enabled = renameInput.isNotBlank(),
                    onClick = {
                        onRenameGallery(target, renameInput)
                        renameTarget = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Wallpaper Target Liquid Glass Modal
    wallpaperTarget?.let { target ->
        LiquidGlassDialog(
            onDismissRequest = { wallpaperTarget = null },
            eyebrow = "DISPLAY",
            title = "Set Wallpaper",
            icon = Icons.Rounded.Wallpaper
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidGlassModalSecondaryButton(
                    label = "Home Screen",
                    onClick = {
                        onSetWallpaper(target, WallpaperManager.FLAG_SYSTEM)
                        wallpaperTarget = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                LiquidGlassModalSecondaryButton(
                    label = "Lock Screen",
                    onClick = {
                        onSetWallpaper(target, WallpaperManager.FLAG_LOCK)
                        wallpaperTarget = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                LiquidGlassModalButton(
                    label = "Home & Lock Screens",
                    onClick = {
                        onSetWallpaper(target, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                        wallpaperTarget = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Permanent Vault Delete Confirmation Liquid Glass Modal
    deleteVaultTarget?.let { target ->
        LiquidGlassDialog(
            onDismissRequest = { deleteVaultTarget = null },
            eyebrow = "ENCRYPTED VAULT",
            title = "Delete Permanently?",
            icon = Icons.Rounded.DeleteForever,
            iconTint = MaterialTheme.colorScheme.error
        ) {
            Text(
                text = "This encrypted item will be securely wiped and cannot be recovered.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.glassSecondaryText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassModalSecondaryButton(
                    label = "Cancel",
                    onClick = { deleteVaultTarget = null },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassModalButton(
                    label = "Delete",
                    isDestructive = true,
                    onClick = {
                        onDeleteVaultPermanently(target)
                        deleteVaultTarget = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ViewerSlimActionPill(
    currentItem: MediaItemRef,
    vaultLevel: Int?,
    onExpandSheet: () -> Unit,
    onShareGallery: (GalleryMedia) -> Unit,
    onToggleFavorite: (GalleryMedia) -> Unit,
    onEditPhoto: (GalleryMedia) -> Unit,
    onEditVideo: (GalleryMedia) -> Unit,
    onOpenDetails: () -> Unit,
    onTrashGallery: (GalleryMedia) -> Unit,
    onUnhideVault: (VaultMedia) -> Unit,
    onMoveToDoubleHidden: (VaultMedia) -> Unit,
    onRequestDeleteVault: (VaultMedia) -> Unit
) {
    GlassSurface(
        shape = CircleShape,
        strongFill = true,
        elevation = 16.dp,
        modifier = Modifier
            .pointerInput(Unit) {
                var accum = 0f
                detectVerticalDragGestures(
                    onDragStart = { accum = 0f },
                    onVerticalDrag = { _, dy -> accum += dy },
                    onDragEnd = { if (accum < -40f) onExpandSheet() }
                )
            }
            .liquidGlassSettle()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (currentItem) {
                is MediaItemRef.Gallery -> {
                    val g = currentItem.item
                    val shareInt = remember { MutableInteractionSource() }
                    val favInt = remember { MutableInteractionSource() }
                    val editInt = remember { MutableInteractionSource() }
                    val detailsInt = remember { MutableInteractionSource() }
                    val delInt = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = { onShareGallery(g) },
                        interactionSource = shareInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(shareInt)
                            .testTag("viewer_share")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onToggleFavorite(g) },
                        interactionSource = favInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(favInt, squashAmount = 0.08f)
                            .testTag("viewer_favorite")
                    ) {
                        AnimatedFavoriteHeartIcon(
                            isFavorite = g.isFavorite,
                            contentDescription = "Favorite",
                            inactiveTint = Color.White,
                            iconSize = 22.dp
                        )
                    }
                    IconButton(
                        onClick = {
                            if (g.mediaType == MediaType.VIDEO) onEditVideo(g) else onEditPhoto(g)
                        },
                        interactionSource = editInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(editInt)
                            .testTag("viewer_edit")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = if (g.mediaType == MediaType.VIDEO) "Edit video" else "Edit photo",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onOpenDetails,
                        interactionSource = detailsInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(detailsInt)
                            .testTag("viewer_details")
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "Details", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onTrashGallery(g) },
                        interactionSource = delInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(delInt)
                            .testTag("viewer_delete")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Move to Trash", tint = DestructiveCoral)
                    }
                }

                is MediaItemRef.Vault -> {
                    val v = currentItem.item
                    val unhideInt = remember { MutableInteractionSource() }
                    val doubleInt = remember { MutableInteractionSource() }
                    val detailsInt = remember { MutableInteractionSource() }
                    val delInt = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = { onUnhideVault(v) },
                        interactionSource = unhideInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(unhideInt)
                            .testTag("viewer_unhide")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = "Unhide / Restore", tint = Color.White)
                    }
                    if (vaultLevel == 1) {
                        IconButton(
                            onClick = { onMoveToDoubleHidden(v) },
                            interactionSource = doubleInt,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .tactilePressMotion(doubleInt)
                                .testTag("viewer_double_hide")
                        ) {
                            Icon(
                                Icons.Rounded.DriveFileMove,
                                contentDescription = "Move to Double Hidden Folder",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenDetails,
                        interactionSource = detailsInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(detailsInt)
                            .testTag("viewer_vault_details")
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "Details", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onRequestDeleteVault(v) },
                        interactionSource = delInt,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(delInt)
                            .testTag("viewer_vault_delete")
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete permanently", tint = DestructiveCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerGlassBottomSheet(
    items: List<MediaItemRef>,
    currentIndex: Int,
    currentItem: MediaItemRef,
    details: MediaDetails?,
    vaultLevel: Int?,
    isExpandedDetails: Boolean,
    enableBlur: Boolean,
    imageLoader: ImageLoader,
    vaultFileProvider: (VaultMedia) -> File,
    onSelectPage: (Int) -> Unit,
    onSwipeUpExpand: () -> Unit,
    onSwipeDownCollapse: () -> Unit,
    onShareGallery: (GalleryMedia) -> Unit,
    onToggleFavorite: (GalleryMedia) -> Unit,
    onEditPhoto: (GalleryMedia) -> Unit,
    onEditVideo: (GalleryMedia) -> Unit,
    onToggleDetails: () -> Unit,
    onTrashGallery: (GalleryMedia) -> Unit,
    onUnhideVault: (VaultMedia) -> Unit,
    onMoveToDoubleHidden: (VaultMedia) -> Unit,
    onRequestDeleteVault: (VaultMedia) -> Unit
) {
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val context = LocalContext.current
    val filmstripState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0)
    )

    val sheetScope = rememberCoroutineScope()
    val sheetDragY = remember { Animatable(0f) }
    val latestExpanded by rememberUpdatedState(isExpandedDetails)
    var didInitialStripSnap by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex) {
        if (!didInitialStripSnap) {
            didInitialStripSnap = true
            return@LaunchedEffect
        }
        if (items.isNotEmpty()) {
            val target = (currentIndex - 2).coerceAtLeast(0)
            filmstripState.animateScrollToItem(target)
        }
    }

    GlassSurface(
        shape = RoundedCornerShape(40.dp),
        strongFill = true,
        elevation = 20.dp,
        enableBackdropBlur = enableBlur,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .graphicsLayer {
                translationY = sheetDragY.value
            }
            .pointerInput(Unit) {
                val tracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = {
                        tracker.resetTracking()
                        sheetScope.launch { sheetDragY.stop() }
                    },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val next = sheetDragY.value + dy * 0.45f
                        val clamped = if (latestExpanded) next.coerceIn(-24f, 96f) else next.coerceIn(-96f, 36f)
                        sheetScope.launch { sheetDragY.snapTo(clamped) }
                    },
                    onDragEnd = {
                        val velY = tracker.calculateVelocity().y
                        val projected = sheetDragY.value + velY * 0.14f
                        when {
                            projected < -44f || (!latestExpanded && sheetDragY.value < -36f) -> onSwipeUpExpand()
                            projected > 44f || (latestExpanded && sheetDragY.value > 36f) -> onSwipeDownCollapse()
                        }
                        sheetScope.launch {
                            sheetDragY.animateTo(0f, motion.fluidFloatSpring())
                        }
                    },
                    onDragCancel = {
                        sheetScope.launch {
                            sheetDragY.animateTo(0f, motion.fluidFloatSpring())
                        }
                    }
                )
            }
            .liquidGlassSettle()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isExpandedDetails) onSwipeDownCollapse() else onSwipeUpExpand()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.42f))
                )
            }

            // Title + Date + Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentItem.displayName.substringBeforeLast('.'),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateGroupingUtils.formatFullDateTime(currentItem.dateMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.86f),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (currentItem is MediaItemRef.Gallery) {
                    val g = currentItem.item
                    val favInt = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .tactilePressMotion(favInt, squashAmount = 0.08f)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(listOf(glass.glassHighlightTop, glass.glassHighlightBottom))
                            )
                            .border(1.dp, glass.glassBorder, CircleShape)
                            .clickable(
                                interactionSource = favInt,
                                indication = androidx.compose.foundation.LocalIndication.current
                            ) { onToggleFavorite(g) }
                            .testTag("viewer_favorite"),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedFavoriteHeartIcon(
                            isFavorite = g.isFavorite,
                            contentDescription = "Favorite",
                            inactiveTint = Color.White,
                            iconSize = 21.dp
                        )
                    }
                } else {
                    CircularGlassIconButton(
                        icon = if (isExpandedDetails) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.Info,
                        contentDescription = "Details",
                        size = 48.dp,
                        enableBackdropBlur = false,
                        tint = Color.White,
                        onClick = onToggleDetails,
                        modifier = Modifier.testTag("viewer_vault_details")
                    )
                }
            }

            // Metadata Chips (Location / Folder, Camera/Lens, File Size)
            val locationChip = when (currentItem) {
                is MediaItemRef.Gallery -> currentItem.item.bucketName
                is MediaItemRef.Vault -> "Vault L${currentItem.item.level}"
            }
            val cameraChip = details?.cameraMakeModel ?: details?.resolution
            val sizeChip = DateGroupingUtils.formatFileSize(currentItem.sizeBytes)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MetadataGlassChip(icon = Icons.Rounded.Folder, text = locationChip)
                }
                if (!cameraChip.isNullOrBlank()) {
                    item {
                        MetadataGlassChip(icon = Icons.Rounded.CameraAlt, text = cameraChip)
                    }
                }
                item {
                    MetadataGlassChip(icon = Icons.Rounded.SdStorage, text = sizeChip)
                }
            }

            // Thumbnail Filmstrip of Neighbouring Items
            if (items.size > 1) {
                LazyRow(
                    state = filmstripState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(items, key = { _, it -> "strip_${it.key}" }) { index, ref ->
                        val isCurrent = index == currentIndex
                        val thumbScale by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else 0.78f,
                            animationSpec = if (motion.prefersReducedMotion) tween(80) else MotionTokens.FollowThroughSpring,
                            label = "filmstrip_thumb_scale"
                        )
                        val thumbData: Any = remember(ref.key) {
                            when (ref) {
                                is MediaItemRef.Gallery -> ref.item.uri
                                is MediaItemRef.Vault -> VaultImageRequest(
                                    file = vaultFileProvider(ref.item),
                                    mimeType = ref.item.mimeType,
                                    isThumbnail = true
                                )
                            }
                        }
                        val req = remember(ref.key, thumbData) {
                            val memCache = imageLoader.memoryCache
                            val k360 = MemoryCache.Key(gridThumbCacheKey(ref.key, 360))
                            val k256 = MemoryCache.Key(gridThumbCacheKey(ref.key, 256))
                            val existingKey = when {
                                memCache?.get(k360) != null -> k360
                                memCache?.get(k256) != null -> k256
                                else -> null
                            }
                            ImageRequest.Builder(context)
                                .data(thumbData)
                                .size(120)
                                .allowRgb565(true)
                                .crossfade(false)
                                .apply {
                                    if (existingKey != null) {
                                        placeholderMemoryCacheKey(existingKey)
                                    }
                                    if (thumbData is VaultImageRequest) diskCachePolicy(CachePolicy.DISABLED)
                                }
                                .build()
                        }
                        val thumbShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .graphicsLayer {
                                    scaleX = thumbScale
                                    scaleY = thumbScale
                                }
                                .clip(thumbShape)
                                .border(
                                    width = if (isCurrent) 2.dp else 0.75.dp,
                                    color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.28f),
                                    shape = thumbShape
                                )
                                .clickable { onSelectPage(index) }
                        ) {
                            AsyncImage(
                                model = req,
                                imageLoader = imageLoader,
                                contentDescription = ref.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Action Buttons Row (Share, Edit/Trim, Info, Delete — plus Vault equivalents)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (currentItem) {
                    is MediaItemRef.Gallery -> {
                        val g = currentItem.item
                        val isVideo = g.mediaType == MediaType.VIDEO
                        PrimaryGlassPillButton(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            onClick = { onShareGallery(g) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("viewer_share")
                        )

                        PrimaryGlassPillButton(
                            icon = Icons.Rounded.Edit,
                            label = "Edit",
                            isAccentFill = false,
                            onClick = {
                                if (isVideo) onEditVideo(g) else onEditPhoto(g)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("viewer_edit")
                        )

                        CircularGlassIconButton(
                            icon = if (isExpandedDetails) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.Info,
                            contentDescription = "Details",
                            size = 48.dp,
                            enableBackdropBlur = false,
                            tint = Color.White,
                            onClick = onToggleDetails,
                            modifier = Modifier.testTag("viewer_details")
                        )

                        CircularGlassIconButton(
                            icon = Icons.Rounded.DeleteOutline,
                            contentDescription = "Move to Trash",
                            size = 48.dp,
                            enableBackdropBlur = false,
                            tint = DestructiveCoral,
                            onClick = { onTrashGallery(g) },
                            modifier = Modifier.testTag("viewer_delete")
                        )
                    }

                    is MediaItemRef.Vault -> {
                        val v = currentItem.item
                        PrimaryGlassPillButton(
                            icon = Icons.Rounded.Restore,
                            label = "Unhide",
                            onClick = { onUnhideVault(v) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("viewer_unhide")
                        )

                        if (vaultLevel == 1) {
                            CircularGlassIconButton(
                                icon = Icons.Rounded.DriveFileMove,
                                contentDescription = "Move to Double Hidden Folder",
                                size = 48.dp,
                                enableBackdropBlur = false,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { onMoveToDoubleHidden(v) },
                                modifier = Modifier.testTag("viewer_double_hide")
                            )
                        }

                        CircularGlassIconButton(
                            icon = Icons.Rounded.DeleteForever,
                            contentDescription = "Delete permanently",
                            size = 48.dp,
                            enableBackdropBlur = false,
                            tint = DestructiveCoral,
                            onClick = { onRequestDeleteVault(v) },
                            modifier = Modifier.testTag("viewer_vault_delete")
                        )
                    }
                }
            }

            // Swipe-Up Expanded Details Section
            AnimatedVisibility(
                visible = isExpandedDetails && details != null,
                enter = if (motion.prefersReducedMotion) motion.fadeEnter(MotionTokens.MicroMs) else motion.fadeEnter(MotionTokens.MicroMs) + expandVertically(motion.uiTween()),
                exit = if (motion.prefersReducedMotion) motion.fadeExit(MotionTokens.MicroFastMs) else motion.fadeExit(MotionTokens.MicroFastMs) + shrinkVertically(motion.exitTween())
            ) {
                details?.let { d ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 6.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(glass.glassBorder)
                        )
                        DetailRow("Filename", d.title)
                        DetailRow("Location", d.path)
                        DetailRow("Date", DateGroupingUtils.formatFullDateTime(d.dateTakenMs))
                        DetailRow("Size", DateGroupingUtils.formatFileSize(d.sizeBytes))
                        DetailRow("Resolution", d.resolution)
                        DetailRow("Format", d.mimeType)
                        if (d.durationMs > 0L) {
                            DetailRow("Duration", DateGroupingUtils.formatDuration(d.durationMs))
                        }
                        d.cameraMakeModel?.let { DetailRow("Device", it) }
                        val cameraMetrics = listOfNotNull(
                            d.aperture,
                            d.shutterSpeed,
                            d.iso,
                            d.focalLength
                        ).joinToString("  •  ")
                        if (cameraMetrics.isNotBlank()) {
                            DetailRow("Camera", cameraMetrics)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataGlassChip(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(0.75.dp, Color.White.copy(alpha = 0.24f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.90f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrimaryGlassPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccentFill: Boolean = true
) {
    val glass = LocalLuminaColors.current
    val interaction = remember { MutableInteractionSource() }
    val pillBrush = if (isAccentFill) {
        Brush.verticalGradient(listOf(glass.glassHighlightTop, glass.glassHighlightBottom))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f)))
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .tactilePressMotion(interaction)
            .clip(CircleShape)
            .background(pillBrush)
            .border(1.dp, Color.White.copy(alpha = if (isAccentFill) 0.50f else 0.34f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.84f),
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ViewerPhotoPage(
    ref: MediaItemRef,
    imageLoader: ImageLoader,
    vaultFileProvider: (VaultMedia) -> File,
    onScaleChanged: (Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val motion = LocalMotionConfig.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember(ref.key) { Animatable(1f) }
    val offsetAnim = remember(ref.key) { Animatable(Offset.Zero, Offset.VectorConverter) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var requestZoomedHiRes by remember(ref.key) { mutableStateOf(false) }

    LaunchedEffect(scaleAnim, ref.key) {
        snapshotFlow { scaleAnim.value }
            .distinctUntilChanged()
            .collect { s ->
                onScaleChanged(s)
                if (s > 1.25f && !requestZoomedHiRes) {
                    requestZoomedHiRes = true
                }
            }
    }

    val modelData: Any = remember(ref.key) {
        when (ref) {
            is MediaItemRef.Gallery -> ref.item.uri
            is MediaItemRef.Vault -> VaultImageRequest(
                file = vaultFileProvider(ref.item),
                mimeType = ref.item.mimeType,
                isThumbnail = false
            )
        }
    }

    val thumbPlaceholderKey = remember(ref.key, imageLoader) {
        val memCache = imageLoader.memoryCache
        val k360 = MemoryCache.Key(gridThumbCacheKey(ref.key, 360))
        val k256 = MemoryCache.Key(gridThumbCacheKey(ref.key, 256))
        when {
            memCache?.get(k360) != null -> k360
            memCache?.get(k256) != null -> k256
            else -> null
        }
    }

    val displayMetrics = context.resources.displayMetrics
    val targetW = remember(displayMetrics.widthPixels, requestZoomedHiRes) {
        if (requestZoomedHiRes) {
            (displayMetrics.widthPixels * 2f).roundToInt().coerceIn(1080, 2400)
        } else {
            displayMetrics.widthPixels.coerceIn(720, 1440)
        }
    }
    val targetH = remember(displayMetrics.heightPixels, requestZoomedHiRes) {
        if (requestZoomedHiRes) {
            (displayMetrics.heightPixels * 2f).roundToInt().coerceIn(1080, 2400)
        } else {
            displayMetrics.heightPixels.coerceIn(720, 1440)
        }
    }

    val request = remember(ref.key, modelData, targetW, targetH, thumbPlaceholderKey) {
        ImageRequest.Builder(context)
            .data(modelData)
            .size(targetW, targetH)
            .precision(Precision.INEXACT)
            .scale(Scale.FIT)
            .crossfade(false)
            .apply {
                if (thumbPlaceholderKey != null) {
                    placeholderMemoryCacheKey(thumbPlaceholderKey)
                }
                if (modelData is VaultImageRequest) {
                    diskCachePolicy(CachePolicy.DISABLED)
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(ref.key) {
                detectTapGestures(
                    onDoubleTap = { tapPos ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val curScale = scaleAnim.value
                        val w = (if (containerSize.width > 0) containerSize.width else size.width).toFloat()
                        val h = (if (containerSize.height > 0) containerSize.height else size.height).toFloat()
                        if (curScale > 1.05f) {
                            scope.launch {
                                if (motion.prefersReducedMotion) {
                                    scaleAnim.snapTo(1f)
                                    offsetAnim.snapTo(Offset.Zero)
                                } else {
                                    launch { scaleAnim.animateTo(1f, motion.fluidFloatSpring()) }
                                    launch { offsetAnim.animateTo(Offset.Zero, spring(dampingRatio = 0.92f, stiffness = 380f)) }
                                }
                            }
                        } else {
                            val targetScale = 2.5f
                            val maxX = (w * (targetScale - 1f)) / 2f
                            val maxY = (h * (targetScale - 1f)) / 2f
                            val rawOffset = Offset(
                                x = ((w / 2f) - tapPos.x) * (targetScale - 1f),
                                y = ((h / 2f) - tapPos.y) * (targetScale - 1f)
                            )
                            val clampedOffset = Offset(
                                x = rawOffset.x.coerceIn(-maxX, maxX),
                                y = rawOffset.y.coerceIn(-maxY, maxY)
                            )
                            scope.launch {
                                if (motion.prefersReducedMotion) {
                                    scaleAnim.snapTo(targetScale)
                                    offsetAnim.snapTo(clampedOffset)
                                } else {
                                    launch { scaleAnim.animateTo(targetScale, motion.fluidFloatSpring()) }
                                    launch { offsetAnim.animateTo(clampedOffset, spring(dampingRatio = 0.92f, stiffness = 380f)) }
                                }
                            }
                        }
                    },
                    onTap = { onTap() }
                )
            }
            .pointerInput(ref.key) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val isMultiTouch = pressedCount >= 2
                        val curScale = scaleAnim.value
                        if (isMultiTouch || curScale > 1.02f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (curScale * zoom).coerceIn(1f, 4f)
                            val w = (if (containerSize.width > 0) containerSize.width else size.width).toFloat()
                            val h = (if (containerSize.height > 0) containerSize.height else size.height).toFloat()
                            val newOffset = if (newScale > 1.02f) {
                                val maxOffsetX = (w * (newScale - 1f)) / 2f
                                val maxOffsetY = (h * (newScale - 1f)) / 2f
                                Offset(
                                    x = (offsetAnim.value.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offsetAnim.value.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                Offset.Zero
                            }
                            scope.launch {
                                scaleAnim.snapTo(newScale)
                                offsetAnim.snapTo(newOffset)
                            }
                            if (isMultiTouch || (curScale > 1.02f && pan.getDistance() > 0.5f)) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = ref.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = scaleAnim.value
                    scaleX = s
                    scaleY = s
                    translationX = offsetAnim.value.x
                    translationY = offsetAnim.value.y
                }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ViewerVideoPage(
    ref: MediaItemRef,
    isActive: Boolean,
    imageLoader: ImageLoader,
    cryptoManager: VaultCryptoManager,
    vaultFileProvider: (VaultMedia) -> File,
    chromeVisible: Boolean,
    scrubberTargetBottom: Dp,
    onToggleChrome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    var isPlaying by remember(ref.key) { mutableStateOf(false) }
    var isBuffering by remember(ref.key) { mutableStateOf(true) }
    var isMuted by remember(ref.key) { mutableStateOf(false) }
    var playbackError by remember(ref.key) { mutableStateOf<String?>(null) }
    var currentPosMs by remember(ref.key) { mutableLongStateOf(0L) }
    var durationMs by remember(ref.key) { mutableLongStateOf(ref.durationMs.coerceAtLeast(1L)) }
    var playerGeneration by remember(ref.key) { mutableLongStateOf(0L) }

    val thumbModel: Any = remember(ref.key) {
        when (ref) {
            is MediaItemRef.Gallery -> ref.item.uri
            is MediaItemRef.Vault -> VaultImageRequest(
                file = vaultFileProvider(ref.item),
                mimeType = ref.item.mimeType,
                isThumbnail = true
            )
        }
    }

    val thumbRequest = remember(thumbModel, ref.key) {
        val memKey = imageLoader.memoryCache?.let { mc ->
            val k360 = MemoryCache.Key(gridThumbCacheKey(ref.key, 360))
            val k256 = MemoryCache.Key(gridThumbCacheKey(ref.key, 256))
            when {
                mc[k360] != null -> k360
                mc[k256] != null -> k256
                else -> null
            }
        }
        ImageRequest.Builder(context)
            .data(thumbModel)
            .size(600)
            .apply {
                if (memKey != null) placeholderMemoryCacheKey(memKey)
                if (thumbModel is VaultImageRequest) {
                    diskCachePolicy(CachePolicy.DISABLED)
                }
            }
            .build()
    }

    val exoPlayer: ExoPlayer? = remember(ref.key, isActive, playerGeneration) {
        if (!isActive) {
            null
        } else {
            runCatching {
                val renderersFactory = DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true)
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(2000, 8000, 250, 500)
                    .build()
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()
                val shouldAutoPlay = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                ExoPlayer.Builder(context, renderersFactory)
                    .setLoadControl(loadControl)
                    .setAudioAttributes(audioAttributes, true)
                    .build()
                    .apply {
                        when (ref) {
                            is MediaItemRef.Gallery -> {
                                setMediaItem(MediaItem.fromUri(ref.item.uri))
                            }
                            is MediaItemRef.Vault -> {
                                val encFile = vaultFileProvider(ref.item)
                                val factory = VaultMediaDataSource.Factory(encFile, cryptoManager)
                                val mediaSource = ProgressiveMediaSource.Factory(factory)
                                    .createMediaSource(MediaItem.fromUri(Uri.parse("vault://${ref.item.id}")))
                                setMediaSource(mediaSource)
                            }
                        }
                        repeatMode = Player.REPEAT_MODE_ONE
                        volume = if (isMuted) 0f else 1f
                        playWhenReady = shouldAutoPlay
                        prepare()
                    }
            }.onFailure { err ->
                playbackError = err.localizedMessage ?: "Unable to initialize video player"
            }.getOrNull()
        }
    }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer, lifecycleOwner) {
            playbackError = null
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING ||
                exoPlayer.playbackState == Player.STATE_IDLE
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (playing) isBuffering = false
                }

                override fun onRenderedFirstFrame() {
                    isBuffering = false
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> isBuffering = true
                        Player.STATE_READY -> {
                            isBuffering = false
                            val d = exoPlayer.duration
                            if (d > 0L) durationMs = d
                        }
                        Player.STATE_ENDED, Player.STATE_IDLE -> {
                            isBuffering = false
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    isBuffering = false
                    isPlaying = false
                    playbackError = error.localizedMessage ?: "Video playback failed"
                }
            }
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                        runCatching {
                            exoPlayer.playWhenReady = false
                            exoPlayer.pause()
                        }
                        isPlaying = false
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        runCatching {
                            exoPlayer.playWhenReady = false
                            exoPlayer.stop()
                            exoPlayer.clearVideoSurface()
                            exoPlayer.release()
                        }
                        isPlaying = false
                    }
                    else -> Unit
                }
            }
            exoPlayer.addListener(listener)
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                runCatching {
                    exoPlayer.removeListener(listener)
                    exoPlayer.playWhenReady = false
                    exoPlayer.stop()
                    exoPlayer.clearVideoSurface()
                    exoPlayer.release()
                }
            }
        }

        // Poll playhead position strictly while playing so paused video performs zero periodic wakeups
        LaunchedEffect(exoPlayer) {
            snapshotFlow { isPlaying }
                .collectLatest { playing ->
                    if (!playing) {
                        runCatching {
                            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                        }
                        return@collectLatest
                    }
                    while (true) {
                        runCatching {
                            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                            val d = exoPlayer.duration
                            if (d > 0L) durationMs = d
                            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
                        }
                        delay(180L)
                    }
                }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleChrome
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = thumbRequest,
                imageLoader = imageLoader,
                contentDescription = ref.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            if (exoPlayer != null && playbackError == null) {
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
                            player = exoPlayer
                        }
                    },
                    update = { playerView ->
                        if (playerView.player !== exoPlayer) {
                            playerView.player = exoPlayer
                        }
                    },
                    onRelease = { playerView ->
                        playerView.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (isActive && isBuffering && playbackError == null) {
            GlassSurface(
                shape = CircleShape,
                strongFill = true,
                elevation = 8.dp
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(14.dp)
                        .size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (!isActive) {
            GlassSurface(
                shape = CircleShape,
                strongFill = true,
                elevation = 8.dp
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(28.dp)
                )
            }
        }

        playbackError?.let { errMsg ->
            GlassSurface(
                shape = RoundedCornerShape(24.dp),
                strongFill = true,
                elevation = 12.dp,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Unable to play video",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = errMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.86f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = {
                            playbackError = null
                            isBuffering = true
                            playerGeneration++
                        }
                    ) {
                        Text("Retry", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        val viewerMotion = LocalMotionConfig.current
        val videoDensity = LocalDensity.current
        val targetScrubberShiftPx = with(videoDensity) { scrubberTargetBottom.toPx() }
        val scrubberShiftPx by animateFloatAsState(
            targetValue = targetScrubberShiftPx,
            animationSpec = if (viewerMotion.prefersReducedMotion) tween(80) else MotionTokens.SnappySpring,
            label = "scrubber_translation_y"
        )
        AnimatedVisibility(
            visible = chromeVisible && exoPlayer != null && playbackError == null,
            enter = viewerMotion.fadeEnter(MotionTokens.MicroMs),
            exit = viewerMotion.fadeExit(MotionTokens.MicroFastMs),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .graphicsLayer { translationY = -scrubberShiftPx }
        ) {
            GlassSurface(
                shape = CircleShape,
                strongFill = true,
                elevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            runCatching {
                                if (exoPlayer?.isPlaying == true) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer?.play()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("video_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = DateGroupingUtils.formatDuration(currentPosMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )

                    Slider(
                        value = (currentPosMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f),
                        onValueChange = { frac ->
                            val target = (frac * durationMs).toLong()
                            currentPosMs = target
                            runCatching { exoPlayer?.seekTo(target) }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = DateGroupingUtils.formatDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.86f)
                    )

                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isMuted = !isMuted
                            runCatching { exoPlayer?.volume = if (isMuted) 0f else 1f }
                        },
                        modifier = Modifier.testTag("video_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
