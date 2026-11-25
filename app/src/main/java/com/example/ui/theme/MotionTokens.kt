package com.example.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Central Motion System for yourSpace.
 * - All standard UI durations stay <= 300ms; only Peak-End celebrations (#19) exceed 300ms.
 * - Curves use slow-in/slow-out or springs (no linear curves except progress indicators).
 * - Springs are naturally interruptible and reversible mid-flight (#4).
 * - Automatically honors system reduced-motion (`ANIMATOR_DURATION_SCALE == 0f`).
 */
object MotionTokens {
    // Duration tokens (ms)
    const val MicroFastMs = 110
    const val MicroMs = 160
    const val StandardMs = 240
    const val SheetMs = 280
    const val PeakCelebrationMs = 540

    // Stagger tokens (#12)
    const val StaggerStepMs = 28
    const val StaggerCapMs = 240

    // Anticipation wind-up (#8)
    const val AnticipationMs = 60

    // Easing curves (#5 Slow in, slow out)
    val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    val ArcCurve = CubicBezierEasing(0.32f, 0.94f, 0.48f, 1f)

    // Spring presets (#4 Interruptible, #10 Follow-through, #13 Exaggeration)
    val SnappySpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 620f
    )

    val FluidSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.92f,
        stiffness = 380f
    )

    val FollowThroughSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.64f,
        stiffness = 420f
    )

    val ExaggeratedSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.48f,
        stiffness = 340f
    )

    val SnappyDpSpring: FiniteAnimationSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 600f
    )

    val FollowThroughDpSpring: FiniteAnimationSpec<Dp> = spring(
        dampingRatio = 0.68f,
        stiffness = 420f
    )

    val SnappyIntOffsetSpring: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 480f
    )
}

@Immutable
data class MotionConfig(
    val prefersReducedMotion: Boolean = false
) {
    fun <T> uiTween(durationMs: Int = MotionTokens.StandardMs): FiniteAnimationSpec<T> =
        if (prefersReducedMotion) tween(durationMillis = 80)
        else tween(durationMillis = durationMs.coerceAtMost(300), easing = MotionTokens.EaseOutQuint)

    fun <T> exitTween(durationMs: Int = MotionTokens.MicroMs): FiniteAnimationSpec<T> =
        if (prefersReducedMotion) tween(durationMillis = 60)
        else tween(durationMillis = durationMs.coerceAtMost(300), easing = MotionTokens.EaseInOutCubic)

    fun floatSpring(allowOvershoot: Boolean = false): FiniteAnimationSpec<Float> =
        if (prefersReducedMotion) tween(durationMillis = 80)
        else if (allowOvershoot) MotionTokens.FollowThroughSpring
        else MotionTokens.SnappySpring

    fun fluidFloatSpring(): FiniteAnimationSpec<Float> =
        if (prefersReducedMotion) tween(durationMillis = 90)
        else MotionTokens.FluidSpring

    fun staggerDelayMs(index: Int): Int =
        if (prefersReducedMotion || index <= 0) 0
        else (index * MotionTokens.StaggerStepMs).coerceAtMost(MotionTokens.StaggerCapMs)

    fun fadeEnter(durationMs: Int = MotionTokens.StandardMs): EnterTransition =
        fadeIn(animationSpec = uiTween(durationMs))

    fun fadeExit(durationMs: Int = MotionTokens.MicroMs): ExitTransition =
        fadeOut(animationSpec = exitTween(durationMs))

    fun bottomSheetEnter(): EnterTransition =
        if (prefersReducedMotion) {
            fadeIn(tween(90))
        } else {
            fadeIn(tween(MotionTokens.StandardMs, easing = MotionTokens.EaseOutQuint)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.90f,
                        stiffness = 400f
                    )
                ) { it / 3 }
        }

    fun bottomSheetExit(): ExitTransition =
        if (prefersReducedMotion) {
            fadeOut(tween(70))
        } else {
            fadeOut(tween(MotionTokens.MicroMs, easing = MotionTokens.EaseInOutCubic)) +
                slideOutVertically(
                    animationSpec = tween(MotionTokens.MicroMs, easing = MotionTokens.EaseInOutCubic)
                ) { it / 3 }
        }

    fun originMenuEnter(origin: TransformOrigin = TransformOrigin(0.92f, 0f)): EnterTransition =
        if (prefersReducedMotion) {
            fadeIn(tween(80))
        } else {
            fadeIn(tween(MotionTokens.MicroMs, easing = MotionTokens.EaseOutQuint)) +
                scaleIn(
                    initialScale = 0.86f,
                    transformOrigin = origin,
                    animationSpec = tween(MotionTokens.MicroMs + 30, easing = MotionTokens.EaseOutQuint)
                )
        }

    fun originMenuExit(origin: TransformOrigin = TransformOrigin(0.92f, 0f)): ExitTransition =
        if (prefersReducedMotion) {
            fadeOut(tween(60))
        } else {
            fadeOut(tween(MotionTokens.MicroFastMs, easing = MotionTokens.EaseInOutCubic)) +
                scaleOut(
                    targetScale = 0.90f,
                    transformOrigin = origin,
                    animationSpec = tween(MotionTokens.MicroFastMs, easing = MotionTokens.EaseInOutCubic)
                )
        }
}

val LocalMotionConfig = compositionLocalOf { MotionConfig() }

private fun readAnimatorReducedMotion(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) <= 0f
}.getOrDefault(false)

@Composable
fun rememberMotionConfig(forceReducedMotion: Boolean = false): MotionConfig {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var sysReduced by remember(context, isInspection) {
        mutableStateOf(if (isInspection) false else readAnimatorReducedMotion(context))
    }

    if (!isInspection) {
        DisposableEffect(context, lifecycleOwner) {
            val refresh = { sysReduced = readAnimatorReducedMotion(context) }
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

    val reduced = forceReducedMotion || sysReduced
    return remember(reduced) { MotionConfig(prefersReducedMotion = reduced) }
}

/**
 * #8 Anticipation + #9 Squash & Stretch + #4 Interruptible press physics.
 * Subtle volume-preserving squash on press (wider X, compressed Y) that springs back cleanly on release.
 */
fun Modifier.tactilePressMotion(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    squashAmount: Float = 0.055f,
    highFrequency: Boolean = false
): Modifier = composed {
    val motion = LocalMotionConfig.current
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScaleX = when {
        !enabled || motion.prefersReducedMotion -> 1f
        isPressed -> 1f + squashAmount * 0.42f
        else -> 1f
    }
    val targetScaleY = when {
        !enabled || motion.prefersReducedMotion -> 1f
        isPressed -> 1f - squashAmount
        else -> 1f
    }
    val targetAlpha = when {
        !enabled -> 0.55f
        motion.prefersReducedMotion && isPressed -> 0.80f
        else -> 1f
    }

    val spec: FiniteAnimationSpec<Float> = if (motion.prefersReducedMotion || highFrequency) {
        motion.floatSpring(allowOvershoot = false)
    } else {
        motion.floatSpring(allowOvershoot = !isPressed)
    }

    val sx by animateFloatAsState(targetValue = targetScaleX, animationSpec = spec, label = "squash_x")
    val sy by animateFloatAsState(targetValue = targetScaleY, animationSpec = spec, label = "squash_y")
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(MotionTokens.MicroFastMs),
        label = "press_alpha"
    )

    this.graphicsLayer {
        scaleX = sx
        scaleY = sy
        this.alpha = alpha
    }
}

/**
 * #12 Staggered entrance (28ms per item, capped at 240ms) using GPU-only opacity + translationY.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    enabled: Boolean = true
): Modifier = composed {
    val motion = LocalMotionConfig.current
    if (!enabled || LocalInspectionMode.current) return@composed this

    val startOffsetPx = with(LocalDensity.current) { 14.dp.toPx() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(index, motion.prefersReducedMotion) {
        val delay = motion.staggerDelayMs(index)
        if (motion.prefersReducedMotion) {
            progress.animateTo(1f, tween(90))
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MotionTokens.StandardMs,
                    delayMillis = delay,
                    easing = MotionTokens.EaseOutQuint
                )
            )
        }
    }

    this.graphicsLayer {
        val p = progress.value
        alpha = p
        if (!motion.prefersReducedMotion) {
            translationY = (1f - p) * startOffsetPx
        }
    }
}

/**
 * #7 Origin-aware scale + fade entrance for dialogs, cards, and popovers.
 */
fun Modifier.originAwareEntrance(
    origin: TransformOrigin = TransformOrigin(0.5f, 0.5f),
    initialScale: Float = 0.88f
): Modifier = composed {
    val motion = LocalMotionConfig.current
    if (LocalInspectionMode.current) return@composed this

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (motion.prefersReducedMotion) {
            progress.animateTo(1f, tween(80))
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 460f)
            )
        }
    }

    this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        if (!motion.prefersReducedMotion) {
            transformOrigin = origin
            val s = initialScale + (1f - initialScale) * p
            scaleX = s
            scaleY = s
        }
    }
}

/**
 * #11 Rubber banding: Smooth tanh-damped overscroll past top/bottom edges of scrollable containers
 * with interruptible spring-back on release.
 */
fun Modifier.verticalRubberBand(
    maxPullDp: Dp = 40.dp,
    enabled: Boolean = true
): Modifier = composed {
    val motion = LocalMotionConfig.current
    if (!enabled || motion.prefersReducedMotion) return@composed this

    val maxPullPx = with(LocalDensity.current) { maxPullDp.toPx() }.coerceAtLeast(1f)
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val rawAccum = remember { floatArrayOf(0f) }

    val connection = remember(maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val cur = rawAccum[0]
                if (cur == 0f || (cur > 0f && available.y > 0f) || (cur < 0f && available.y < 0f)) {
                    return Offset.Zero
                }
                val next = cur + available.y
                val consumedY = if ((cur > 0f && next <= 0f) || (cur < 0f && next >= 0f)) {
                    rawAccum[0] = 0f
                    -cur
                } else {
                    rawAccum[0] = next
                    available.y
                }
                val damped = maxPullPx * tanh(rawAccum[0] / (maxPullPx * 2.4f))
                scope.launch { offsetY.snapTo(damped) }
                return Offset(0f, consumedY)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || abs(available.y) < 0.25f) return Offset.Zero
                rawAccum[0] += available.y
                val damped = maxPullPx * tanh(rawAccum[0] / (maxPullPx * 2.4f))
                scope.launch { offsetY.snapTo(damped) }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (abs(rawAccum[0]) > 0.5f || abs(offsetY.value) > 0.5f) {
                    rawAccum[0] = 0f
                    offsetY.animateTo(0f, MotionTokens.FluidSpring)
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (abs(rawAccum[0]) > 0.5f || abs(offsetY.value) > 0.5f) {
                    rawAccum[0] = 0f
                    offsetY.animateTo(0f, MotionTokens.FluidSpring)
                }
                return Velocity.Zero
            }
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer { translationY = offsetY.value }
}

/**
 * #11 Rubber banding on horizontal scrollable strips (e.g. MultiClipTimelineBar).
 */
fun Modifier.horizontalRubberBand(
    maxPullDp: Dp = 32.dp,
    enabled: Boolean = true
): Modifier = composed {
    val motion = LocalMotionConfig.current
    if (!enabled || motion.prefersReducedMotion) return@composed this

    val maxPullPx = with(LocalDensity.current) { maxPullDp.toPx() }.coerceAtLeast(1f)
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val rawAccum = remember { floatArrayOf(0f) }

    val connection = remember(maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val cur = rawAccum[0]
                if (cur == 0f || (cur > 0f && available.x > 0f) || (cur < 0f && available.x < 0f)) {
                    return Offset.Zero
                }
                val next = cur + available.x
                val consumedX = if ((cur > 0f && next <= 0f) || (cur < 0f && next >= 0f)) {
                    rawAccum[0] = 0f
                    -cur
                } else {
                    rawAccum[0] = next
                    available.x
                }
                val damped = maxPullPx * tanh(rawAccum[0] / (maxPullPx * 2.4f))
                scope.launch { offsetX.snapTo(damped) }
                return Offset(consumedX, 0f)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || abs(available.x) < 0.25f) return Offset.Zero
                rawAccum[0] += available.x
                val damped = maxPullPx * tanh(rawAccum[0] / (maxPullPx * 2.4f))
                scope.launch { offsetX.snapTo(damped) }
                return Offset(available.x, 0f)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (abs(rawAccum[0]) > 0.5f || abs(offsetX.value) > 0.5f) {
                    rawAccum[0] = 0f
                    offsetX.animateTo(0f, MotionTokens.FluidSpring)
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (abs(rawAccum[0]) > 0.5f || abs(offsetX.value) > 0.5f) {
                    rawAccum[0] = 0f
                    offsetX.animateTo(0f, MotionTokens.FluidSpring)
                }
                return Velocity.Zero
            }
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer { translationX = offsetX.value }
}

/**
 * #6 Arcs + #14 Secondary Action: Emits curved parabolic arc particles whenever `triggerCount` increments.
 */
@Composable
fun ArcParticleBurst(
    triggerCount: Int,
    color: Color,
    modifier: Modifier = Modifier,
    particleCount: Int = 6,
    spreadDp: Dp = 26.dp
) {
    val motion = LocalMotionConfig.current
    if (motion.prefersReducedMotion || triggerCount <= 0) return

    val progress = remember { Animatable(1f) }
    LaunchedEffect(triggerCount) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 440, easing = MotionTokens.ArcCurve)
        )
    }

    val p = progress.value
    if (p >= 0.99f) return

    val spreadPx = with(LocalDensity.current) { spreadDp.toPx() }
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val alpha = (1f - p).coerceIn(0f, 1f)
        val radius = (3.5.dp.toPx() * (1f - p * 0.65f)).coerceAtLeast(1.dp.toPx())
        for (i in 0 until particleCount) {
            val angle = ((2.0 * PI / particleCount) * i) - (PI / 2.0)
            val dx = (cos(angle) * spreadPx * p).toFloat()
            val arcLift = (sin(PI * p) * spreadPx * 0.38f).toFloat()
            val dy = (sin(angle) * spreadPx * p).toFloat() - arcLift
            drawCircle(
                color = color.copy(alpha = alpha * 0.9f),
                radius = radius,
                center = Offset(center.x + dx, center.y + dy)
            )
        }
    }
}

/**
 * #8 Anticipation + #13 Exaggeration + #14 Secondary action + #6 Arcs:
 * Favorite heart icon with subtle dip -> elastic pop + curved particle burst on activation.
 */
@Composable
fun AnimatedFavoriteHeartIcon(
    isFavorite: Boolean,
    contentDescription: String?,
    activeTint: Color = FavoriteRose,
    inactiveTint: Color = Color.White,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val motion = LocalMotionConfig.current
    val scale = remember { Animatable(1f) }
    var prevFavorite by remember { androidx.compose.runtime.mutableStateOf(isFavorite) }
    var burstTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isFavorite) {
        if (isFavorite && !prevFavorite) {
            burstTrigger++
            if (!motion.prefersReducedMotion) {
                scale.animateTo(0.80f, tween(MotionTokens.AnticipationMs, easing = MotionTokens.EaseInOutCubic))
                scale.animateTo(1.26f, MotionTokens.ExaggeratedSpring)
                scale.animateTo(1.0f, MotionTokens.FollowThroughSpring)
            }
        } else if (!isFavorite && prevFavorite && !motion.prefersReducedMotion) {
            scale.animateTo(0.86f, tween(70))
            scale.animateTo(1.0f, MotionTokens.SnappySpring)
        }
        prevFavorite = isFavorite
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ArcParticleBurst(
            triggerCount = burstTrigger,
            color = activeTint,
            spreadDp = iconSize * 1.15f,
            modifier = Modifier.size(iconSize * 2.2f)
        )
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = contentDescription,
            tint = if (isFavorite) activeTint else inactiveTint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}

/**
 * #19 Peak-End Rule + #14 Secondary Action (self-drawing vector checkmark + arc sparkles):
 * Celebratory frosted glass overlay shown at major completion moments (Photo Save, Video Export).
 */
@Composable
fun PeakSuccessOverlay(
    visible: Boolean,
    title: String,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val motion = LocalMotionConfig.current
    val ringScale = remember { Animatable(if (motion.prefersReducedMotion) 1f else 0.72f) }
    val checkProgress = remember { Animatable(if (motion.prefersReducedMotion) 1f else 0f) }
    var burstTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(visible, title) {
        burstTrigger++
        if (!motion.prefersReducedMotion) {
            ringScale.snapTo(0.72f)
            checkProgress.snapTo(0f)
            launch {
                ringScale.animateTo(1f, MotionTokens.ExaggeratedSpring)
            }
            checkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MotionTokens.PeakCelebrationMs - 140,
                    delayMillis = 90,
                    easing = MotionTokens.EaseOutQuint
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .originAwareEntrance(initialScale = 0.86f)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = accentColor.copy(alpha = 0.48f),
                    spotColor = accentColor.copy(alpha = 0.48f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xEE141B29),
                            Color(0xEE0B101A)
                        )
                    )
                )
                .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .padding(horizontal = 26.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ArcParticleBurst(
                        triggerCount = burstTrigger,
                        color = accentColor,
                        particleCount = 8,
                        spreadDp = 34.dp,
                        modifier = Modifier.size(72.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                scaleX = ringScale.value
                                scaleY = ringScale.value
                            }
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.22f))
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val t = checkProgress.value.coerceIn(0f, 1f)
                            val p0 = Offset(size.width * 0.18f, size.height * 0.54f)
                            val p1 = Offset(size.width * 0.42f, size.height * 0.78f)
                            val p2 = Offset(size.width * 0.84f, size.height * 0.26f)
                            val path = Path().apply {
                                moveTo(p0.x, p0.y)
                                val seg1 = (t / 0.40f).coerceIn(0f, 1f)
                                lineTo(
                                    p0.x + (p1.x - p0.x) * seg1,
                                    p0.y + (p1.y - p0.y) * seg1
                                )
                                if (t > 0.40f) {
                                    val seg2 = ((t - 0.40f) / 0.60f).coerceIn(0f, 1f)
                                    lineTo(
                                        p1.x + (p2.x - p1.x) * seg2,
                                        p1.y + (p2.y - p1.y) * seg2
                                    )
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        }
    }
}
