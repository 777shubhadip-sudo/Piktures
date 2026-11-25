package com.example.ui.editor.photo

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Deblur
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.editor.common.EditorGlassTokens
import com.example.ui.editor.common.EditorPalette
import com.example.ui.editor.common.EditorSliderControl
import com.example.ui.editor.common.GlassButton
import com.example.ui.editor.common.GlassPanel
import com.example.ui.editor.common.LocalEditorGlassConfig
import com.example.ui.editor.common.withGlassLegibility
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.horizontalRubberBand
import com.example.ui.theme.tactilePressMotion
import kotlin.math.roundToInt

@Composable
fun SuggestionsPanel(
    activeSuggestion: PhotoSuggestionPreset,
    thumbnails: Map<PhotoSuggestionPreset, Bitmap>,
    onSelectSuggestion: (PhotoSuggestionPreset) -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val haptics = LocalHapticFeedback.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.horizontalRubberBand()
    ) {
        itemsIndexed(PhotoSuggestionPreset.entries) { idx, preset ->
            val selected = preset == activeSuggestion
            val bmp = thumbnails[preset]
            val interaction = remember { MutableInteractionSource() }
            GlassPanel(
                modifier = Modifier
                    .defaultMinSize(minWidth = 74.dp, minHeight = 48.dp)
                    .tactilePressMotion(interaction, squashAmount = 0.045f)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button
                    ) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectSuggestion(preset)
                    }
                    .testTag("suggestion_${preset.name.lowercase()}"),
                shape = RoundedCornerShape(18.dp),
                active = selected,
                enableBlur = false,
                elevation = if (selected) 10.dp else 0.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 66.dp, height = 56.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            val img = remember(bmp) { bmp.asImageBitmap() }
                            Image(
                                bitmap = img,
                                contentDescription = "${preset.title} preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else if (preset == PhotoSuggestionPreset.AUTO_ENHANCE) {
                            Icon(
                                imageVector = Icons.Rounded.AutoFixHigh,
                                contentDescription = preset.title,
                                tint = config.accentColor
                            )
                        }
                        if (preset == PhotoSuggestionPreset.AUTO_ENHANCE) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(config.accentColor)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "1-Tap",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EditorGlassTokens.DarkOnAccent
                                )
                            }
                        }
                    }
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.labelMedium
                            .copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
                            .withGlassLegibility(config),
                        color = if (selected) config.accentColor else config.contentPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CropPanel(
    crop: PhotoCropState,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onRotate90: () -> Unit,
    onFlip: () -> Unit,
    onSelectAspect: (PhotoAspectRatio) -> Unit,
    onPreviewStraighten: (Float) -> Unit,
    onCommitStraighten: () -> Unit,
    onResetStraighten: () -> Unit
) {
    val motion = LocalMotionConfig.current
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
                EditorActionPill(
                    icon = Icons.AutoMirrored.Rounded.RotateRight,
                    label = "Rotate",
                    selected = crop.rotation90 != 0,
                    onClick = onRotate90,
                    modifier = Modifier.testTag("editor_rotate_90")
                )
                EditorActionPill(
                    icon = Icons.Rounded.Flip,
                    label = "Flip",
                    selected = crop.flipHorizontal,
                    onClick = onFlip,
                    modifier = Modifier.testTag("editor_flip")
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(PhotoAspectRatio.entries) { aspect ->
                        EditorChoiceChip(
                            label = aspect.label,
                            selected = crop.aspectRatio == aspect,
                            onClick = { onSelectAspect(aspect) },
                            modifier = Modifier.testTag("crop_aspect_${aspect.name.lowercase()}")
                        )
                    }
                }
            }
        }

        EditorSliderControl(
            label = "Straighten",
            value = crop.straightenDegrees,
            valueRange = -45f..45f,
            defaultValue = 0f,
            valueText = "${crop.straightenDegrees.roundToInt()}°",
            onValueChange = onPreviewStraighten,
            onValueChangeFinished = onCommitStraighten,
            onReset = onResetStraighten,
            onDragStateChanged = onSliderDraggingChange,
            sliderTag = "editor_straighten_slider",
            resetTag = "editor_straighten_reset"
        )
    }
}

@Composable
fun AdjustPanel(
    adjustments: PhotoAdjustments,
    selectedParam: AdjustParameter,
    thumbnails: Map<AdjustParameter, Bitmap>,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onSelectParam: (AdjustParameter) -> Unit,
    onPreviewValue: (AdjustParameter, Float) -> Unit,
    onCommitValue: () -> Unit,
    onResetParam: (AdjustParameter) -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    val currentVal = adjustments.get(selectedParam)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorSliderControl(
            label = selectedParam.label,
            value = currentVal,
            valueRange = -100f..100f,
            defaultValue = 0f,
            valueText = if (currentVal > 0f) "+${currentVal.roundToInt()}" else "${currentVal.roundToInt()}",
            onValueChange = { onPreviewValue(selectedParam, it) },
            onValueChangeFinished = onCommitValue,
            onReset = { onResetParam(selectedParam) },
            onDragStateChanged = onSliderDraggingChange,
            sliderTag = "adjust_slider",
            resetTag = "adjust_reset_${selectedParam.name.lowercase()}"
        )

        AnimatedVisibility(
            visible = !isSliderDragging,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.horizontalRubberBand()
            ) {
                items(AdjustParameter.entries) { param ->
                    val v = adjustments.get(param).roundToInt()
                    val selected = param == selectedParam
                    val bmp = thumbnails[param]
                    val interaction = remember { MutableInteractionSource() }
                    GlassPanel(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                            .tactilePressMotion(interaction, squashAmount = 0.045f, highFrequency = true)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Button
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectParam(param)
                            }
                            .testTag("adjust_chip_${param.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        active = selected,
                        enableBlur = false,
                        elevation = if (selected) 8.dp else 0.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bmp != null) {
                                    val img = remember(bmp) { bmp.asImageBitmap() }
                                    Image(
                                        bitmap = img,
                                        contentDescription = "${param.label} preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                if (v != 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.72f))
                                            .padding(vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (v > 0) "+$v" else "$v",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = config.accentColor
                                        )
                                    }
                                }
                            }
                            Text(
                                text = param.label,
                                style = MaterialTheme.typography.labelSmall
                                    .copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                    .withGlassLegibility(config),
                                color = if (selected) config.accentColor else config.contentPrimaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersPanel(
    selectedFilter: PhotoFilterPreset,
    intensity: Float,
    thumbnails: Map<PhotoFilterPreset, Bitmap>,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onSelectFilter: (PhotoFilterPreset) -> Unit,
    onPreviewIntensity: (Float) -> Unit,
    onCommitIntensity: () -> Unit,
    onResetIntensity: () -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selectedFilter != PhotoFilterPreset.ORIGINAL) {
            EditorSliderControl(
                label = "${selectedFilter.label} Strength",
                value = intensity,
                valueRange = 0f..1f,
                defaultValue = 1f,
                valueText = "${(intensity * 100f).roundToInt()}%",
                onValueChange = onPreviewIntensity,
                onValueChangeFinished = onCommitIntensity,
                onReset = onResetIntensity,
                onDragStateChanged = onSliderDraggingChange,
                sliderTag = "filter_intensity_slider",
                resetTag = "filter_intensity_reset"
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
                    val isSelected = preset == selectedFilter
                    val bmp = thumbnails[preset]
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
                            .testTag("filter_thumb_${preset.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        active = isSelected,
                        enableBlur = false,
                        elevation = if (isSelected) 10.dp else 0.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
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
                                if (isSelected) {
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
                                    .copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                    .withGlassLegibility(config),
                                color = if (isSelected) config.accentColor else config.contentPrimaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkupPanel(
    activeTool: MarkupTool,
    activeColor: Long,
    widthFraction: Float,
    fontStyle: MarkupFontStyle,
    textSizeSp: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onSelectTool: (MarkupTool) -> Unit,
    onSelectColor: (Long) -> Unit,
    onWidthChange: (Float) -> Unit,
    onSelectFont: (MarkupFontStyle) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onAddTextClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(
            visible = !isSliderDragging,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            EditorActionPill(
                                icon = Icons.Rounded.Draw,
                                label = "Pen",
                                selected = activeTool == MarkupTool.PEN,
                                onClick = { onSelectTool(MarkupTool.PEN) }
                            )
                        }
                        item {
                            EditorActionPill(
                                icon = Icons.Rounded.Highlight,
                                label = "Highlight",
                                selected = activeTool == MarkupTool.HIGHLIGHTER,
                                onClick = { onSelectTool(MarkupTool.HIGHLIGHTER) }
                            )
                        }
                        item {
                            EditorActionPill(
                                icon = Icons.Rounded.TextFields,
                                label = "Text",
                                selected = activeTool == MarkupTool.TEXT,
                                onClick = {
                                    onSelectTool(MarkupTool.TEXT)
                                    onAddTextClick()
                                }
                            )
                        }
                        item {
                            EditorActionPill(
                                icon = Icons.Rounded.CleaningServices,
                                label = "Eraser",
                                selected = activeTool == MarkupTool.ERASER,
                                onClick = { onSelectTool(MarkupTool.ERASER) }
                            )
                        }
                    }

                    GlassButton(
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = "Undo markup",
                        enabled = canUndo,
                        enableBlur = false,
                        onClick = onUndo
                    )
                    GlassButton(
                        icon = Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = "Redo markup",
                        enabled = canRedo,
                        enableBlur = false,
                        onClick = onRedo
                    )
                }

                if (activeTool != MarkupTool.ERASER) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(MARKUP_PALETTE_COLORS) { argb ->
                            val selected = argb == activeColor
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectColor(argb)
                                    }
                                    .semantics { contentDescription = "Select color" },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 32.dp else 26.dp)
                                        .clip(CircleShape)
                                        .background(argb.toComposeColor())
                                        .border(
                                            width = if (selected) 2.5.dp else 1.5.dp,
                                            color = if (selected) config.accentColor else Color.White.copy(alpha = 0.65f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                if (activeTool == MarkupTool.TEXT) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MarkupFontStyle.entries) { f ->
                            EditorChoiceChip(
                                label = f.label,
                                selected = f == fontStyle,
                                onClick = { onSelectFont(f) }
                            )
                        }
                    }
                }
            }
        }

        if (activeTool == MarkupTool.TEXT) {
            EditorSliderControl(
                label = "Text Size",
                value = textSizeSp,
                valueRange = 16f..48f,
                defaultValue = 28f,
                valueText = "${textSizeSp.roundToInt()}sp",
                onValueChange = onTextSizeChange,
                onValueChangeFinished = {},
                onReset = { onTextSizeChange(28f) },
                onDragStateChanged = onSliderDraggingChange,
                helperHint = null,
                sliderTag = "markup_text_size_slider",
                resetTag = "markup_text_size_reset"
            )
        } else {
            EditorSliderControl(
                label = if (activeTool == MarkupTool.ERASER) "Eraser Size" else "Brush Size",
                value = widthFraction,
                valueRange = 0.005f..0.04f,
                defaultValue = 0.012f,
                valueText = "${(widthFraction * 1000f).roundToInt()}pt",
                onValueChange = onWidthChange,
                onValueChangeFinished = {},
                onReset = { onWidthChange(0.012f) },
                onDragStateChanged = onSliderDraggingChange,
                helperHint = null,
                sliderTag = "markup_brush_size_slider",
                resetTag = "markup_brush_size_reset"
            )
        }
    }
}

@Composable
fun ToolsPanel(
    tools: PhotoToolsState,
    magicEraserMode: Boolean,
    eraserRadiusFraction: Float,
    isSliderDragging: Boolean,
    onSliderDraggingChange: (Boolean) -> Unit,
    onToggleAutoEnhance: () -> Unit,
    onPreviewBlur: (Float) -> Unit,
    onCommitBlur: () -> Unit,
    onResetBlur: () -> Unit,
    onPreviewDenoise: (Float) -> Unit,
    onCommitDenoise: () -> Unit,
    onResetDenoise: () -> Unit,
    onToggleMagicEraser: () -> Unit,
    onEraserRadiusChange: (Float) -> Unit,
    onClearMagicEraser: () -> Unit
) {
    val motion = LocalMotionConfig.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(
            visible = !isSliderDragging,
            enter = if (motion.prefersReducedMotion) motion.fadeEnter() else motion.fadeEnter() + expandVertically(motion.uiTween()),
            exit = if (motion.prefersReducedMotion) motion.fadeExit() else motion.fadeExit() + shrinkVertically(motion.exitTween())
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.horizontalRubberBand()
            ) {
                item {
                    ToolActionTile(
                        icon = Icons.Rounded.AutoFixHigh,
                        label = "Auto Enhance",
                        subtitle = "One-tap balance",
                        active = tools.autoEnhanced,
                        onClick = onToggleAutoEnhance,
                        modifier = Modifier.testTag("tool_auto_enhance")
                    )
                }
                item {
                    ToolActionTile(
                        icon = Icons.Rounded.BlurOn,
                        label = "Blur Background",
                        subtitle = "Portrait depth",
                        active = tools.backgroundBlurAmount > 0.02f,
                        onClick = {
                            onPreviewBlur(if (tools.backgroundBlurAmount > 0.02f) 0f else 0.55f)
                            onCommitBlur()
                        },
                        modifier = Modifier.testTag("tool_blur_bg")
                    )
                }
                item {
                    ToolActionTile(
                        icon = Icons.Rounded.Brush,
                        label = "Magic Eraser",
                        subtitle = "Brush to remove",
                        active = magicEraserMode || tools.eraserStrokes.isNotEmpty(),
                        onClick = onToggleMagicEraser,
                        modifier = Modifier.testTag("tool_magic_eraser")
                    )
                }
                item {
                    ToolActionTile(
                        icon = Icons.Rounded.Deblur,
                        label = "Unblur & Clean",
                        subtitle = "Fix motion & grain",
                        active = tools.denoiseUnblurStrength > 0.02f,
                        onClick = {
                            onPreviewDenoise(if (tools.denoiseUnblurStrength > 0.02f) 0f else 0.60f)
                            onCommitDenoise()
                        },
                        modifier = Modifier.testTag("tool_unblur_denoise")
                    )
                }
            }
        }

        if (magicEraserMode) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EditorSliderControl(
                    label = "Eraser Brush Size",
                    value = eraserRadiusFraction,
                    valueRange = 0.02f..0.09f,
                    defaultValue = 0.045f,
                    valueText = "${(eraserRadiusFraction * 1000f).roundToInt()}",
                    onValueChange = onEraserRadiusChange,
                    onValueChangeFinished = {},
                    onReset = onClearMagicEraser,
                    onDragStateChanged = onSliderDraggingChange,
                    helperHint = if (tools.eraserStrokes.isNotEmpty()) "Tap reset to clear" else "Brush over object",
                    sliderTag = "magic_eraser_size_slider",
                    resetTag = "magic_eraser_clear"
                )
                if (!isSliderDragging && tools.eraserStrokes.isNotEmpty()) {
                    EditorChoiceChip(
                        label = "Clear erased spots",
                        selected = false,
                        onClick = onClearMagicEraser
                    )
                }
            }
        } else if (tools.backgroundBlurAmount > 0.02f) {
            EditorSliderControl(
                label = "Background Blur",
                value = tools.backgroundBlurAmount,
                valueRange = 0f..1f,
                defaultValue = 0f,
                valueText = "${(tools.backgroundBlurAmount * 100f).roundToInt()}%",
                onValueChange = onPreviewBlur,
                onValueChangeFinished = onCommitBlur,
                onReset = onResetBlur,
                onDragStateChanged = onSliderDraggingChange,
                sliderTag = "tool_blur_slider",
                resetTag = "tool_blur_reset"
            )
        } else if (tools.denoiseUnblurStrength > 0.02f) {
            EditorSliderControl(
                label = "Unblur & Clean",
                value = tools.denoiseUnblurStrength,
                valueRange = 0f..1f,
                defaultValue = 0f,
                valueText = "${(tools.denoiseUnblurStrength * 100f).roundToInt()}%",
                onValueChange = onPreviewDenoise,
                onValueChangeFinished = onCommitDenoise,
                onReset = onResetDenoise,
                onDragStateChanged = onSliderDraggingChange,
                sliderTag = "tool_denoise_slider",
                resetTag = "tool_denoise_reset"
            )
        }
    }
}

@Composable
internal fun EditorActionPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassButton(
        icon = icon,
        label = label,
        active = selected,
        enableBlur = false,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun EditorChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassButton(
        label = label,
        active = selected,
        enableBlur = false,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun ToolActionTile(
    icon: ImageVector,
    label: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalEditorGlassConfig.current
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    GlassPanel(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .tactilePressMotion(interaction, squashAmount = 0.045f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        active = active,
        enableBlur = false,
        elevation = if (active) 10.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) config.accentColor else config.contentPrimaryColor,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                        .copy(fontWeight = FontWeight.Bold)
                        .withGlassLegibility(config),
                    color = config.contentPrimaryColor,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.withGlassLegibility(config),
                    color = if (active) config.accentColor else config.contentSecondaryColor,
                    maxLines = 1
                )
            }
        }
    }
}
