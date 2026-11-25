package com.example.ui.components

import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.GalleryTab
import com.example.domain.model.ThemeMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.LuminaTheme
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.ObsidianInk
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.VaultAtmosphere
import com.example.ui.theme.originAwareEntrance
import com.example.ui.theme.tactilePressMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

val LocalHazeState = compositionLocalOf<HazeState?> { null }
val LocalSupportsLiquidBlur = compositionLocalOf<Boolean?> { null }
val LocalReduceTransparency = compositionLocalOf { false }

private const val LIQUID_GLASS_OPTICAL_AGSL = """
    uniform float2 u_resolution;
    uniform float u_cornerRadius;
    uniform float u_fresnelIntensity;
    uniform float u_dispersion;
    uniform float u_glareIntensity;
    uniform float4 u_coolTint;
    uniform float4 u_warmTint;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + float2(r);
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    half4 main(float2 fragCoord) {
        float2 halfSize = max(u_resolution * 0.5, float2(1.0));
        float2 p = fragCoord - halfSize;
        float r = clamp(u_cornerRadius, 1.0, min(halfSize.x, halfSize.y));
        float dist = sdRoundedBox(p, halfSize, r);
        if (dist > 0.5) {
            return half4(0.0);
        }

        float edgeDepth = clamp(-dist, 0.0, 32.0);
        float fresnel = pow(clamp(1.0 - edgeDepth / 22.0, 0.0, 1.0), 2.3) * u_fresnelIntensity;

        float eps = 1.0;
        float dx = sdRoundedBox(p + float2(eps, 0.0), halfSize, r) - dist;
        float dy = sdRoundedBox(p + float2(0.0, eps), halfSize, r) - dist;
        float2 grad = normalize(float2(dx, dy) + float2(0.0001));

        float2 lightDir = normalize(float2(-0.68, -0.73));
        float dirCatch = clamp(dot(-grad, lightDir) * 0.5 + 0.5, 0.0, 1.0);
        float glare = pow(dirCatch, 2.0) * fresnel * u_glareIntensity;

        float2 uv = fragCoord / max(u_resolution, float2(1.0));
        float diagSheen = pow(clamp(1.0 - (uv.x * 0.48 + uv.y * 0.72), 0.0, 1.0), 2.2) * 0.11 * u_glareIntensity;
        float caustic = exp(-pow((uv.x * 0.65 - uv.y * 0.55 + 0.12) * 3.4, 2.0)) * 0.055 * u_glareIntensity;

        float prismBand = smoothstep(10.0, 0.0, edgeDepth) * u_dispersion;
        float coolWeight = clamp(dot(-grad, float2(-0.70, -0.71)) * 0.5 + 0.5, 0.0, 1.0);
        float warmWeight = 1.0 - coolWeight;
        float3 prismRgb = u_coolTint.rgb * (coolWeight * u_coolTint.a) + u_warmTint.rgb * (warmWeight * u_warmTint.a);

        float specAlpha = clamp(glare * 0.48 + fresnel * 0.20 + diagSheen + caustic, 0.0, 0.58);
        float3 rgb = float3(1.0) * specAlpha + prismRgb * prismBand;
        float outAlpha = clamp(specAlpha + prismBand * 0.54, 0.0, 0.70);
        return half4(rgb, outAlpha);
    }
"""

private fun readSystemReduceTransparency(context: Context): Boolean {
    val animScale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)
    val reduceContrastOrTransparency = runCatching {
        Settings.Secure.getInt(
            context.contentResolver,
            "reduce_transparency",
            0
        ) != 0
    }.getOrDefault(false)
    return animScale <= 0f || reduceContrastOrTransparency
}

@Composable
fun rememberSystemReduceTransparency(): Boolean {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    if (isInspection) return false

    var sysReduce by remember(context) { mutableStateOf(readSystemReduceTransparency(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context, lifecycleOwner) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                sysReduce = readSystemReduceTransparency(context)
            }
        }
        runCatching {
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer
            )
            resolver.registerContentObserver(
                Settings.Secure.getUriFor("reduce_transparency"),
                false,
                observer
            )
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sysReduce = readSystemReduceTransparency(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            runCatching { resolver.unregisterContentObserver(observer) }
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return sysReduce
}

@Composable
fun rememberSupportsLiquidBlur(): Boolean {
    if (LocalReduceTransparency.current) return false
    LocalSupportsLiquidBlur.current?.let { return it }
    val isInspection = LocalInspectionMode.current
    if (isInspection || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return !rememberSystemReduceTransparency()
}

@Composable
fun Modifier.liquidGlassSettle(enabled: Boolean = true): Modifier {
    if (!enabled || LocalInspectionMode.current) return this
    val motion = LocalMotionConfig.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.uiTween(MotionTokens.StandardMs),
        label = "liquid_settle_alpha"
    )
    val startY = if (motion.prefersReducedMotion) 0f else with(LocalDensity.current) { 8.dp.toPx() }
    val offsetYPx by animateFloatAsState(
        targetValue = if (visible) 0f else startY,
        animationSpec = motion.uiTween(MotionTokens.StandardMs),
        label = "liquid_settle_y"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetYPx
    }
}

@Composable
fun MeshGradientBackground(
    modifier: Modifier = Modifier,
    useDotPattern: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLuminaColors.current
    val hazeState = LocalHazeState.current

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) {
                        Modifier.hazeSource(state = hazeState, zIndex = 0f)
                    } else {
                        Modifier
                    }
                )
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val maxDim = maxOf(w, h)

                    val orb1Center = Offset(w * 0.18f, h * 0.14f)
                    val orb1Radius = maxDim * 0.72f
                    val orb1Brush = Brush.radialGradient(
                        colors = listOf(glass.meshOrb1, Color.Transparent),
                        center = orb1Center,
                        radius = orb1Radius
                    )

                    val orb2Center = Offset(w * 0.84f, h * 0.44f)
                    val orb2Radius = maxDim * 0.58f
                    val orb2Brush = Brush.radialGradient(
                        colors = listOf(glass.meshOrb2, Color.Transparent),
                        center = orb2Center,
                        radius = orb2Radius
                    )

                    val orb3Center = Offset(w * 0.24f, h * 0.86f)
                    val orb3Radius = maxDim * 0.64f
                    val orb3Brush = Brush.radialGradient(
                        colors = listOf(glass.meshOrb3, Color.Transparent),
                        center = orb3Center,
                        radius = orb3Radius
                    )

                    val orb4Center = Offset(w * 0.86f, h * 0.15f)
                    val orb4Radius = maxDim * 0.50f
                    val orb4Brush = if (glass.meshOrb4.alpha > 0f) {
                        Brush.radialGradient(
                            colors = listOf(glass.meshOrb4, Color.Transparent),
                            center = orb4Center,
                            radius = orb4Radius
                        )
                    } else null

                    val orb5Center = Offset(w * 0.78f, h * 0.85f)
                    val orb5Radius = maxDim * 0.52f
                    val orb5Brush = if (glass.meshOrb5.alpha > 0f) {
                        Brush.radialGradient(
                            colors = listOf(glass.meshOrb5, Color.Transparent),
                            center = orb5Center,
                            radius = orb5Radius
                        )
                    } else null

                    val dotStepPx = 24.dp.toPx()
                    val dotRadiusPx = 1.dp.toPx()

                    onDrawBehind {
                        drawRect(color = glass.meshBase)
                        drawCircle(brush = orb1Brush, radius = orb1Radius, center = orb1Center)
                        drawCircle(brush = orb2Brush, radius = orb2Radius, center = orb2Center)
                        drawCircle(brush = orb3Brush, radius = orb3Radius, center = orb3Center)
                        if (orb4Brush != null) {
                            drawCircle(brush = orb4Brush, radius = orb4Radius, center = orb4Center)
                        }
                        if (orb5Brush != null) {
                            drawCircle(brush = orb5Brush, radius = orb5Radius, center = orb5Center)
                        }
                        if (useDotPattern) {
                            var y = dotStepPx * 0.5f
                            while (y < size.height) {
                                var x = dotStepPx * 0.5f
                                while (x < size.width) {
                                    drawCircle(color = glass.patternOverlay, radius = dotRadiusPx, center = Offset(x, y))
                                    x += dotStepPx
                                }
                                y += dotStepPx
                            }
                        }
                    }
                }
        )
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun buildOpticalShaderOrNull(): RuntimeShader? = runCatching {
    RuntimeShader(LIQUID_GLASS_OPTICAL_AGSL)
}.getOrNull()

/**
 * Physical Liquid Glass surface implementing:
 * - Multi-pass Haze backdrop blur on Android 12+ (with solid Reduce Transparency fallback)
 * - AGSL Fresnel rim intensity, directional glare & chromatic dispersion on Android 13+
 * - Continuous rounded geometry, specular 1dp prism border, and soft diffused elevation shadow.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    strongFill: Boolean = false,
    elevation: Dp = 12.dp,
    enableBackdropBlur: Boolean = true,
    cornerRadiusHint: Dp = if (shape == CircleShape) 100.dp else 24.dp,
    hazeState: HazeState? = LocalHazeState.current,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLuminaColors.current
    val reduceTransparency = LocalReduceTransparency.current
    val supportsBlur = rememberSupportsLiquidBlur()
    val realBlurEnabled = !reduceTransparency && enableBackdropBlur && hazeState != null && supportsBlur
    val isInspection = LocalInspectionMode.current

    if (reduceTransparency) {
        val solidBg = if (strongFill) glass.solidFallbackElevated else glass.solidFallbackSurface
        val solidBorder = if (glass.isDark) Color.White.copy(alpha = 0.22f) else Color(0xFFCBD5E1)
        Box(
            modifier = modifier
                .then(
                    if (elevation > 0.dp) {
                        Modifier.shadow(
                            elevation = (elevation * 0.65f).coerceAtLeast(2.dp),
                            shape = shape,
                            ambientColor = Color.Black.copy(alpha = 0.14f),
                            spotColor = Color.Black.copy(alpha = 0.18f)
                        )
                    } else Modifier
                )
                .clip(shape)
                .background(solidBg)
                .border(borderWidth, solidBorder, shape),
            content = content
        )
        return
    }

    val blurTintAlpha = if (glass.isDark) {
        if (strongFill) 0.44f else 0.30f
    } else {
        if (strongFill) 0.60f else 0.44f
    }

    val blurRadius = if (strongFill) 22.dp else 18.dp

    val sheenBrush = remember(glass.isDark, strongFill, realBlurEnabled) {
        val colors = if (glass.isDark) {
            val topA = if (realBlurEnabled) (if (strongFill) 0.16f else 0.12f) else (if (strongFill) 0.22f else 0.15f)
            val botA = if (realBlurEnabled) (if (strongFill) 0.06f else 0.03f) else (if (strongFill) 0.10f else 0.06f)
            listOf(Color.White.copy(alpha = topA), Color.White.copy(alpha = botA))
        } else {
            val topA = if (realBlurEnabled) (if (strongFill) 0.56f else 0.40f) else (if (strongFill) 0.72f else 0.54f)
            val botA = if (realBlurEnabled) (if (strongFill) 0.24f else 0.14f) else (if (strongFill) 0.44f else 0.28f)
            listOf(Color.White.copy(alpha = topA), Color.White.copy(alpha = botA))
        }
        Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val hazeStyle = remember(glass, strongFill, blurTintAlpha, blurRadius) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = blurRadius,
            noiseFactor = 0f,
            tints = listOf(
                HazeTint(color = glass.glassFillBottom.copy(alpha = blurTintAlpha)),
                HazeTint(
                    color = glass.accentGlow.copy(alpha = if (glass.isDark) 0.06f else 0.025f),
                    blendMode = BlendMode.Plus
                )
            ),
            fallbackTint = HazeTint(color = glass.glassStrongFill)
        )
    }

    val prismBorderBrush = remember(glass.prismBorderStart, glass.prismBorderMid, glass.prismBorderEnd) {
        Brush.linearGradient(
            colors = listOf(glass.prismBorderStart, glass.prismBorderMid, glass.prismBorderEnd),
            start = Offset.Zero,
            end = Offset(900f, 900f)
        )
    }

    val opticalShader = remember(isInspection) {
        if (!isInspection && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            buildOpticalShaderOrNull()
        } else null
    }

    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = if (glass.isDark) glass.accentGlow.copy(alpha = 0.16f) else glass.accentGlow.copy(alpha = 0.10f),
                        spotColor = if (glass.isDark) Color.Black.copy(alpha = 0.42f) else Color(0xFF0F172A).copy(alpha = 0.14f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(
                if (realBlurEnabled && hazeState != null) {
                    Modifier.hazeEffect(state = hazeState, style = hazeStyle) {
                        blurEnabled = true
                    }
                } else {
                    Modifier.background(
                        if (glass.isDark) {
                            glass.glassFillBottom.copy(alpha = if (strongFill) 0.58f else 0.40f)
                        } else {
                            glass.glassFillBottom.copy(alpha = if (strongFill) 0.72f else 0.54f)
                        }
                    )
                }
            )
            .background(brush = sheenBrush)
            .drawWithCache {
                val cornerPx = cornerRadiusHint.toPx()
                val strokePx = 1.dp.toPx()
                val fresnelStrength = if (glass.isDark) 0.92f else 0.85f
                val dispersionStrength = if (glass.isDark) 0.52f else 0.42f
                val glareStrength = if (glass.isDark) 0.88f else 0.96f

                val shaderBrush = if (opticalShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        opticalShader.setFloatUniform("u_resolution", size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))
                        opticalShader.setFloatUniform("u_cornerRadius", cornerPx)
                        opticalShader.setFloatUniform("u_fresnelIntensity", fresnelStrength)
                        opticalShader.setFloatUniform("u_dispersion", dispersionStrength)
                        opticalShader.setFloatUniform("u_glareIntensity", glareStrength)
                        opticalShader.setFloatUniform(
                            "u_coolTint",
                            glass.dispersionCool.red,
                            glass.dispersionCool.green,
                            glass.dispersionCool.blue,
                            0.42f
                        )
                        opticalShader.setFloatUniform(
                            "u_warmTint",
                            glass.dispersionWarm.red,
                            glass.dispersionWarm.green,
                            glass.dispersionWarm.blue,
                            0.36f
                        )
                        ShaderBrush(opticalShader)
                    }.getOrNull()
                } else null

                onDrawWithContent {
                    drawContent()
                    if (shaderBrush != null) {
                        drawRect(brush = shaderBrush)
                    }
                    drawLine(
                        color = glass.glassTopEdgeHighlight,
                        start = Offset(0f, strokePx * 0.5f),
                        end = Offset(size.width, strokePx * 0.5f),
                        strokeWidth = strokePx
                    )
                    drawLine(
                        color = glass.glassBottomEdgeHighlight,
                        start = Offset(0f, size.height - strokePx * 0.5f),
                        end = Offset(size.width, size.height - strokePx * 0.5f),
                        strokeWidth = strokePx
                    )
                }
            }
            .border(width = borderWidth, brush = prismBorderBrush, shape = shape),
        content = content
    )
}

@Composable
fun CircularGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    strongFill: Boolean = false,
    enableBackdropBlur: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val baseElevation = if (enableBackdropBlur) 10.dp else 4.dp

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .tactilePressMotion(interactionSource = interactionSource, squashAmount = 0.055f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            strongFill = strongFill,
            elevation = baseElevation,
            enableBackdropBlur = enableBackdropBlur,
            cornerRadiusHint = size / 2
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    if (motion.prefersReducedMotion) {
        Box(modifier = modifier.background(glass.shimmerBase))
        return
    }
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = MotionTokens.EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(glass.shimmerBase, glass.shimmerHighlight, glass.shimmerBase),
                start = Offset(translateAnim, translateAnim * 0.5f),
                end = Offset(translateAnim + 280f, translateAnim * 0.5f + 280f)
            )
        )
    )
}

@Composable
fun FloatingGlassBottomBar(
    currentTab: GalleryTab,
    onSelectTab: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier,
    onOpenQuickMenu: (() -> Unit)? = null
) {
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val haptics = LocalHapticFeedback.current

    val albumsSelected = currentTab == GalleryTab.ALBUMS
    val photosSelected = currentTab == GalleryTab.PHOTOS
    val favoritesSelected = currentTab == GalleryTab.FAVORITES

    val activeFill = MaterialTheme.colorScheme.primary
    val activeIconTint = MaterialTheme.colorScheme.onPrimary
    val inactiveIconTint = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .liquidGlassSettle(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Detached Liquid Glass Circle: Collections / Albums
        val albumsInteraction = remember { MutableInteractionSource() }
        val albumsScale by animateFloatAsState(
            targetValue = if (albumsSelected && !motion.prefersReducedMotion) 1.04f else 1.0f,
            animationSpec = motion.floatSpring(allowOvershoot = false),
            label = "dock_albums_scale"
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .minimumInteractiveComponentSize()
                .tactilePressMotion(albumsInteraction, squashAmount = 0.055f, highFrequency = true)
                .graphicsLayer {
                    scaleX = albumsScale
                    scaleY = albumsScale
                }
                .testTag("nav_tab_albums")
                .clickable(
                    interactionSource = albumsInteraction,
                    indication = null,
                    role = Role.Tab
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectTab(GalleryTab.ALBUMS)
                },
            contentAlignment = Alignment.Center
        ) {
            if (albumsSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(14.dp, CircleShape, ambientColor = activeFill, spotColor = activeFill)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(activeFill, activeFill.copy(alpha = 0.86f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = GalleryTab.ALBUMS.label,
                        tint = activeIconTint,
                        modifier = Modifier.size(23.dp)
                    )
                }
            } else {
                GlassSurface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    strongFill = false,
                    elevation = 12.dp,
                    cornerRadiusHint = 28.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = GalleryTab.ALBUMS.label,
                            tint = inactiveIconTint,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
        }

        // Center Liquid Glass Capsule Pill: Home (Photos) + Favorites
        GlassSurface(
            modifier = Modifier.height(56.dp),
            shape = CircleShape,
            strongFill = false,
            elevation = 16.dp,
            cornerRadiusHint = 28.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockCapsuleSlot(
                    icon = if (photosSelected) Icons.Rounded.Home else Icons.Outlined.Home,
                    contentDescription = GalleryTab.PHOTOS.label,
                    isSelected = photosSelected,
                    activeFill = activeFill,
                    activeTint = activeIconTint,
                    inactiveTint = inactiveIconTint,
                    testTag = "nav_tab_photos",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectTab(GalleryTab.PHOTOS)
                    }
                )
                DockCapsuleSlot(
                    icon = if (favoritesSelected) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = GalleryTab.FAVORITES.label,
                    isSelected = favoritesSelected,
                    activeFill = activeFill,
                    activeTint = activeIconTint,
                    inactiveTint = inactiveIconTint,
                    testTag = "nav_tab_favorites",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectTab(GalleryTab.FAVORITES)
                    }
                )
            }
        }

        // Right Detached Liquid Glass Circle: Quick Studio Actions (+)
        val quickInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(56.dp)
                .minimumInteractiveComponentSize()
                .tactilePressMotion(quickInteraction, squashAmount = 0.055f, highFrequency = true)
                .testTag("quick_actions_button")
                .clickable(
                    interactionSource = quickInteraction,
                    indication = null,
                    role = Role.Button
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenQuickMenu?.invoke()
                },
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                strongFill = false,
                elevation = 12.dp,
                cornerRadiusHint = 28.dp
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Quick actions",
                        tint = inactiveIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockCapsuleSlot(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    activeFill: Color,
    activeTint: Color,
    inactiveTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val motion = LocalMotionConfig.current
    val interactionSource = remember { MutableInteractionSource() }
    val slotScale by animateFloatAsState(
        targetValue = if (isSelected && !motion.prefersReducedMotion) 1.03f else 1.0f,
        animationSpec = motion.floatSpring(allowOvershoot = false),
        label = "dock_slot_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeFill else Color.Transparent,
        animationSpec = motion.uiTween(MotionTokens.MicroMs),
        label = "dock_slot_bg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeTint else inactiveTint,
        animationSpec = motion.uiTween(MotionTokens.MicroMs),
        label = "dock_slot_tint"
    )

    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 46.dp)
            .minimumInteractiveComponentSize()
            .tactilePressMotion(interactionSource = interactionSource, squashAmount = 0.045f, highFrequency = true)
            .graphicsLayer {
                scaleX = slotScale
                scaleY = slotScale
            }
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (isSelected) {
                    Modifier.border(0.85.dp, Color.White.copy(alpha = 0.42f), CircleShape)
                } else Modifier
            )
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun LiquidGlassSegmentedControl(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    moreOptions: List<Pair<String, String>> = emptyList(),
    enableBackdropBlur: Boolean = true,
    tagProvider: (String) -> String = { "filter_pill_$it" }
) {
    val glass = LocalLuminaColors.current
    val haptics = LocalHapticFeedback.current
    var moreExpanded by remember { mutableStateOf(false) }

    val selectedMoreOption = remember(selectedKey, moreOptions) {
        moreOptions.firstOrNull { it.first == selectedKey }
    }

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (enableBackdropBlur) Modifier.liquidGlassSettle() else Modifier),
        shape = CircleShape,
        elevation = if (enableBackdropBlur) 10.dp else 4.dp,
        enableBackdropBlur = enableBackdropBlur,
        cornerRadiusHint = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = key == selectedKey
                SegmentedControlButton(
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(key)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tagProvider(key))
                )
            }

            if (moreOptions.isNotEmpty()) {
                val isMoreSelected = selectedMoreOption != null
                Box(modifier = Modifier.weight(1f)) {
                    SegmentedControlButton(
                        label = selectedMoreOption?.second ?: "More",
                        isSelected = isMoreSelected,
                        trailingIcon = Icons.Rounded.ExpandMore,
                        onClick = { moreExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tagProvider("MORE"))
                    )

                    DropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false },
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 18.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (LocalReduceTransparency.current) glass.glassBorder else glass.prismBorderStart.copy(alpha = 0.75f)
                        ),
                        containerColor = if (LocalReduceTransparency.current) glass.solidFallbackElevated else glass.glassStrongFill
                    ) {
                        Column(
                            modifier = Modifier.originAwareEntrance(
                                origin = TransformOrigin(0.85f, 0f)
                            )
                        ) {
                            moreOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Normal,
                                            color = if (key == selectedKey) {
                                                if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        moreExpanded = false
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelect(key)
                                    },
                                    modifier = Modifier.testTag(tagProvider(key))
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
fun LiquidGlassPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val reduceTransparency = LocalReduceTransparency.current
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val highlightBorderColor = when {
        selected && glass.isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        selected -> Color.White
        else -> glass.glassBorder.copy(alpha = 0.72f)
    }
    val textColor by animateColorAsState(
        targetValue = when {
            selected && glass.isDark -> Color.White
            selected -> MaterialTheme.colorScheme.onBackground
            else -> glass.glassSecondaryText
        },
        animationSpec = motion.uiTween(MotionTokens.MicroMs),
        label = "pill_chip_color"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .tactilePressMotion(interactionSource = interactionSource, squashAmount = 0.045f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(CircleShape)
                .then(
                    when {
                        reduceTransparency -> Modifier.background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else glass.solidFallbackElevated
                        )
                        selected -> Modifier
                            .background(
                                Brush.verticalGradient(
                                    if (glass.isDark) {
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                        )
                                    } else {
                                        listOf(
                                            Color.White.copy(alpha = 0.90f),
                                            Color.White.copy(alpha = 0.56f)
                                        )
                                    }
                                )
                            )
                            .drawWithContent {
                                drawContent()
                                val strokePx = 1.dp.toPx()
                                drawLine(
                                    color = glass.glassTopEdgeHighlight,
                                    start = Offset(0f, strokePx * 0.5f),
                                    end = Offset(size.width, strokePx * 0.5f),
                                    strokeWidth = strokePx
                                )
                            }
                        else -> Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    glass.glassFillTop.copy(alpha = if (glass.isDark) 0.14f else 0.44f),
                                    glass.glassFillBottom.copy(alpha = if (glass.isDark) 0.24f else 0.28f)
                                )
                            )
                        )
                    }
                )
                .border(1.dp, highlightBorderColor, CircleShape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SegmentedControlButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null
) {
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val reduceTransparency = LocalReduceTransparency.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isSelected || motion.prefersReducedMotion) 1.0f else 0.98f,
        animationSpec = motion.floatSpring(allowOvershoot = false),
        label = "segment_scale"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected && glass.isDark -> Color.White
            isSelected -> MaterialTheme.colorScheme.onBackground
            else -> glass.glassSecondaryText
        },
        animationSpec = motion.uiTween(MotionTokens.MicroMs),
        label = "segment_text_color"
    )

    val segmentActiveBrush = Brush.horizontalGradient(
        colors = when {
            reduceTransparency && glass.isDark -> listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            )
            reduceTransparency -> listOf(Color.White, Color.White)
            glass.isDark -> listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.26f)
            )
            else -> listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.78f)
            )
        }
    )
    val segmentBorderBrush = Brush.horizontalGradient(
        colors = if (glass.isDark) {
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.68f),
                glass.dispersionCool.copy(alpha = 0.45f)
            )
        } else {
            listOf(
                Color.White,
                glass.prismBorderMid.copy(alpha = 0.72f)
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .tactilePressMotion(interactionSource = interactionSource, squashAmount = 0.04f, highFrequency = true)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSelected && !glass.isDark) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = glass.accentGlow.copy(alpha = 0.14f),
                        spotColor = Color.Black.copy(alpha = 0.12f)
                    )
                } else Modifier
            )
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(brush = segmentActiveBrush)
                        .border(1.dp, segmentBorderBrush, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CollapsingGlassTopBar(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    isLargeDisplayTitle: Boolean = false,
    isScrolled: Boolean,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val hazeState = LocalHazeState.current
    val reduceTransparency = LocalReduceTransparency.current
    val realBlurEnabled = !reduceTransparency && rememberSupportsLiquidBlur() && hazeState != null

    val chromeAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = motion.uiTween(MotionTokens.StandardMs),
        label = "topbar_chrome_alpha"
    )

    val topBarStyle = remember(glass) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 20.dp,
            noiseFactor = 0f,
            tints = listOf(HazeTint(glass.glassFillBottom.copy(alpha = if (glass.isDark) 0.44f else 0.54f))),
            fallbackTint = HazeTint(glass.glassStrongFill)
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (chromeAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = chromeAlpha }
                    .then(
                        when {
                            reduceTransparency -> Modifier.background(glass.solidFallbackSurface)
                            realBlurEnabled && hazeState != null -> Modifier.hazeEffect(state = hazeState, style = topBarStyle) {
                                blurEnabled = true
                            }
                            else -> Modifier.background(glass.glassStrongFill)
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(glass.glassBorder.copy(alpha = glass.glassBorder.alpha * chromeAlpha))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSurface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        elevation = 8.dp,
                        cornerRadiusHint = 24.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            navigationIcon()
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .then(
                        if (onTitleClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTitleClick
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (!eyebrow.isNullOrBlank()) {
                    AnimatedVisibility(
                        visible = !isScrolled,
                        enter = motion.fadeEnter(MotionTokens.MicroMs),
                        exit = motion.fadeExit(MotionTokens.MicroFastMs)
                    ) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = glass.glassSecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
                AnimatedContent(
                    targetState = title to (isScrolled || navigationIcon != null),
                    transitionSpec = { motion.fadeEnter(MotionTokens.MicroMs) togetherWith motion.fadeExit(MotionTokens.MicroFastMs) },
                    label = "topbar_title"
                ) { (resolvedTitle, compact) ->
                    val baseStyle = when {
                        compact -> MaterialTheme.typography.titleLarge.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 23.sp,
                            letterSpacing = (-0.02).em
                        )
                        isLargeDisplayTitle -> MaterialTheme.typography.displaySmall.copy(
                            fontFamily = OutfitFontFamily,
                            fontSize = 27.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.025).em
                        )
                        else -> MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = OutfitFontFamily,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em
                        )
                    }
                    var scaleFactor by remember(resolvedTitle, compact, isLargeDisplayTitle) {
                        mutableFloatStateOf(1f)
                    }
                    Text(
                        text = resolvedTitle,
                        style = baseStyle.copy(fontSize = baseStyle.fontSize * scaleFactor),
                        color = if (!glass.isDark && isLargeDisplayTitle) glass.editorialGold else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { layoutResult ->
                            if (layoutResult.didOverflowWidth && scaleFactor > 0.70f) {
                                scaleFactor = (scaleFactor * 0.90f).coerceAtLeast(0.70f)
                            }
                        }
                    )
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.glassSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = actions
            )
        }
    }
}

@Composable
fun AnimatedEmptyState(
    @DrawableRes illustrationRes: Int,
    title: String,
    subtitle: String,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val floatOffset = if (motion.prefersReducedMotion) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "empty_float")
        infiniteTransition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = MotionTokens.EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "empty_bob"
        ).value
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .liquidGlassSettle(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassSurface(
            modifier = Modifier
                .graphicsLayer {
                    translationY = floatOffset.dp.toPx()
                }
                .fillMaxWidth(0.86f)
                .height(254.dp),
            shape = RoundedCornerShape(34.dp),
            strongFill = true,
            elevation = 20.dp,
            cornerRadiusHint = 34.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = illustrationRes),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(74.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                            )
                        )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.42f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(glass.accentGlow)
                    )
                    Text(
                        text = "PIKTURES GALLERY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = glass.glassSecondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (ctaLabel != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularGlassIconButton(
                    icon = Icons.Rounded.PhotoSizeSelectActual,
                    contentDescription = title,
                    onClick = onCtaClick,
                    size = 54.dp,
                    tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold
                )

                val ctaContainer = MaterialTheme.colorScheme.primary
                val ctaContent = MaterialTheme.colorScheme.onPrimary

                Button(
                    onClick = onCtaClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ctaContainer,
                        contentColor = ctaContent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("empty_state_cta")
                ) {
                    Text(
                        text = ctaLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sculpted Liquid Glass modal dialog with optical Fresnel rim lighting,
 * backdrop blur refraction (when Haze is active), icon crest badge, and
 * automatic solid high-contrast Reduce Transparency fallback.
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    eyebrow: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    showCloseButton: Boolean = true,
    origin: TransformOrigin = TransformOrigin(0.5f, 0.5f),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLuminaColors.current
    val reduceTransparency = LocalReduceTransparency.current
    val dismissInteraction = remember { MutableInteractionSource() }
    val resolvedIconTint = if (!glass.isDark && iconTint == MaterialTheme.colorScheme.primary) {
        glass.editorialGold
    } else {
        iconTint
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (glass.isDark) Color(0x88030712) else Color(0x4D0F172A)
                )
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 22.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val stopClickInteraction = remember { MutableInteractionSource() }
            GlassSurface(
                shape = RoundedCornerShape(32.dp),
                borderWidth = 1.25.dp,
                strongFill = true,
                elevation = 24.dp,
                cornerRadiusHint = 32.dp,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .originAwareEntrance(origin)
                    .clickable(
                        interactionSource = stopClickInteraction,
                        indication = null,
                        onClick = {}
                    )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (!reduceTransparency) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            iconTint.copy(alpha = if (glass.isDark) 0.14f else 0.08f),
                                            glass.dispersionCool.copy(alpha = if (glass.isDark) 0.08f else 0.05f),
                                            Color.Transparent
                                        ),
                                        start = Offset.Zero,
                                        end = Offset(700f, 520f)
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (icon != null) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    iconTint.copy(alpha = if (glass.isDark) 0.28f else 0.18f),
                                                    glass.dispersionCool.copy(alpha = if (glass.isDark) 0.18f else 0.12f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (reduceTransparency) glass.glassBorder else glass.prismBorderStart.copy(alpha = 0.78f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = resolvedIconTint,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                if (!eyebrow.isNullOrBlank()) {
                                    Text(
                                        text = eyebrow,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            letterSpacing = 1.05.sp
                                        ),
                                        color = resolvedIconTint
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (showCloseButton) {
                                CircularGlassIconButton(
                                    icon = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    onClick = onDismissRequest,
                                    size = 38.dp,
                                    enableBackdropBlur = false
                                )
                            }
                        }

                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassModalButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val glass = LocalLuminaColors.current
    val interaction = remember { MutableInteractionSource() }
    val primaryColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val textColor = if (isDestructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    val secondaryColor = when {
        isDestructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.84f)
        textColor == Color.White -> primaryColor.copy(alpha = 0.86f)
        else -> MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .tactilePressMotion(interaction, enabled = enabled, squashAmount = 0.03f)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = CircleShape,
                ambientColor = primaryColor.copy(alpha = 0.32f),
                spotColor = primaryColor.copy(alpha = 0.28f)
            )
            .clip(CircleShape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
                } else {
                    Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.40f), secondaryColor.copy(alpha = 0.40f)))
                }
            )
            .border(
                width = 1.dp,
                color = if (glass.isDark) Color.White.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.56f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LiquidGlassModalSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val glass = LocalLuminaColors.current
    val reduceTransparency = LocalReduceTransparency.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .tactilePressMotion(interaction, squashAmount = 0.03f)
            .clip(CircleShape)
            .background(
                if (reduceTransparency) {
                    glass.solidFallbackElevated
                } else {
                    glass.glassFillTop.copy(alpha = if (glass.isDark) 0.16f else 0.60f)
                }
            )
            .border(
                width = 1.dp,
                color = if (reduceTransparency) glass.glassBorder else glass.prismBorderStart.copy(alpha = 0.65f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun LiquidGlassPreviewCanvas() {
    val glass = LocalLuminaColors.current
    MeshGradientBackground(useDotPattern = glass.accentGlow == com.example.ui.theme.VaultL2Accent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CollapsingGlassTopBar(
                    eyebrow = "Piktures",
                    title = "Collection",
                    isLargeDisplayTitle = true,
                    isScrolled = false
                ) {
                    CircularGlassIconButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "Search",
                        onClick = {}
                    )
                }

                LiquidGlassSegmentedControl(
                    options = listOf("ALL" to "All", "IMAGES" to "Photos", "VIDEOS" to "Videos"),
                    selectedKey = "ALL",
                    onSelect = {},
                    moreOptions = listOf("ALBUM_1" to "Camera")
                )

                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Liquid Glass Card",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Physically convincing refraction, dispersion, and Fresnel reflections.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.glassSecondaryText
                        )
                    }
                }
            }

            FloatingGlassBottomBar(
                currentTab = GalleryTab.PHOTOS,
                onSelectTab = {}
            )
        }
    }
}

@Preview(name = "Liquid Glass - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GlassComponentsDarkPreview() {
    LuminaTheme(themeMode = ThemeMode.DARK, atmosphere = VaultAtmosphere.GALLERY) {
        LiquidGlassPreviewCanvas()
    }
}

@Preview(name = "Liquid Glass - Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun GlassComponentsLightPreview() {
    LuminaTheme(themeMode = ThemeMode.LIGHT, atmosphere = VaultAtmosphere.GALLERY) {
        LiquidGlassPreviewCanvas()
    }
}

@Preview(name = "Liquid Glass - Vault L2", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GlassComponentsVaultL2Preview() {
    LuminaTheme(themeMode = ThemeMode.DARK, atmosphere = VaultAtmosphere.VAULT_LEVEL_2) {
        LiquidGlassPreviewCanvas()
    }
}
