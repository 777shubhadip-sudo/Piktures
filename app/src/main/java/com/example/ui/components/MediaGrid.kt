package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.repository.VaultImageRequest
import com.example.domain.model.MediaItemRef
import com.example.domain.model.MediaType
import androidx.compose.ui.graphics.TransformOrigin
import com.example.ui.theme.FavoriteRose
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.verticalRubberBand
import com.example.ui.viewmodel.DateGroupingUtils
import com.example.ui.viewmodel.DateSection
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp

private sealed interface GridEntry<out T : MediaItemRef> {
    val stableKey: String

    data class Header(val title: String, val itemCount: Int) : GridEntry<Nothing> {
        override val stableKey: String = "hdr_$title"
    }

    data class Media<T : MediaItemRef>(
        val ref: T,
        val flatIndex: Int,
        val sectionTitle: String
    ) : GridEntry<T> {
        override val stableKey: String = ref.key
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val SharedMediaBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

fun gridThumbSizeBucket(columns: Int): Int = if (columns <= 3) 360 else 256

fun gridThumbCacheKey(refKey: String, thumbBucket: Int): String = "grid_thumb_${refKey}_$thumbBucket"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T : MediaItemRef> MediaSectionsGrid(
    sections: List<DateSection<T>>,
    columns: Int,
    selectedKeys: Set<String>,
    imageLoader: ImageLoader,
    dataProvider: (T) -> Any,
    onColumnsChanged: (Int) -> Unit,
    onItemClick: (T, Int) -> Unit,
    onToggleSelect: (T) -> Unit,
    onSetSelect: (T, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 110.dp),
    sharedElementPrefix: String = "",
    activeSharedKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onNearEnd: (() -> Unit)? = null,
    allowSelection: Boolean = true,
    headerContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val hazeState = LocalHazeState.current
    val headerCount = if (headerContent != null) 1 else 0
    val thumbBucket = gridThumbSizeBucket(columns)

    val entries = remember(sections) {
        buildList {
            var flatIdx = 0
            val seenHeaderKeys = HashSet<String>()
            val seenMediaKeys = HashSet<String>()
            for (section in sections) {
                if (seenHeaderKeys.add(section.header)) {
                    add(GridEntry.Header(section.header, section.items.size))
                }
                for (item in section.items) {
                    if (seenMediaKeys.add(item.key)) {
                        add(GridEntry.Media(item, flatIdx++, section.header))
                    }
                }
            }
        }
    }

    val currentEntries by rememberUpdatedState(entries)
    val currentSelectedKeys by rememberUpdatedState(selectedKeys)
    val currentColumns by rememberUpdatedState(columns)
    val currentHeaderCount by rememberUpdatedState(headerCount)
    val currentOnNearEnd by rememberUpdatedState(onNearEnd)
    val currentDataProvider by rememberUpdatedState(dataProvider)

    // Eagerly pre-warm initial grid thumbnails so fast scrolling starts with warm cache
    LaunchedEffect(entries, thumbBucket) {
        if (entries.isEmpty()) return@LaunchedEffect
        delay(30L)
        var warmed = 0
        val warmupTarget = (currentColumns * 10).coerceAtLeast(30)
        for (entry in entries) {
            if (warmed >= warmupTarget) break
            val mediaEntry = entry as? GridEntry.Media ?: continue
            val memKeyStr = gridThumbCacheKey(mediaEntry.ref.key, thumbBucket)
            val memKey = MemoryCache.Key(memKeyStr)
            if (imageLoader.memoryCache?.get(memKey) == null) {
                val data = currentDataProvider(mediaEntry.ref)
                val isVault = data is VaultImageRequest
                val req = ImageRequest.Builder(context)
                    .data(data)
                    .size(thumbBucket)
                    .memoryCacheKey(memKey)
                    .allowRgb565(true)
                    .apply {
                        if (isVault) diskCachePolicy(CachePolicy.DISABLED)
                    }
                    .build()
                imageLoader.enqueue(req)
            }
            warmed++
        }
    }

    // Scroll-ahead thumbnail prefetching & incremental MediaStore pagination trigger
    LaunchedEffect(gridState, entries, columns) {
        if (entries.isEmpty()) return@LaunchedEffect
        val prefetchAheadCount = columns * 12
        val nearEndThreshold = columns * 6
        var maxPrefetchedEntryIdx = -1

        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
            .distinctUntilChanged()
            .collectLatest { lastVisibleGridIdx ->
                val lastVisibleEntry = (lastVisibleGridIdx - currentHeaderCount).coerceAtLeast(0)
                if (lastVisibleEntry >= currentEntries.size - nearEndThreshold) {
                    currentOnNearEnd?.invoke()
                }
                val startIdx = maxOf(lastVisibleEntry + 1, maxPrefetchedEntryIdx + 1)
                val endIdx = minOf(currentEntries.lastIndex, lastVisibleEntry + prefetchAheadCount)
                if (startIdx <= endIdx) {
                    for (i in startIdx..endIdx) {
                        val mediaEntry = currentEntries[i] as? GridEntry.Media ?: continue
                        val memKeyStr = gridThumbCacheKey(mediaEntry.ref.key, thumbBucket)
                        val memKey = MemoryCache.Key(memKeyStr)
                        if (imageLoader.memoryCache?.get(memKey) == null) {
                            val data = currentDataProvider(mediaEntry.ref)
                            val isVault = data is VaultImageRequest
                            val req = ImageRequest.Builder(context)
                                .data(data)
                                .size(thumbBucket)
                                .memoryCacheKey(memKey)
                                .allowRgb565(true)
                                .apply {
                                    if (isVault) {
                                        diskCachePolicy(CachePolicy.DISABLED)
                                    }
                                }
                                .build()
                            imageLoader.enqueue(req)
                        }
                    }
                    maxPrefetchedEntryIdx = endIdx
                }
            }
    }

    var isFastScrolling by remember { mutableStateOf(false) }
    var fastScrollFraction by remember { mutableFloatStateOf(0f) }
    val fastScrollTargetFlow = remember { MutableStateFlow<Int?>(null) }

    LaunchedEffect(gridState) {
        fastScrollTargetFlow.collectLatest { target ->
            if (target != null && currentEntries.isNotEmpty()) {
                val safeIndex = (target + currentHeaderCount).coerceIn(0, currentEntries.lastIndex + currentHeaderCount)
                gridState.scrollToItem(safeIndex)
            }
        }
    }

    val activeDateLabel by remember(gridState) {
        derivedStateOf {
            val list = currentEntries
            if (list.isEmpty()) return@derivedStateOf ""
            val entryIdx = (gridState.firstVisibleItemIndex - currentHeaderCount).coerceIn(0, list.lastIndex)
            when (val entry = list.getOrNull(entryIdx)) {
                is GridEntry.Header -> entry.title
                is GridEntry.Media -> entry.sectionTitle
                null -> ""
            }
        }
    }

    val isSelectionMode = selectedKeys.isNotEmpty()
    val cornerRadius = when (columns) {
        2 -> 22.dp
        3 -> 18.dp
        4 -> 14.dp
        else -> 12.dp
    }
    val tileShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val placeholderPainter = remember(glass.shimmerBase) { ColorPainter(glass.shimmerBase) }
    val videoScrimBrush = remember {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))
    }
    val favBadgeBrush = remember {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.42f)))
    }

    val entriesByKey = remember(entries) {
        entries.filterIsInstance<GridEntry.Media<T>>().associateBy { it.ref.key }
    }
    val currentEntriesByKey by rememberUpdatedState(entriesByKey)
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemCoordsMap = remember { HashMap<String, LayoutCoordinates>() }
    var suppressClickState by remember { mutableStateOf<Pair<String, Long>?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // Two-finger pinch gesture to dynamically adjust column density + drag-to-select
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { containerCoords = it }
                .then(
                    if (hazeState != null) {
                        Modifier.hazeSource(state = hazeState, zIndex = 1f)
                    } else {
                        Modifier
                    }
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var zoomAccum = 1f
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount >= 2) {
                                val zoomChange = event.calculateZoom()
                                if (zoomChange.isFinite() && zoomChange != 1f) {
                                    zoomAccum *= zoomChange
                                    val cols = currentColumns
                                    if (zoomAccum > 1.24f && cols > 2) {
                                        zoomAccum = 1f
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onColumnsChanged(cols - 1)
                                    } else if (zoomAccum < 0.80f && cols < 5) {
                                        zoomAccum = 1f
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onColumnsChanged(cols + 1)
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            if (pressedCount == 0) break
                        }
                    }
                }
                .then(
                    if (allowSelection) {
                        Modifier.pointerInput(Unit) {
                            var dragTargetState = true
                            var lastVisitedKey: String? = null
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val hit = findMediaEntryAtOffset(
                                        containerCoords = containerCoords,
                                        itemCoordsMap = itemCoordsMap,
                                        entriesByKey = currentEntriesByKey,
                                        gridState = gridState,
                                        entries = currentEntries,
                                        offset = offset,
                                        headerCount = currentHeaderCount
                                    )
                                    if (hit != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val currentlySelected = currentSelectedKeys.contains(hit.ref.key)
                                        dragTargetState = !currentlySelected
                                        lastVisitedKey = hit.ref.key
                                        suppressClickState = hit.ref.key to (SystemClock.uptimeMillis() + 320L)
                                        onSetSelect(hit.ref, dragTargetState)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val hit = findMediaEntryAtOffset(
                                        containerCoords = containerCoords,
                                        itemCoordsMap = itemCoordsMap,
                                        entriesByKey = currentEntriesByKey,
                                        gridState = gridState,
                                        entries = currentEntries,
                                        offset = change.position,
                                        headerCount = currentHeaderCount
                                    )
                                    if (hit != null && hit.ref.key != lastVisitedKey) {
                                        lastVisitedKey = hit.ref.key
                                        suppressClickState = hit.ref.key to (SystemClock.uptimeMillis() + 320L)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSetSelect(hit.ref, dragTargetState)
                                    }
                                },
                                onDragEnd = {
                                    lastVisitedKey = null
                                },
                                onDragCancel = {
                                    lastVisitedKey = null
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalRubberBand()
                    .testTag("media_grid")
            ) {
                if (headerContent != null) {
                    item(
                        key = "grid_featured_header",
                        contentType = "featured_header",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        headerContent()
                    }
                }

                itemsIndexed(
                    items = entries,
                    key = { _, entry -> entry.stableKey },
                    contentType = { _, entry ->
                        when (entry) {
                            is GridEntry.Header -> "header"
                            is GridEntry.Media -> "media"
                        }
                    },
                    span = { _, entry ->
                        when (entry) {
                            is GridEntry.Header -> GridItemSpan(maxLineSpan)
                            is GridEntry.Media -> GridItemSpan(1)
                        }
                    }
                ) { _, entry ->
                    when (entry) {
                        is GridEntry.Header -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    glass.prismBorderMid.copy(alpha = 0.65f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                GlassSurface(
                                    shape = CircleShape,
                                    elevation = 1.dp,
                                    enableBackdropBlur = false
                                ) {
                                    Text(
                                        text = if (entry.itemCount == 1) "1 item" else "${entry.itemCount} items",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = glass.glassSecondaryText,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        is GridEntry.Media -> {
                            val isSelected = selectedKeys.contains(entry.ref.key)
                            val scale = if (isSelectionMode || isSelected) {
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isSelected) 0.88f else 1f,
                                    animationSpec = motion.floatSpring(allowOvershoot = !motion.prefersReducedMotion),
                                    label = "thumb_select_scale"
                                )
                                animatedScale
                            } else {
                                1f
                            }

                            val fullSharedKey = "${sharedElementPrefix}_${entry.ref.key}"
                            val shouldAttachShared = sharedTransitionScope != null &&
                                animatedVisibilityScope != null &&
                                sharedElementPrefix.isNotBlank() &&
                                activeSharedKey == fullSharedKey

                            val sharedMod = if (shouldAttachShared) {
                                with(sharedTransitionScope!!) {
                                    Modifier.sharedElement(
                                        state = rememberSharedContentState(key = fullSharedKey),
                                        animatedVisibilityScope = animatedVisibilityScope!!,
                                        boundsTransform = SharedMediaBoundsTransform
                                    )
                                }
                            } else {
                                Modifier
                            }

                            DisposableEffect(entry.ref.key) {
                                onDispose {
                                    itemCoordsMap.remove(entry.ref.key)
                                }
                            }

                            MediaGridThumbnail(
                                ref = entry.ref,
                                data = dataProvider(entry.ref),
                                thumbBucket = thumbBucket,
                                imageLoader = imageLoader,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                tileShape = tileShape,
                                placeholderColor = glass.shimmerBase,
                                placeholderPainter = placeholderPainter,
                                videoScrimBrush = videoScrimBrush,
                                favBadgeBrush = favBadgeBrush,
                                modifier = Modifier
                                    .onGloballyPositioned { coords ->
                                        itemCoordsMap[entry.ref.key] = coords
                                    }
                                    .graphicsLayer {
                                        if (scale != 1f) {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                    }
                                    .then(sharedMod)
                                    .testTag("media_tile_${entry.flatIndex}")
                                    .clickable {
                                        val suppressed = suppressClickState
                                        if (suppressed != null &&
                                            suppressed.first == entry.ref.key &&
                                            SystemClock.uptimeMillis() <= suppressed.second
                                        ) {
                                            suppressClickState = null
                                            return@clickable
                                        }
                                        suppressClickState = null
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSelectionMode) {
                                            onToggleSelect(entry.ref)
                                        } else {
                                            onItemClick(entry.ref, entry.flatIndex)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Fast-scroll track + Liquid Glass date bubble
        if (entries.size > 18) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 12.dp, bottom = 112.dp, end = 4.dp)
                    .width(28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                val h = size.height
                                val count = currentEntries.size
                                if (h > 0 && count > 0) {
                                    isFastScrolling = true
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val frac = (offset.y / h.toFloat()).coerceIn(0f, 1f)
                                    fastScrollFraction = frac
                                    val target = (frac * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                    fastScrollTargetFlow.value = target
                                }
                            },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val h = size.height
                                val count = currentEntries.size
                                if (h > 0 && count > 0) {
                                    val frac = (change.position.y / h.toFloat()).coerceIn(0f, 1f)
                                    fastScrollFraction = frac
                                    val target = (frac * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                    fastScrollTargetFlow.value = target
                                }
                            },
                            onDragEnd = {
                                isFastScrolling = false
                                fastScrollTargetFlow.value = null
                            },
                            onDragCancel = {
                                isFastScrolling = false
                                fastScrollTargetFlow.value = null
                            }
                        )
                    }
            ) {
                val trackHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(44f)
                val scrollProgress by remember {
                    derivedStateOf {
                        val count = currentEntries.size
                        if (count <= 1) 0f
                        else if (isFastScrolling) fastScrollFraction
                        else {
                            val first = (gridState.firstVisibleItemIndex - currentHeaderCount).coerceAtLeast(0).toFloat()
                            (first / (count - 1).toFloat()).coerceIn(0f, 1f)
                        }
                    }
                }
                val thumbOffsetY = ((trackHeightPx - 44f) * scrollProgress).roundToInt().coerceAtLeast(0)

                // Thumb pill
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, thumbOffsetY) }
                        .align(Alignment.TopEnd)
                        .width(6.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFastScrolling) MaterialTheme.colorScheme.primary
                            else glass.glassHighlight
                        )
                )

                // Floating glass date bubble during scrub (#7 Origin-aware from track edge)
                AnimatedVisibility(
                    visible = isFastScrolling && activeDateLabel.isNotBlank(),
                    enter = motion.originMenuEnter(TransformOrigin(1f, 0.5f)),
                    exit = motion.originMenuExit(TransformOrigin(1f, 0.5f)),
                    modifier = Modifier
                        .offset { IntOffset(-16, (thumbOffsetY - 10).coerceAtLeast(0)) }
                        .align(Alignment.TopEnd)
                ) {
                    GlassSurface(
                        shape = CircleShape,
                        strongFill = true,
                        elevation = 14.dp,
                        modifier = Modifier.offset(x = (-32).dp)
                    ) {
                        Text(
                            text = activeDateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun <T : MediaItemRef> findMediaEntryAtOffset(
    containerCoords: LayoutCoordinates?,
    itemCoordsMap: Map<String, LayoutCoordinates>,
    entriesByKey: Map<String, GridEntry.Media<T>>,
    gridState: LazyGridState,
    entries: List<GridEntry<T>>,
    offset: Offset,
    headerCount: Int
): GridEntry.Media<T>? {
    if (containerCoords != null && containerCoords.isAttached) {
        for ((key, itemCoords) in itemCoordsMap) {
            if (!itemCoords.isAttached) continue
            val bounds = containerCoords.localBoundingBoxOf(itemCoords, clipBounds = true)
            if (offset.x in bounds.left..bounds.right && offset.y in bounds.top..bounds.bottom) {
                return entriesByKey[key]
            }
        }
        return null
    }
    val layoutInfo = gridState.layoutInfo
    val yOffsetCorrection = -layoutInfo.viewportStartOffset
    val hitInfo = layoutInfo.visibleItemsInfo.firstOrNull { item ->
        val itemTop = item.offset.y + yOffsetCorrection
        offset.x >= item.offset.x &&
            offset.x <= item.offset.x + item.size.width &&
            offset.y >= itemTop &&
            offset.y <= itemTop + item.size.height
    } ?: return null
    val entryIdx = hitInfo.index - headerCount
    return if (entryIdx >= 0) entries.getOrNull(entryIdx) as? GridEntry.Media<T> else null
}

@Composable
private fun MediaGridThumbnail(
    ref: MediaItemRef,
    data: Any,
    thumbBucket: Int,
    imageLoader: ImageLoader,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    tileShape: RoundedCornerShape,
    placeholderColor: Color,
    placeholderPainter: ColorPainter,
    videoScrimBrush: Brush,
    favBadgeBrush: Brush,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVault = data is VaultImageRequest
    val cacheKeyStr = gridThumbCacheKey(ref.key, thumbBucket)

    val request = remember(data, thumbBucket, cacheKeyStr) {
        ImageRequest.Builder(context)
            .data(data)
            .size(thumbBucket)
            .memoryCacheKey(cacheKeyStr)
            .allowRgb565(true)
            .crossfade(120)
            .apply {
                if (isVault) {
                    diskCachePolicy(CachePolicy.DISABLED)
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(tileShape)
            .background(placeholderColor)
            .then(
                if (isSelected) {
                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, tileShape)
                } else {
                    Modifier.border(0.8.dp, Color.White.copy(alpha = 0.45f), tileShape)
                }
            )
    ) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            contentDescription = ref.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (ref.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(videoScrimBrush)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.50f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Video",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                if (ref.durationMs > 0L) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = DateGroupingUtils.formatDuration(ref.durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // Top-right glass circle with heart for favorited items
        if (ref is MediaItemRef.Gallery && ref.item.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(favBadgeBrush)
                    .border(0.8.dp, Color.White.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Favorite",
                    tint = FavoriteRose,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.45f)
                    )
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
