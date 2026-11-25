package com.example.ui.editor.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.originAwareEntrance
import com.example.ui.theme.tactilePressMotion
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Semantic color & accent palette for photo/video editors, backed by EditorGlassTokens.
 */
object EditorPalette {
    val CanvasBg = Color(0xFF06080D)
    val LightCanvasBg = Color(0xFF10141E)
    val ToolbarSurface = Color(0xFF151924)
    val CardSurface = Color(0xFF1E2434)
    val ChipInactive = Color(0xFF23293A)
    val BorderSubtle = Color.White.copy(alpha = 0.22f)
    val Accent = EditorGlassTokens.Accent
    val TextPrimary = EditorGlassTokens.TextOnDark
    val TextSecondary = Color(0xFFD5DFEE)
    val TooltipBg = Color(0xFF1C2336)
}

data class EditorTabUi<T>(
    val tab: T,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Floating Top Bar rendered with Liquid Glass pill/circle buttons over the full-bleed photo/video.
 */
@Composable
fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    primaryActionLabel: String,
    primaryActionIcon: ImageVector,
    primaryEnabled: Boolean,
    isBusy: Boolean,
    onCancel: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPrimaryAction: () -> Unit,
    autoEnhanceActive: Boolean? = null,
    onAutoEnhanceClick: (() -> Unit)? = null,
    cancelTag: String = "editor_cancel",
    undoTag: String = "editor_undo",
    redoTag: String = "editor_redo",
    primaryTag: String = "editor_save"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Consolidated leading Cancel + Undo/Redo pill (1 blur surface)
        GlassPanel(
            shape = CircleShape,
            enableBlur = true,
            elevation = 8.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close editor",
                    enableBlur = false,
                    onClick = onCancel,
                    modifier = Modifier.testTag(cancelTag)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                GlassButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo last change",
                    enabled = canUndo,
                    enableBlur = false,
                    onClick = onUndo,
                    modifier = Modifier.testTag(undoTag)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                GlassButton(
                    icon = Icons.AutoMirrored.Rounded.Redo,
                    contentDescription = "Redo change",
                    enabled = canRedo,
                    enableBlur = false,
                    onClick = onRedo,
                    modifier = Modifier.testTag(redoTag)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (onAutoEnhanceClick != null && autoEnhanceActive != null) {
            // Consolidated trailing Auto + Save pill (2nd blur surface)
            GlassPanel(
                shape = CircleShape,
                enableBlur = true,
                strongFill = true,
                elevation = 8.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassButton(
                        icon = Icons.Rounded.AutoFixHigh,
                        label = "Auto",
                        contentDescription = "Auto enhance",
                        active = autoEnhanceActive,
                        enableBlur = false,
                        onClick = onAutoEnhanceClick,
                        modifier = Modifier.testTag("editor_quick_auto")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    GlassButton(
                        icon = primaryActionIcon,
                        label = primaryActionLabel,
                        contentDescription = primaryActionLabel,
                        active = true,
                        enabled = primaryEnabled,
                        isLoading = isBusy,
                        enableBlur = false,
                        strongFill = true,
                        onLongPressHaptic = true,
                        onClick = onPrimaryAction,
                        modifier = Modifier.testTag(primaryTag)
                    )
                }
            }
        } else {
            GlassButton(
                icon = primaryActionIcon,
                label = primaryActionLabel,
                contentDescription = primaryActionLabel,
                active = true,
                enabled = primaryEnabled,
                isLoading = isBusy,
                enableBlur = true,
                strongFill = true,
                onLongPressHaptic = true,
                onClick = onPrimaryAction,
                modifier = Modifier.testTag(primaryTag)
            )
        }
    }
}

/**
 * Floating Liquid Glass bottom pill toolbar containing tool tabs with >= 48dp touch targets
 * and bright accent glow on the active tool.
 */
@Composable
fun <T> EditorBottomTabBar(
    tabs: List<EditorTabUi<T>>,
    selectedTab: T,
    onSelectTab: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        strongFill = true,
        enableBlur = true,
        elevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                val selected = item.tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }

                val tintColor by animateColorAsState(
                    targetValue = if (selected) config.accentColor else config.contentSecondaryColor,
                    animationSpec = motion.uiTween(MotionTokens.MicroMs),
                    label = "tab_tint_${item.testTag}"
                )
                val tabScale by animateFloatAsState(
                    targetValue = if (selected && !motion.prefersReducedMotion) 1.03f else 1f,
                    animationSpec = motion.floatSpring(allowOvershoot = false),
                    label = "tab_scale_${item.testTag}"
                )

                GlassPanel(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 68.dp, minHeight = 50.dp)
                        .graphicsLayer {
                            scaleX = tabScale
                            scaleY = tabScale
                        }
                        .tactilePressMotion(interactionSource, squashAmount = 0.045f, highFrequency = true)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(item.tab)
                        }
                        .testTag(item.testTag),
                    shape = CircleShape,
                    active = selected,
                    enableBlur = false,
                    elevation = if (selected) 8.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                                .copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                .withGlassLegibility(config),
                            color = if (selected) config.contentPrimaryColor else config.contentSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Glass Slider with:
 * - Translucent Glass Orb Thumb that grows and emits an accent glow while dragged
 * - Live value pill, >= 48dp Reset button, double-tap to reset, haptic snap detents
 * - Reports `onDragStateChanged(Boolean)` so parent screens can hide panels while dragging
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultValue: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    onDragStateChanged: (Boolean) -> Unit = {},
    snapThreshold: Float = (valueRange.endInclusive - valueRange.start) * 0.025f,
    helperHint: String? = "Double-tap to reset",
    sliderTag: String = "editor_slider",
    resetTag: String = "editor_slider_reset"
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    val isModified = abs(value - defaultValue) > 0.005f
    var lastVal by remember(label) { mutableFloatStateOf(value) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val activeDrag = isDragged || isPressed

    LaunchedEffect(activeDrag) {
        onDragStateChanged(activeDrag)
    }

    DisposableEffect(Unit) {
        onDispose { onDragStateChanged(false) }
    }

    val thumbScale by animateFloatAsState(
        targetValue = if (activeDrag) 1f else 0.81f,
        animationSpec = if (motion.prefersReducedMotion) tween(60) else MotionTokens.SnappySpring,
        label = "orb_thumb_scale"
    )
    val valuePillPulse = remember { Animatable(1f) }
    val pulseScope = rememberCoroutineScope()

    fun triggerSnapPulse() {
        if (motion.prefersReducedMotion) return
        pulseScope.launch {
            valuePillPulse.snapTo(1.14f)
            valuePillPulse.animateTo(1f, MotionTokens.SnappySpring)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(label, defaultValue) {
                detectTapGestures(
                    onDoubleTap = {
                        if (isModified) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            triggerSnapPulse()
                            onReset()
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall
                    .copy(fontWeight = FontWeight.SemiBold)
                    .withGlassLegibility(config),
                color = config.contentPrimaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val s = valuePillPulse.value
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(
                        if (isModified) config.accentColor.copy(alpha = 0.26f)
                        else Color.White.copy(alpha = 0.14f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isModified) config.accentColor.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.24f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium
                        .copy(fontWeight = FontWeight.Bold)
                        .withGlassLegibility(config),
                    color = if (isModified) config.accentColor else config.contentPrimaryColor,
                    modifier = Modifier.testTag("${sliderTag}_value")
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!helperHint.isNullOrBlank()) {
                Text(
                    text = helperHint,
                    style = MaterialTheme.typography.labelSmall.withGlassLegibility(config),
                    color = config.contentSecondaryColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            GlassButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Reset $label",
                enabled = isModified,
                active = isModified,
                enableBlur = false,
                onLongPressHaptic = true,
                onClick = {
                    triggerSnapPulse()
                    onReset()
                },
                modifier = Modifier.testTag(resetTag)
            )
        }

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { next ->
                val crossedSnap = (lastVal < defaultValue && next >= defaultValue) ||
                    (lastVal > defaultValue && next <= defaultValue)
                val finalNext = if (abs(next - defaultValue) <= snapThreshold &&
                    abs(lastVal - defaultValue) > snapThreshold
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    triggerSnapPulse()
                    defaultValue
                } else {
                    if (crossedSnap) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        triggerSnapPulse()
                    }
                    next
                }
                lastVal = finalNext
                onValueChange(finalNext)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = config.accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            ),
            thumb = {
                // Translucent Glass Orb Thumb scaled on GPU without layout invalidation
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            scaleX = thumbScale
                            scaleY = thumbScale
                        }
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = config.accentColor,
                            spotColor = config.accentColor
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (config.reduceTransparency) {
                                    listOf(Color.White, config.accentColor)
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.92f),
                                        config.accentColor.copy(alpha = if (activeDrag) 0.72f else 0.45f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = if (activeDrag) 2.dp else 1.5.dp,
                            color = if (activeDrag) Color.White else Color.White.copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    drawStopIndicator = null,
                    colors = SliderDefaults.colors(
                        activeTrackColor = config.accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .semantics { contentDescription = "$label slider, current value $valueText" }
                .testTag(sliderTag)
        )
    }
}

/**
 * Floating frosted Glass Tooltip banner shown once per tool and skippable.
 */
@Composable
fun EditorTooltipBanner(
    visible: Boolean,
    title: String,
    message: String,
    onDismissTip: () -> Unit,
    onSkipAllTips: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    AnimatedVisibility(
        visible = visible,
        enter = motion.bottomSheetEnter(),
        exit = motion.bottomSheetExit(),
        modifier = modifier
    ) {
        GlassPanel(
            shape = RoundedCornerShape(22.dp),
            active = true,
            enableBlur = true,
            elevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(config.accentColor.copy(alpha = 0.24f))
                        .border(1.dp, config.accentColor.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = config.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge
                            .copy(fontWeight = FontWeight.Bold)
                            .withGlassLegibility(config),
                        color = config.contentPrimaryColor
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.withGlassLegibility(config),
                        color = config.contentSecondaryColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = onDismissTip,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            "Got it",
                            color = config.accentColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge.withGlassLegibility(config)
                        )
                    }
                    TextButton(
                        onClick = onSkipAllTips,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(
                            "Skip tips",
                            color = config.contentSecondaryColor,
                            style = MaterialTheme.typography.labelMedium.withGlassLegibility(config)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating frosted Glass Toast card shown inside the editor canvas.
 */
@Composable
fun GlassToastBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = motion.originMenuEnter(TransformOrigin(0.5f, 0f)),
        exit = motion.originMenuExit(TransformOrigin(0.5f, 0f)),
        modifier = modifier
    ) {
        GlassPanel(
            shape = CircleShape,
            active = true,
            strongFill = true,
            enableBlur = true,
            elevation = 14.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = config.accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.labelLarge
                        .copy(fontWeight = FontWeight.Bold)
                        .withGlassLegibility(config),
                    color = config.contentPrimaryColor
                )
            }
        }
    }
}

/**
 * Empty state inside a glass panel sheet (nested: blur disabled to preserve 60fps).
 */
@Composable
fun EditorEmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalEditorGlassConfig.current
    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        enableBlur = false,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = config.accentColor,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
                    .copy(fontWeight = FontWeight.Bold)
                    .withGlassLegibility(config),
                color = config.contentPrimaryColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.withGlassLegibility(config),
                color = config.contentSecondaryColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            GlassButton(
                label = actionLabel,
                active = true,
                enableBlur = false,
                onClick = onAction
            )
        }
    }
}

/**
 * Reusable Frosted Glass Dialog shell for all editor modal dialogs.
 */
@Composable
fun GlassDialogShell(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val config = LocalEditorGlassConfig.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        GlassPanel(
            shape = RoundedCornerShape(26.dp),
            strongFill = true,
            enableBlur = true,
            elevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .originAwareEntrance(TransformOrigin(0.5f, 0.5f))
                .glassSheetMotion()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                        .copy(fontWeight = FontWeight.Bold)
                        .withGlassLegibility(config),
                    color = config.contentPrimaryColor
                )
                content()
            }
        }
    }
}

/**
 * Confirmation Glass Dialog shown ONLY when exiting with unsaved changes.
 */
@Composable
fun UnsavedChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscardChanges: () -> Unit
) {
    val config = LocalEditorGlassConfig.current
    GlassDialogShell(
        onDismissRequest = onKeepEditing,
        title = "Discard unsaved edits?"
    ) {
        Text(
            text = "You have edits that haven't been saved yet. Leaving now will lose those changes.",
            style = MaterialTheme.typography.bodyMedium.withGlassLegibility(config),
            color = config.contentSecondaryColor
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onKeepEditing,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    "Keep editing",
                    color = config.accentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge.withGlassLegibility(config)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            TextButton(
                onClick = onDiscardChanges,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("editor_discard_confirm")
            ) {
                Text(
                    "Discard",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge.withGlassLegibility(config)
                )
            }
        }
    }
}
