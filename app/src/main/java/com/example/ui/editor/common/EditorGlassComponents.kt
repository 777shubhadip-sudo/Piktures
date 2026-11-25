package com.example.ui.editor.common

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.tactilePressMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

object EditorGlassTokens {
    val Accent = Color(0xFFC48CB3)
    val AccentGlow = Color(0xFF83A6CE)
    val Gold = Color(0xFFE5C9D7)
    val DarkSolidSurface = Color(0xFF0B1B32)
    val DarkSolidElevated = Color(0xFF26415E)
    val LightSolidSurface = Color(0xFFF3EEF4)
    val LightSolidElevated = Color(0xFFFFFFFF)
    val TextOnDark = Color(0xFFFFFFFF)
    val TextOnLight = Color(0xFF0D1E4C)
    val DarkOnAccent = Color(0xFF0D1E4C)
}

@Immutable
data class EditorGlassConfig(
    val isDarkGlass: Boolean = true,
    val supportsNativeBlur: Boolean = false,
    val reduceTransparency: Boolean = false,
    val backdropLuminance: Float = 0.25f,
    val accentColor: Color = EditorGlassTokens.Accent,
    val hazeState: HazeState? = null
) {
    /**
     * When backdrop behind the floating glass is very bright, smoothly deepen the dark tint
     * so white text/icons preserve >= 4.5:1 contrast.
     */
    val adaptiveDarkScrimAlpha: Float
        get() {
            val extra = ((backdropLuminance - 0.38f).coerceAtLeast(0f) / 0.62f) * 0.34f
            return (0.36f + extra).coerceIn(0.36f, 0.72f)
        }

    val contentPrimaryColor: Color
        get() = when {
            reduceTransparency && !isDarkGlass -> EditorGlassTokens.TextOnLight
            !isDarkGlass && backdropLuminance > 0.65f -> EditorGlassTokens.TextOnLight
            else -> EditorGlassTokens.TextOnDark
        }

    val contentSecondaryColor: Color
        get() = contentPrimaryColor.copy(alpha = 0.80f)

    val textShadow: Shadow?
        get() = if (contentPrimaryColor == EditorGlassTokens.TextOnDark && !reduceTransparency) {
            Shadow(
                color = Color.Black.copy(alpha = 0.60f),
                offset = Offset(0f, 1.5f),
                blurRadius = 5f
            )
        } else null
}

val LocalEditorGlassConfig = compositionLocalOf { EditorGlassConfig() }

private fun readSystemReduceTransparency(context: Context): Boolean {
    val sysReduce = runCatching {
        Settings.Secure.getInt(context.contentResolver, "reduce_transparency", 0) != 0
    }.getOrDefault(false)
    val animOff = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) <= 0f
    }.getOrDefault(false)
    return sysReduce || animOff
}

@Composable
fun rememberEditorGlassConfig(
    themeMode: ThemeMode,
    reduceTransparencyPref: Boolean,
    backdropLuminance: Float,
    hazeState: HazeState?
): EditorGlassConfig {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val systemDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    var systemReduceTransparency by remember(context, isInspection) {
        mutableStateOf(if (isInspection) false else readSystemReduceTransparency(context))
    }

    if (!isInspection) {
        DisposableEffect(context, lifecycleOwner) {
            val refresh = { systemReduceTransparency = readSystemReduceTransparency(context) }
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refresh()
                }
            }
            val lifecycleObserver = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refresh()
            }
            runCatching {
                context.contentResolver.registerContentObserver(
                    Settings.Secure.getUriFor("reduce_transparency"),
                    false,
                    observer
                )
                context.contentResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                    false,
                    observer
                )
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            onDispose {
                runCatching { context.contentResolver.unregisterContentObserver(observer) }
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            }
        }
    }

    val reduceTransparency = reduceTransparencyPref || systemReduceTransparency
    val supportsNativeBlur = !isInspection && !reduceTransparency &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null
    val themeAccent = androidx.compose.material3.MaterialTheme.colorScheme.primary

    return remember(isDark, supportsNativeBlur, reduceTransparency, backdropLuminance, hazeState, themeAccent) {
        EditorGlassConfig(
            isDarkGlass = isDark,
            supportsNativeBlur = supportsNativeBlur,
            reduceTransparency = reduceTransparency,
            backdropLuminance = backdropLuminance.coerceIn(0f, 1f),
            accentColor = themeAccent,
            hazeState = hazeState
        )
    }
}

fun TextStyle.withGlassLegibility(config: EditorGlassConfig): TextStyle =
    if (config.textShadow != null) copy(shadow = config.textShadow) else this

/**
 * Spring entrance modifier for floating glass sheets (MotionTokens.StandardMs fade + FluidSpring slide up).
 */
@Composable
fun Modifier.glassSheetMotion(enabled: Boolean = true): Modifier {
    if (!enabled || LocalInspectionMode.current) return this
    val motion = LocalMotionConfig.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.uiTween(MotionTokens.StandardMs),
        label = "glass_sheet_alpha"
    )
    val offsetYPx by animateFloatAsState(
        targetValue = if (visible || motion.prefersReducedMotion) 0f else with(LocalDensity.current) { 18.dp.toPx() },
        animationSpec = motion.fluidFloatSpring(),
        label = "glass_sheet_offset"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        if (!motion.prefersReducedMotion) {
            this.translationY = offsetYPx
        }
    }
}

/**
 * Reusable Liquid Glass Panel:
 * - Native RenderEffect blur (24.dp) on Android 12+ via Haze, translucent fallback on older APIs,
 *   and solid high-contrast surface fallback when "Reduce transparency" is enabled.
 * - White translucent fill (10-18%), adaptive dark contrast tint on bright backgrounds,
 *   1px light border (white 24%), top-edge highlight + bottom inner depth shadow, and soft outer shadow.
 * - Limit active GPU blur layers by setting `enableBlur = false` on nested controls inside a sheet.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    active: Boolean = false,
    strongFill: Boolean = false,
    enableBlur: Boolean = true,
    blurRadius: Dp = 24.dp,
    elevation: Dp = 14.dp,
    backdropLuminanceOverride: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val config = LocalEditorGlassConfig.current
    val effectiveLuminance = backdropLuminanceOverride ?: config.backdropLuminance
    val effectiveConfig = remember(config, effectiveLuminance) {
        if (backdropLuminanceOverride != null) config.copy(backdropLuminance = effectiveLuminance) else config
    }

    val useRealBlur = enableBlur && effectiveConfig.supportsNativeBlur && effectiveConfig.hazeState != null
    val reduceTrans = effectiveConfig.reduceTransparency

    val whiteFillAlpha = when {
        reduceTrans -> 0f
        active -> 0.18f
        strongFill -> 0.15f
        else -> 0.11f
    }

    val motion = LocalMotionConfig.current
    val borderColor by animateColorAsState(
        targetValue = when {
            active -> effectiveConfig.accentColor.copy(alpha = 0.72f)
            reduceTrans && !effectiveConfig.isDarkGlass -> Color.Black.copy(alpha = 0.14f)
            reduceTrans -> Color.White.copy(alpha = 0.18f)
            else -> Color.White.copy(alpha = 0.26f)
        },
        animationSpec = motion.uiTween(200),
        label = "glass_panel_border"
    )

    val hazeStyle = remember(effectiveConfig, useRealBlur, whiteFillAlpha, active, strongFill, blurRadius) {
        val baseTint = when {
            !effectiveConfig.isDarkGlass && effectiveConfig.backdropLuminance <= 0.65f ->
                Color(0xFF0E1526).copy(alpha = effectiveConfig.adaptiveDarkScrimAlpha * 0.85f)
            !effectiveConfig.isDarkGlass ->
                Color.White.copy(alpha = if (strongFill) 0.74f else 0.60f)
            else ->
                Color(0xFF090D16).copy(
                    alpha = if (strongFill) {
                        (effectiveConfig.adaptiveDarkScrimAlpha + 0.12f).coerceAtMost(0.78f)
                    } else {
                        effectiveConfig.adaptiveDarkScrimAlpha
                    }
                )
        }
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = blurRadius,
            noiseFactor = 0f,
            tints = buildList {
                add(HazeTint(baseTint))
                add(HazeTint(Color.White.copy(alpha = whiteFillAlpha), blendMode = BlendMode.Plus))
                if (active) {
                    add(HazeTint(effectiveConfig.accentColor.copy(alpha = 0.18f), blendMode = BlendMode.Plus))
                }
            },
            fallbackTint = HazeTint(
                if (effectiveConfig.isDarkGlass) EditorGlassTokens.DarkSolidSurface else EditorGlassTokens.LightSolidSurface
            )
        )
    }

    val fallbackBg = remember(effectiveConfig, reduceTrans, active, strongFill, whiteFillAlpha) {
        when {
            reduceTrans && effectiveConfig.isDarkGlass ->
                if (active) EditorGlassTokens.DarkSolidElevated else EditorGlassTokens.DarkSolidSurface
            reduceTrans ->
                if (active) EditorGlassTokens.LightSolidElevated else EditorGlassTokens.LightSolidSurface
            !effectiveConfig.isDarkGlass && effectiveConfig.backdropLuminance > 0.65f ->
                Color.White.copy(alpha = if (active) 0.88f else 0.76f)
            else -> {
                val base = (effectiveConfig.adaptiveDarkScrimAlpha + if (strongFill) 0.26f else 0.16f)
                    .coerceIn(0.48f, 0.86f)
                Color(0xFF0D1320).copy(alpha = base)
            }
        }
    }

    val sheenBrush = remember(reduceTrans, whiteFillAlpha, active, effectiveConfig.accentColor) {
        if (reduceTrans) {
            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    if (active) effectiveConfig.accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = whiteFillAlpha),
                    Color.White.copy(alpha = whiteFillAlpha * 0.35f)
                )
            )
        }
    }

    val shadowColor = if (active) effectiveConfig.accentColor.copy(alpha = 0.48f) else Color.Black.copy(alpha = 0.36f)

    val prismBorderBrush = remember(active, reduceTrans, effectiveConfig.isDarkGlass, borderColor) {
        when {
            active -> Brush.linearGradient(
                listOf(
                    effectiveConfig.accentColor.copy(alpha = 0.85f),
                    EditorGlassTokens.AccentGlow.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.48f)
                )
            )
            !reduceTrans && effectiveConfig.isDarkGlass -> Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.36f),
                    effectiveConfig.accentColor.copy(alpha = 0.24f),
                    EditorGlassTokens.AccentGlow.copy(alpha = 0.20f)
                )
            )
            else -> Brush.linearGradient(listOf(borderColor, borderColor))
        }
    }

    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                } else Modifier
            )
            .clip(shape)
            .then(
                if (useRealBlur && effectiveConfig.hazeState != null) {
                    Modifier.hazeEffect(state = effectiveConfig.hazeState, style = hazeStyle) {
                        blurEnabled = true
                    }
                } else {
                    Modifier.background(fallbackBg)
                }
            )
            .background(sheenBrush)
            .drawWithContent {
                drawContent()
                if (!reduceTrans) {
                    val strokePx = 1.dp.toPx()
                    // Soft top-edge highlight
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f),
                        start = Offset(0f, strokePx * 0.5f),
                        end = Offset(size.width, strokePx * 0.5f),
                        strokeWidth = strokePx
                    )
                    // Subtle bottom inner depth shadow
                    drawLine(
                        color = Color.Black.copy(alpha = 0.26f),
                        start = Offset(0f, size.height - strokePx * 0.5f),
                        end = Offset(size.width, size.height - strokePx * 0.5f),
                        strokeWidth = strokePx
                    )
                }
            }
            .border(width = 1.dp, brush = prismBorderBrush, shape = shape),
        content = content
    )
}

/**
 * Reusable Liquid Glass Pill / Circle Button:
 * - Minimum 48dp accessible touch target
 * - Smooth spring press-scale animation (1f -> 0.93f) + haptic feedback
 * - Optional active accent glow halo & brighter glass fill
 * - Supports icon-only, label-only, or icon + label pill layouts
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    contentDescription: String? = label,
    active: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    enableBlur: Boolean = false,
    strongFill: Boolean = false,
    shape: Shape = CircleShape,
    onLongPressHaptic: Boolean = false
) {
    val config = LocalEditorGlassConfig.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> config.contentPrimaryColor.copy(alpha = 0.36f)
            active -> config.accentColor
            else -> config.contentPrimaryColor
        },
        animationSpec = motion.uiTween(MotionTokens.MicroMs),
        label = "glass_btn_tint"
    )

    val minWidth = if (label.isNullOrBlank()) 48.dp else 56.dp
    val baseElevation = if (active) 12.dp else if (enableBlur) 8.dp else 2.dp
    val pressElevation = if (isPressed && enabled) 1.dp else baseElevation

    GlassPanel(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minWidth = minWidth, minHeight = 48.dp)
            .tactilePressMotion(interactionSource, enabled = enabled, squashAmount = 0.055f)
            .then(
                if (!contentDescription.isNullOrBlank()) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            ),
        shape = shape,
        active = active && enabled,
        strongFill = strongFill,
        enableBlur = enableBlur,
        elevation = pressElevation
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = minWidth, minHeight = 48.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled && !isLoading,
                    role = Role.Button
                ) {
                    haptics.performHapticFeedback(
                        if (onLongPressHaptic) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                    )
                    onClick()
                }
                .padding(horizontal = if (label.isNullOrBlank()) 12.dp else 15.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = config.accentColor
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = if (label.isNullOrBlank()) contentDescription else null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (!label.isNullOrBlank()) {
                        if (icon != null) Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge
                                .copy(fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold)
                                .withGlassLegibility(config),
                            color = tint,
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
 * Computes average relative luminance (0f..1f) of the lower 35% + upper 18% of a bitmap
 * on a small 32x32 sample so glass panels auto-darken over bright scenes (>= 4.5:1 contrast).
 */
fun sampleBitmapLuminance(bitmap: android.graphics.Bitmap?): Float {
    if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return 0.25f
    val small = android.graphics.Bitmap.createScaledBitmap(bitmap, 24, 24, false)
    return try {
        var sum = 0.0
        var count = 0
        for (y in 0 until 24) {
            if (y > 4 && y < 15) continue
            for (x in 0 until 24) {
                val px = small.getPixel(x, y)
                val r = ((px shr 16) and 0xFF) / 255.0
                val g = ((px shr 8) and 0xFF) / 255.0
                val b = (px and 0xFF) / 255.0
                sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                count++
            }
        }
        if (count == 0) 0.25f else (sum / count).toFloat().coerceIn(0f, 1f)
    } finally {
        if (small !== bitmap) small.recycle()
    }
}

