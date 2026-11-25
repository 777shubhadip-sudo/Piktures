package com.example.ui.editor.video

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.editor.common.EditorEmptyStateCard
import com.example.ui.editor.common.EditorGlassTokens
import com.example.ui.editor.common.EditorSliderControl
import com.example.ui.editor.common.GlassPanel
import com.example.ui.editor.common.LocalEditorGlassConfig
import com.example.ui.editor.common.withGlassLegibility
import com.example.ui.editor.photo.EditorActionPill
import com.example.ui.editor.photo.EditorChoiceChip
import com.example.ui.editor.photo.MARKUP_PALETTE_COLORS
import com.example.ui.editor.photo.MarkupFontStyle
import com.example.ui.editor.photo.PhotoFilterPreset
import com.example.ui.editor.photo.toComposeColor
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.SnapSuccessMint
import com.example.ui.theme.horizontalRubberBand
import com.example.ui.theme.tactilePressMotion
import com.example.ui.viewmodel.DateGroupingUtils
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class VideoColorParam(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    WARMTH("Warmth")
}

private enum class VideoFilterSubMode(val label: String) {
    FILTERS("Looks"),
    ADJUST("Color"),
    TEMPLATES("Templates")
}

/**
 * Interactive multi-clip timeline rendered inside a frosted Glass Strip container:
 * - Pinch-to-zoom (2-finger gesture without breaking scroll) & double-tap zoom reset
 * - Accent Playhead line + draggable top cap + magnetic snap alignment indicator
 * - Big draggable Left & Right Trim Handles on the selected clip
 * - Tap clip to select, long-press + drag to reorder
 */
@Composable
fun MultiClipTimelineBar(
    clips: List<VideoClipItem>,
    selectedClipId: String?,
    playheadMs: Long,
    totalDurationMs: Long,
    clipThumbnails: Map<String, List<Bitmap>>,
    onSelectClip: (String) -> Unit,
    onMoveClip: (Int, Int) -> Unit,
    onSeekTo: (Long) -> Unit,
    onPreviewTrim: (Long, Long) -> Unit,
    onCommitTrim: () -> Unit,
    onAddClip: () -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val zoomAnim = remember { Animatable(1f) }
    val zoomScale = zoomAnim.value
    var snapLabel by remember { mutableStateOf<String?>(null) }
    var snapFraction by remember { mutableStateOf<Float?>(null) }

    val pxPerSec = 36f * zoomScale
    val clipSpacingDp = 6.dp
    val clipWidthsDp: List<Dp> = remember(clips, zoomScale) {
        clips.map { clip ->
            ((clip.effectiveDurationMs / 1000f) * pxPerSec).coerceIn(88f, 340f).dp
        }
    }
    val totalTrackWidthDp: Dp = remember(clipWidthsDp) {
        val sum = clipWidthsDp.fold(0.dp) { acc, w -> acc + w }
        val gaps = clipSpacingDp * (clips.size - 1).coerceAtLeast(0)
        (sum + gaps).coerceAtLeast(88.dp)
    }

    val playheadOffsetDp: Dp = remember(clips, clipWidthsDp, playheadMs) {
        var accDp = 0.dp
        var remaining = playheadMs.coerceAtLeast(0L)
        for (i in clips.indices) {
            val dur = clips[i].effectiveDurationMs.coerceAtLeast(1L)
            val wDp = clipWidthsDp[i]
            if (remaining <= dur || i == clips.lastIndex) {
                val frac = (remaining.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                accDp += wDp * frac
                break
            } else {
                accDp += wDp + clipSpacingDp
                remaining -= dur
            }
        }
        accDp
    }

    val scrollState = rememberScrollState()

    GlassPanel(
        shape = RoundedCornerShape(22.dp),
        strongFill = true,
        enableBlur = true,
        elevation = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val selectedIdx = clips.indexOfFirst { it.id == selectedClipId }
                    val statusText = when {
                        snapLabel != null -> snapLabel!!
                        selectedIdx >= 0 -> "Clip ${selectedIdx + 1} of ${clips.size} selected"
                        else -> "${clips.size} clip${if (clips.size == 1) "" else "s"}"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium
                            .copy(fontWeight = FontWeight.SemiBold)
                            .withGlassLegibility(config),
                        color = if (snapLabel != null) config.accentColor else config.contentSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Pinch zoom pill (double-tap to reset to 1x)
                GlassPanel(
                    shape = CircleShape,
                    enableBlur = false,
                    elevation = 0.dp,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        zoomAnim.animateTo(1f, motion.floatSpring(allowOvershoot = false))
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ZoomIn,
                            contentDescription = "Pinch timeline to zoom",
                            tint = config.contentSecondaryColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", zoomScale)}x",
                            style = MaterialTheme.typography.labelSmall
                                .copy(fontWeight = FontWeight.Bold)
                                .withGlassLegibility(config),
                            color = config.contentSecondaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${DateGroupingUtils.formatDuration(playheadMs)} / ${DateGroupingUtils.formatDuration(totalDurationMs)}",
                    style = MaterialTheme.typography.labelMedium
                        .copy(fontWeight = FontWeight.Bold)
                        .withGlassLegibility(config),
                    color = config.accentColor
                )
            }

            // Scrollable + Pinch-to-Zoom Timeline Strip with Playhead & Trim Handles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    if (abs(zoomChange - 1f) > 0.005f) {
                                        val nextZoom = (zoomAnim.value * zoomChange).coerceIn(0.6f, 3.0f)
                                        scope.launch { zoomAnim.snapTo(nextZoom) }
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalRubberBand()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(totalTrackWidthDp)
                            .fillMaxHeight()
                    ) {
                        val latestTotalDurationMs by rememberUpdatedState(totalDurationMs)
                        val latestPlayheadMs by rememberUpdatedState(playheadMs)
                        val latestOnPreviewTrim by rememberUpdatedState(onPreviewTrim)
                        val latestOnCommitTrim by rememberUpdatedState(onCommitTrim)
                        val latestOnSeekTo by rememberUpdatedState(onSeekTo)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(clipSpacingDp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 7.dp)
                        ) {
                            clips.forEachIndexed { index, clip ->
                                val isSelected = clip.id == selectedClipId
                                val thumbs = clipThumbnails[clip.uriString].orEmpty()
                                val widthDp = clipWidthsDp[index]
                                val widthPx = with(density) { widthDp.toPx() }.coerceAtLeast(1f)
                                var dragAccum by remember(clip.id) { mutableFloatStateOf(0f) }

                                Box(
                                    modifier = Modifier
                                        .width(widthDp)
                                        .fillMaxHeight()
                                        .then(
                                            if (isSelected) {
                                                Modifier.shadow(
                                                    elevation = 8.dp,
                                                    shape = RoundedCornerShape(14.dp),
                                                    ambientColor = config.accentColor.copy(alpha = 0.55f),
                                                    spotColor = config.accentColor.copy(alpha = 0.55f)
                                                )
                                            } else Modifier
                                        )
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) config.accentColor else Color.White.copy(alpha = 0.28f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .pointerInput(clip.id, index, clips.size) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    dragAccum = 0f
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onSelectClip(clip.id)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragAccum += dragAmount.x
                                                    if (dragAccum > 72f && index < clips.lastIndex) {
                                                        onMoveClip(index, index + 1)
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        dragAccum = 0f
                                                    } else if (dragAccum < -72f && index > 0) {
                                                        onMoveClip(index, index - 1)
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        dragAccum = 0f
                                                    }
                                                },
                                                onDragEnd = { dragAccum = 0f },
                                                onDragCancel = { dragAccum = 0f }
                                            )
                                        }
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectClip(clip.id)
                                        }
                                        .testTag("timeline_clip_$index")
                                ) {
                                    if (thumbs.isNotEmpty()) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            thumbs.forEach { frame ->
                                                val img = remember(frame) { frame.asImageBitmap() }
                                                Image(
                                                    bitmap = img,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(horizontal = 24.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = DateGroupingUtils.formatDuration(clip.effectiveDurationMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        if (abs(clip.speed - 1f) > 0.05f) {
                                            Text(
                                                text = "${String.format("%.1f", clip.speed)}x",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = config.accentColor
                                            )
                                        }
                                    }

                                    // Draggable LEFT & RIGHT trim handles on selected clip
                                    if (isSelected && clip.sourceDurationMs > 500L) {
                                        val msPerPx = (clip.trimmedSourceDurationMs.toFloat() / widthPx).coerceAtLeast(4f)
                                        val latestClip by rememberUpdatedState(clip)
                                        val latestMsPerPx by rememberUpdatedState(msPerPx)

                                        // Left Trim Handle
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .width(22.dp)
                                                .fillMaxHeight()
                                                .background(config.accentColor)
                                                .pointerInput(clip.id) {
                                                    var lastSnappedMs = -1L
                                                    var draftStartMs = 0L
                                                    var draftEndMs = 0L
                                                    var activeMsPerPx = 4f
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            val c = latestClip
                                                            draftStartMs = c.trimStartMs
                                                            draftEndMs = c.trimEndMs
                                                            activeMsPerPx = latestMsPerPx
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        },
                                                        onDrag = { change, drag ->
                                                            change.consume()
                                                            val maxStart = (draftEndMs - 300L).coerceAtLeast(0L)
                                                            draftStartMs = (draftStartMs + (drag.x * activeMsPerPx).roundToLong())
                                                                .coerceIn(0L, maxStart)
                                                            val snappedGrid = ((draftStartMs + 250L) / 500L) * 500L
                                                            val curTotal = latestTotalDurationMs.coerceAtLeast(1L)
                                                            val finalStart = if (abs(draftStartMs - snappedGrid) <= 95L) {
                                                                if (lastSnappedMs != snappedGrid) {
                                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    lastSnappedMs = snappedGrid
                                                                }
                                                                snapLabel = "Snap ${DateGroupingUtils.formatDuration(snappedGrid)}"
                                                                snapFraction = (snappedGrid.toFloat() / curTotal.toFloat()).coerceIn(0f, 1f)
                                                                snappedGrid.coerceIn(0L, maxStart)
                                                            } else {
                                                                snapLabel = null
                                                                snapFraction = null
                                                                draftStartMs
                                                            }
                                                            latestOnPreviewTrim(finalStart, draftEndMs)
                                                        },
                                                        onDragEnd = {
                                                            snapLabel = null
                                                            snapFraction = null
                                                            latestOnCommitTrim()
                                                        },
                                                        onDragCancel = {
                                                            snapLabel = null
                                                            snapFraction = null
                                                            latestOnCommitTrim()
                                                        }
                                                    )
                                                }
                                                .semantics { contentDescription = "Trim clip start handle" }
                                                .testTag("timeline_trim_start_handle"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(22.dp)
                                                    .clip(CircleShape)
                                                    .background(EditorGlassTokens.DarkOnAccent)
                                            )
                                        }

                                        // Right Trim Handle
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .width(22.dp)
                                                .fillMaxHeight()
                                                .background(config.accentColor)
                                                .pointerInput(clip.id) {
                                                    var lastSnappedMs = -1L
                                                    var draftStartMs = 0L
                                                    var draftEndMs = 0L
                                                    var activeMsPerPx = 4f
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            val c = latestClip
                                                            draftStartMs = c.trimStartMs
                                                            draftEndMs = c.trimEndMs
                                                            activeMsPerPx = latestMsPerPx
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        },
                                                        onDrag = { change, drag ->
                                                            change.consume()
                                                            val maxEnd = latestClip.sourceDurationMs
                                                            val minEnd = (draftStartMs + 300L).coerceAtMost(maxEnd)
                                                            draftEndMs = (draftEndMs + (drag.x * activeMsPerPx).roundToLong())
                                                                .coerceIn(minEnd, maxEnd)
                                                            val snappedGrid = ((draftEndMs + 250L) / 500L) * 500L
                                                            val curTotal = latestTotalDurationMs.coerceAtLeast(1L)
                                                            val finalEnd = if (abs(draftEndMs - snappedGrid) <= 95L) {
                                                                if (lastSnappedMs != snappedGrid) {
                                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    lastSnappedMs = snappedGrid
                                                                }
                                                                snapLabel = "Snap ${DateGroupingUtils.formatDuration(snappedGrid)}"
                                                                snapFraction = (snappedGrid.toFloat() / curTotal.toFloat()).coerceIn(0f, 1f)
                                                                snappedGrid.coerceIn(minEnd, maxEnd)
                                                            } else {
                                                                snapLabel = null
                                                                snapFraction = null
                                                                draftEndMs
                                                            }
                                                            latestOnPreviewTrim(draftStartMs, finalEnd)
                                                        },
                                                        onDragEnd = {
                                                            snapLabel = null
                                                            snapFraction = null
                                                            latestOnCommitTrim()
                                                        },
                                                        onDragCancel = {
                                                            snapLabel = null
                                                            snapFraction = null
                                                            latestOnCommitTrim()
                                                        }
                                                    )
                                                }
                                                .semantics { contentDescription = "Trim clip end handle" }
                                                .testTag("timeline_trim_end_handle"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(22.dp)
                                                    .clip(CircleShape)
                                                    .background(EditorGlassTokens.DarkOnAccent)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Snap alignment indicator line
                        val snapFrac = snapFraction
                        if (snapFrac != null) {
                            val snapX = with(density) { (totalTrackWidthDp * snapFrac).roundToPx() }
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(snapX, 0) }
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(SnapSuccessMint)
                            )
                        }

                        // Glowing accent Playhead line + draggable orb cap
                        val playheadPx = with(density) { playheadOffsetDp.roundToPx() }
                        val trackWidthPx = with(density) { totalTrackWidthDp.toPx() }.coerceAtLeast(1f)
                        val latestTrackWidthPx by rememberUpdatedState(trackWidthPx)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .offset { IntOffset((playheadPx - with(density) { 10.dp.roundToPx() }), 0) }
                                .width(20.dp)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    var draftPlayheadMs = 0L
                                    detectDragGestures(
                                        onDragStart = {
                                            draftPlayheadMs = latestPlayheadMs
                                        },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            val curTotal = latestTotalDurationMs.coerceAtLeast(1L)
                                            val curWidth = latestTrackWidthPx.coerceAtLeast(1f)
                                            val deltaMs = ((drag.x / curWidth) * curTotal).roundToLong()
                                            draftPlayheadMs = (draftPlayheadMs + deltaMs).coerceIn(0L, curTotal)
                                            latestOnSeekTo(draftPlayheadMs)
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 14.dp, height = 10.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(4.dp),
                                        ambientColor = config.accentColor,
                                        spotColor = config.accentColor
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .border(1.dp, config.accentColor, RoundedCornerShape(4.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .weight(1f)
                                    .background(config.accentColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Glass Add clip tile
                    GlassPanel(
                        shape = RoundedCornerShape(14.dp),
                        active = true,
                        enableBlur = false,
                        elevation = 4.dp,
                        modifier = Modifier
                            .size(width = 56.dp, height = 60.dp)
                            .clickable(role = Role.Button) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAddClip()
                            }
                            .testTag("timeline_add_clip")
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add video clip",
                                tint = config.accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoTrimPanel(
    selectedClip: VideoClipItem?,
    selectedIndex: Int,
    clipCount: Int,
    aspectRatio: VideoAspectRatio,
    onAddClip: () -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDeleteClip: () -> Unit,
    onSelectAspect: (VideoAspectRatio) -> Unit,
    onRotate90: () -> Unit,
    onFlip: () -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    var showCropTray by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                EditorActionPill(
                    icon = Icons.Rounded.ContentCut,
                    label = "Split",
                    selected = false,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSplitAtPlayhead()
                    },
                    modifier = Modifier.testTag("timeline_split")
                )
            }
            item {
                EditorActionPill(
                    icon = Icons.Rounded.Crop,
                    label = "Crop & Rotate",
                    selected = showCropTray || aspectRatio != VideoAspectRatio.ORIGINAL ||
                        (selectedClip?.rotationDegrees ?: 0) != 0 || selectedClip?.flipHorizontal == true,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCropTray = !showCropTray
                    },
                    modifier = Modifier.testTag("video_toggle_crop")
                )
            }
            item {
                EditorActionPill(
                    icon = Icons.Rounded.Add,
                    label = "Add Clip",
                    selected = false,
                    onClick = onAddClip
                )
            }
            if (clipCount > 1) {
                item {
                    EditorActionPill(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        label = "Move Left",
                        selected = false,
                        onClick = onMoveLeft
                    )
                }
                item {
                    EditorActionPill(
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        label = "Move Right",
                        selected = false,
                        onClick = onMoveRight
                    )
                }
                item {
                    EditorActionPill(
                        icon = Icons.Rounded.Delete,
                        label = "Delete",
                        selected = false,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteClip()
                        },
                        modifier = Modifier.testTag("timeline_delete")
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showCropTray,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorActionPill(
                    icon = Icons.AutoMirrored.Rounded.RotateRight,
                    label = "Rotate",
                    selected = (selectedClip?.rotationDegrees ?: 0) != 0,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRotate90()
                    },
                    modifier = Modifier.testTag("video_rotate_90")
                )
                EditorActionPill(
                    icon = Icons.Rounded.Flip,
                    label = "Flip",
                    selected = selectedClip?.flipHorizontal == true,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFlip()
                    }
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(VideoAspectRatio.entries) { ratio ->
                        EditorChoiceChip(
                            label = ratio.label,
                            selected = ratio == aspectRatio,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectAspect(ratio)
                            },
                            modifier = Modifier.testTag("video_aspect_${ratio.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (selectedClip != null) {
            Text(
                text = "Clip ${selectedIndex + 1} trim: ${DateGroupingUtils.formatDuration(selectedClip.trimStartMs)} – ${DateGroupingUtils.formatDuration(selectedClip.trimEndMs)}",
                style = MaterialTheme.typography.labelMedium.withGlassLegibility(config),
                color = config.contentSecondaryColor
            )
        }
    }
}

@Composable
fun VideoSpeedPanel(
    selectedClip: VideoClipItem?,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onApplyWholeClipSpeed: (Float) -> Unit,
    onApplySegmentSpeed: (Long, Long, Float) -> Unit,
    onToggleStabilize: () -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    val baseSpeed = selectedClip?.speed ?: 1f
    val spanMs = selectedClip?.trimmedSourceDurationMs ?: 1000L
    var segmentMode by remember { mutableStateOf(false) }
    var draftSpeed by remember(selectedClip?.id, baseSpeed) { mutableFloatStateOf(baseSpeed) }
    var segStart by remember(selectedClip?.id, spanMs) { mutableFloatStateOf(0f) }
    var segEnd by remember(selectedClip?.id, spanMs) { mutableFloatStateOf(spanMs.toFloat()) }

    val speedPresets = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 4f)
    val stabilizeEnabled = selectedClip?.stabilize == true

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(
            visible = !isSliderDragging,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorChoiceChip(
                    label = "Whole Clip",
                    selected = !segmentMode,
                    onClick = { segmentMode = false }
                )
                EditorChoiceChip(
                    label = "Segment",
                    selected = segmentMode,
                    onClick = { segmentMode = true }
                )
            }
        }

        EditorSliderControl(
            label = "Playback Speed",
            value = draftSpeed,
            valueRange = 0.25f..4f,
            defaultValue = 1f,
            valueText = "${String.format("%.2f", draftSpeed)}x",
            onValueChange = { draftSpeed = it },
            onValueChangeFinished = {
                if (!segmentMode) onApplyWholeClipSpeed(draftSpeed)
            },
            onReset = {
                draftSpeed = 1f
                if (!segmentMode) onApplyWholeClipSpeed(1f)
            },
            onDragStateChanged = onSliderDraggingChange,
            sliderTag = "video_speed_slider",
            resetTag = "video_speed_reset"
        )

        AnimatedVisibility(
            visible = !isSliderDragging,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(speedPresets) { p ->
                        EditorChoiceChip(
                            label = "${p}x",
                            selected = abs(draftSpeed - p) < 0.05f,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                draftSpeed = p
                                if (!segmentMode) onApplyWholeClipSpeed(p)
                            }
                        )
                    }
                }

                if (segmentMode && spanMs > 800L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RangeSlider(
                            value = segStart..segEnd.coerceAtLeast(segStart + 300f),
                            onValueChange = { r ->
                                segStart = r.start
                                segEnd = r.endInclusive
                            },
                            valueRange = 0f..spanMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = config.accentColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        )
                        EditorActionPill(
                            icon = Icons.Rounded.Speed,
                            label = "Apply",
                            selected = true,
                            onClick = { onApplySegmentSpeed(segStart.toLong(), segEnd.toLong(), draftSpeed) }
                        )
                    }
                }

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable(role = Role.Switch) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleStabilize()
                        },
                    shape = RoundedCornerShape(18.dp),
                    active = stabilizeEnabled,
                    enableBlur = false,
                    elevation = if (stabilizeEnabled) 8.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = "Stabilize video",
                            tint = if (stabilizeEnabled) config.accentColor else config.contentPrimaryColor
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smooth Shaky Video",
                                style = MaterialTheme.typography.titleSmall
                                    .copy(fontWeight = FontWeight.Bold)
                                    .withGlassLegibility(config),
                                color = config.contentPrimaryColor
                            )
                            Text(
                                text = if (stabilizeEnabled) "Enabled (slight edge crop for steadiness)" else "Reduce handheld camera shake",
                                style = MaterialTheme.typography.labelSmall.withGlassLegibility(config),
                                color = config.contentSecondaryColor
                            )
                        }
                        Switch(
                            checked = stabilizeEnabled,
                            onCheckedChange = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleStabilize()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EditorGlassTokens.DarkOnAccent,
                                checkedTrackColor = config.accentColor
                            ),
                            modifier = Modifier.testTag("video_stabilize_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoFiltersPanel(
    color: VideoColorAdjustments,
    filterThumbnails: Map<PhotoFilterPreset, Bitmap>,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onSelectFilter: (PhotoFilterPreset) -> Unit,
    onPreviewColor: (VideoColorAdjustments) -> Unit,
    onCommitColor: () -> Unit,
    onResetFilterIntensity: () -> Unit,
    onResetColorParam: (VideoColorParam) -> Unit,
    onApplyTemplate: (VideoTemplatePreset) -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    var subMode by remember { mutableStateOf(VideoFilterSubMode.FILTERS) }
    var selectedParam by remember { mutableStateOf(VideoColorParam.BRIGHTNESS) }

    val currentVal = when (selectedParam) {
        VideoColorParam.BRIGHTNESS -> color.brightness
        VideoColorParam.CONTRAST -> color.contrast
        VideoColorParam.SATURATION -> color.saturation
        VideoColorParam.WARMTH -> color.warmth
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(
            visible = !isSliderDragging,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoFilterSubMode.entries.forEach { mode ->
                    EditorChoiceChip(
                        label = mode.label,
                        selected = mode == subMode,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            subMode = mode
                        },
                        modifier = Modifier.testTag("video_filters_sub_${mode.name.lowercase()}")
                    )
                }
            }
        }

        when (subMode) {
            VideoFilterSubMode.FILTERS -> {
                if (color.filter != PhotoFilterPreset.ORIGINAL) {
                    EditorSliderControl(
                        label = "${color.filter.label} Strength",
                        value = color.filterIntensity,
                        valueRange = 0f..1f,
                        defaultValue = 1f,
                        valueText = "${(color.filterIntensity * 100f).roundToInt()}%",
                        onValueChange = { onPreviewColor(color.copy(filterIntensity = it)) },
                        onValueChangeFinished = onCommitColor,
                        onReset = onResetFilterIntensity,
                        onDragStateChanged = onSliderDraggingChange,
                        sliderTag = "video_filter_intensity_slider",
                        resetTag = "video_filter_intensity_reset"
                    )
                }

                AnimatedVisibility(
                    visible = !isSliderDragging,
                    enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
                    exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.horizontalRubberBand()
                    ) {
                        itemsIndexed(PhotoFilterPreset.entries) { idx, preset ->
                            val selected = color.filter == preset
                            val bmp = filterThumbnails[preset]
                            val interaction = remember { MutableInteractionSource() }
                            GlassPanel(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                                    .tactilePressMotion(interaction, squashAmount = 0.045f)
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null,
                                        role = Role.Button
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectFilter(preset)
                                    }
                                    .testTag("video_filter_thumb_${preset.name.lowercase()}"),
                                shape = RoundedCornerShape(16.dp),
                                active = selected,
                                enableBlur = false,
                                elevation = if (selected) 10.dp else 0.dp
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bmp != null) {
                                            val img = remember(bmp) { bmp.asImageBitmap() }
                                            Image(
                                                bitmap = img,
                                                contentDescription = preset.label,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.matchParentSize()
                                            )
                                        }
                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(config.accentColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected filter",
                                                    tint = EditorGlassTokens.DarkOnAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelMedium
                                            .copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                            .withGlassLegibility(config),
                                        color = if (selected) config.accentColor else config.contentPrimaryColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            VideoFilterSubMode.ADJUST -> {
                EditorSliderControl(
                    label = selectedParam.label,
                    value = currentVal,
                    valueRange = -100f..100f,
                    defaultValue = 0f,
                    valueText = if (currentVal > 0f) "+${currentVal.roundToInt()}" else "${currentVal.roundToInt()}",
                    onValueChange = { v ->
                        val updated = when (selectedParam) {
                            VideoColorParam.BRIGHTNESS -> color.copy(brightness = v)
                            VideoColorParam.CONTRAST -> color.copy(contrast = v)
                            VideoColorParam.SATURATION -> color.copy(saturation = v)
                            VideoColorParam.WARMTH -> color.copy(warmth = v)
                        }
                        onPreviewColor(updated)
                    },
                    onValueChangeFinished = onCommitColor,
                    onReset = { onResetColorParam(selectedParam) },
                    onDragStateChanged = onSliderDraggingChange,
                    sliderTag = "video_color_slider",
                    resetTag = "video_color_reset"
                )

                AnimatedVisibility(
                    visible = !isSliderDragging,
                    enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
                    exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalRubberBand()
                    ) {
                        items(VideoColorParam.entries) { p ->
                            val v = when (p) {
                                VideoColorParam.BRIGHTNESS -> color.brightness
                                VideoColorParam.CONTRAST -> color.contrast
                                VideoColorParam.SATURATION -> color.saturation
                                VideoColorParam.WARMTH -> color.warmth
                            }.roundToInt()
                            val chipLabel = if (v != 0) "${p.label} (${if (v > 0) "+$v" else "$v"})" else p.label
                            EditorChoiceChip(
                                label = chipLabel,
                                selected = p == selectedParam,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedParam = p
                                }
                            )
                        }
                    }
                }
            }

            VideoFilterSubMode.TEMPLATES -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.horizontalRubberBand()
                ) {
                    items(VideoTemplatePreset.entries) { preset ->
                        val interaction = remember { MutableInteractionSource() }
                        GlassPanel(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 118.dp, minHeight = 52.dp)
                                .tactilePressMotion(interaction, squashAmount = 0.045f)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.Button
                                ) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onApplyTemplate(preset)
                                }
                                .testTag("video_template_${preset.name.lowercase()}"),
                            shape = RoundedCornerShape(18.dp),
                            enableBlur = false,
                            elevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesomeMosaic,
                                        contentDescription = null,
                                        tint = config.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelLarge
                                            .copy(fontWeight = FontWeight.Bold)
                                            .withGlassLegibility(config),
                                        color = config.contentPrimaryColor
                                    )
                                }
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.labelSmall.withGlassLegibility(config),
                                    color = config.contentSecondaryColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoTextPanel(
    overlays: List<VideoTextOverlay>,
    selectedOverlayId: String?,
    totalDurationMs: Long,
    onAddNewText: () -> Unit,
    onSelectOverlay: (VideoTextOverlay) -> Unit,
    onEditOverlayText: (VideoTextOverlay) -> Unit,
    onUpdateOverlay: (VideoTextOverlay) -> Unit,
    onDeleteOverlay: (String) -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val haptics = LocalHapticFeedback.current
    val active = overlays.firstOrNull { it.id == selectedOverlayId } ?: overlays.lastOrNull()

    if (overlays.isEmpty()) {
        EditorEmptyStateCard(
            icon = Icons.Rounded.TextFields,
            title = "No text added yet",
            subtitle = "Add titles or captions, drag them anywhere on the video, and set when they appear.",
            actionLabel = "Add Text",
            onAction = onAddNewText
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                EditorActionPill(
                    icon = Icons.Rounded.Add,
                    label = "New Text",
                    selected = true,
                    onClick = onAddNewText,
                    modifier = Modifier.testTag("video_add_text")
                )
            }
            items(overlays, key = { it.id }) { item ->
                EditorChoiceChip(
                    label = item.text.take(14),
                    selected = item.id == active?.id,
                    onClick = { onSelectOverlay(item) }
                )
            }
        }

        if (active != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditorActionPill(
                    icon = Icons.Rounded.Edit,
                    label = "Edit",
                    selected = false,
                    onClick = { onEditOverlayText(active) }
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(MarkupFontStyle.entries) { font ->
                        EditorChoiceChip(
                            label = font.label,
                            selected = active.fontStyle == font,
                            onClick = { onUpdateOverlay(active.copy(fontStyle = font)) }
                        )
                    }
                }
                IconButton(
                    onClick = { onDeleteOverlay(active.id) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete text overlay",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                items(MARKUP_PALETTE_COLORS) { argb ->
                    val sel = argb == active.colorArgb
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .clickable(role = Role.Button) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateOverlay(active.copy(colorArgb = argb))
                            }
                            .semantics { contentDescription = "Text color" },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (sel) 30.dp else 24.dp)
                                .clip(CircleShape)
                                .background(argb.toComposeColor())
                                .border(
                                    if (sel) 2.5.dp else 1.5.dp,
                                    if (sel) config.accentColor else Color.White.copy(alpha = 0.65f),
                                    CircleShape
                                )
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${DateGroupingUtils.formatDuration(active.startMs)}–${DateGroupingUtils.formatDuration(active.endMs)}",
                    style = MaterialTheme.typography.labelMedium
                        .copy(fontWeight = FontWeight.Bold)
                        .withGlassLegibility(config),
                    color = config.accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                RangeSlider(
                    value = active.startMs.toFloat()..active.endMs.toFloat()
                        .coerceIn(active.startMs.toFloat() + 300f, totalDurationMs.toFloat().coerceAtLeast(500f)),
                    onValueChange = { r ->
                        onUpdateOverlay(active.copy(startMs = r.start.toLong(), endMs = r.endInclusive.toLong()))
                    },
                    valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(500f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = config.accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                )
            }
        }
    }
}

@Composable
fun VideoMusicPanel(
    builtInTracks: List<VideoAudioTrack>,
    selectedMusic: VideoAudioTrack?,
    onSelectMusic: (VideoAudioTrack?) -> Unit,
    onPickDeviceAudio: () -> Unit,
    onPreviewMusicTrim: (Long, Long) -> Unit,
    onCommitChanges: () -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selectedMusic == null) {
            EditorEmptyStateCard(
                icon = Icons.Rounded.MusicNote,
                title = "Add background music",
                subtitle = "Pick a track below or import an audio file from your phone.",
                actionLabel = "Pick from Device",
                onAction = onPickDeviceAudio
            )
        } else {
            GlassPanel(
                shape = RoundedCornerShape(18.dp),
                active = true,
                enableBlur = false,
                elevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = config.accentColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedMusic.title,
                            style = MaterialTheme.typography.titleSmall
                                .copy(fontWeight = FontWeight.Bold)
                                .withGlassLegibility(config),
                            color = config.contentPrimaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Trim: ${DateGroupingUtils.formatDuration(selectedMusic.trimStartMs)} – ${DateGroupingUtils.formatDuration(selectedMusic.trimEndMs)}",
                            style = MaterialTheme.typography.labelSmall.withGlassLegibility(config),
                            color = config.accentColor
                        )
                    }
                    IconButton(
                        onClick = { onSelectMusic(null) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove music track",
                            tint = config.contentSecondaryColor
                        )
                    }
                }
            }

            RangeSlider(
                value = selectedMusic.trimStartMs.toFloat()..selectedMusic.trimEndMs.toFloat()
                    .coerceAtLeast(selectedMusic.trimStartMs.toFloat() + 500f),
                onValueChange = { r -> onPreviewMusicTrim(r.start.toLong(), r.endInclusive.toLong()) },
                onValueChangeFinished = onCommitChanges,
                valueRange = 0f..selectedMusic.durationMs.toFloat().coerceAtLeast(1000f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = config.accentColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                EditorActionPill(
                    icon = Icons.Rounded.AudioFile,
                    label = "Device Audio",
                    selected = selectedMusic?.isBuiltIn == false,
                    onClick = onPickDeviceAudio,
                    modifier = Modifier.testTag("video_pick_audio")
                )
            }
            items(builtInTracks, key = { it.id }) { track ->
                EditorActionPill(
                    icon = Icons.Rounded.MusicNote,
                    label = track.title,
                    selected = selectedMusic?.id == track.id,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMusic(track)
                    }
                )
            }
        }
    }
}

@Composable
fun VideoAudioPanel(
    hasMusicSelected: Boolean,
    originalVolume: Float,
    musicVolume: Float,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onPreviewOriginalVolume: (Float) -> Unit,
    onPreviewMusicVolume: (Float) -> Unit,
    onResetOriginalVolume: () -> Unit,
    onResetMusicVolume: () -> Unit,
    onApplyPreset: (Float, Float) -> Unit,
    onCommitChanges: () -> Unit
) {
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    var activeSliderIndex by remember { mutableStateOf(-1) }

    val trackDrag: (Int, Boolean) -> Unit = { idx, dragging ->
        activeSliderIndex = if (dragging) idx else if (activeSliderIndex == idx) -1 else activeSliderIndex
        onSliderDraggingChange(dragging)
    }

    DisposableEffect(Unit) {
        onDispose { onSliderDraggingChange(false) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(
            visible = !isSliderDragging,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorActionPill(
                    icon = Icons.AutoMirrored.Rounded.VolumeOff,
                    label = "Mute Video",
                    selected = originalVolume <= 0.01f,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onApplyPreset(if (originalVolume <= 0.01f) 1f else 0f, musicVolume)
                    }
                )
                EditorActionPill(
                    icon = Icons.Rounded.GraphicEq,
                    label = "Balanced Mix",
                    selected = abs(originalVolume - 0.5f) < 0.06f && abs(musicVolume - 0.8f) < 0.06f,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onApplyPreset(0.5f, 0.8f)
                    }
                )
            }
        }

        if (!isSliderDragging || activeSliderIndex != 1) {
            EditorSliderControl(
                label = "Video Sound",
                value = originalVolume,
                valueRange = 0f..1f,
                defaultValue = 1f,
                valueText = "${(originalVolume * 100f).roundToInt()}%",
                onValueChange = onPreviewOriginalVolume,
                onValueChangeFinished = onCommitChanges,
                onReset = onResetOriginalVolume,
                onDragStateChanged = { trackDrag(0, it) },
                sliderTag = "audio_original_slider",
                resetTag = "audio_original_reset"
            )
        }

        if (!isSliderDragging || activeSliderIndex != 0) {
            EditorSliderControl(
                label = "Music Volume",
                value = musicVolume,
                valueRange = 0f..1f,
                defaultValue = 0.7f,
                valueText = "${(musicVolume * 100f).roundToInt()}%",
                onValueChange = onPreviewMusicVolume,
                onValueChangeFinished = onCommitChanges,
                onReset = onResetMusicVolume,
                onDragStateChanged = { trackDrag(1, it) },
                helperHint = if (hasMusicSelected) "Double-tap to reset" else "Pick music in Music tab",
                sliderTag = "audio_music_slider",
                resetTag = "audio_music_reset"
            )
        }
    }
}
