package com.example.ui.editor.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.TextureOverlay
import com.example.ui.editor.photo.MarkupFontStyle
import com.example.ui.editor.photo.PhotoFilterPreset
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sin

@OptIn(UnstableApi::class)
object VideoEffectsFactory {

    /**
     * Scales 16-bit PCM audio samples by [gain] (0.0f = mute, 1.0f = unity).
     */
    class GainAudioProcessor(private val gain: Float) : BaseAudioProcessor() {
        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
                throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            }
            return inputAudioFormat
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val remaining = inputBuffer.remaining()
            if (remaining == 0) return
            val out = replaceOutputBuffer(remaining)
            while (inputBuffer.remaining() >= 2) {
                val sample = inputBuffer.short.toFloat()
                val scaled = (sample * gain).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out.putShort(scaled.toShort())
            }
            out.flip()
        }
    }

    /**
     * Converts Brightness, Contrast, Saturation, Warmth & PhotoFilterPreset into a 4x4 GL RgbMatrix.
     */
    class ColorGradeRgbMatrix(private val color: VideoColorAdjustments) : RgbMatrix {
        private val glMatrix: FloatArray by lazy { computeGl4x4Matrix(color) }

        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = glMatrix

        private fun computeGl4x4Matrix(adj: VideoColorAdjustments): FloatArray {
            val cm = ColorMatrix()

            // Brightness & Contrast
            val bright = (adj.brightness / 100f) * 85f
            val contrast = (1f + (adj.contrast / 100f) * 0.55f).coerceIn(0.3f, 1.8f)
            val trans = (-0.5f * contrast + 0.5f) * 255f + bright
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, trans,
                        0f, contrast, 0f, 0f, trans,
                        0f, 0f, contrast, 0f, trans,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )

            // Warmth
            val warm = (adj.warmth / 100f) * 32f
            if (abs(warm) > 0.1f) {
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, warm,
                            0f, 1f, 0f, 0f, 0f,
                            0f, 0f, 1f, 0f, -warm,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }

            // Saturation
            val sat = (1f + adj.saturation / 100f).coerceIn(0f, 2f)
            cm.postConcat(ColorMatrix().apply { setSaturation(sat) })

            // Filter preset blended by filterIntensity
            val filter = adj.filter
            val mix = adj.filterIntensity.coerceIn(0f, 1f)
            if (filter != PhotoFilterPreset.ORIGINAL && mix > 0.001f) {
                val fCm = ColorMatrix()
                fCm.setScale(
                    1f + (filter.rScale - 1f) * mix,
                    1f + (filter.gScale - 1f) * mix,
                    1f + (filter.bScale - 1f) * mix,
                    1f
                )
                if (filter.monochrome) {
                    fCm.postConcat(ColorMatrix().apply { setSaturation((1f - mix).coerceIn(0f, 1f)) })
                } else if (abs(filter.satBoost) > 0.01f) {
                    fCm.postConcat(ColorMatrix().apply { setSaturation((1f + filter.satBoost * mix).coerceIn(0f, 2f)) })
                }
                if (abs(filter.contrastBoost) > 0.01f) {
                    val fc = 1f + filter.contrastBoost * mix
                    val ft = (-0.5f * fc + 0.5f) * 255f
                    fCm.postConcat(
                        ColorMatrix(
                            floatArrayOf(
                                fc, 0f, 0f, 0f, ft,
                                0f, fc, 0f, 0f, ft,
                                0f, 0f, fc, 0f, ft,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    )
                }
                cm.postConcat(fCm)
            }

            // Android ColorMatrix is 4x5 row-major where offset is [0..255].
            // Media3 RgbMatrix expects 4x4 column-major OpenGL matrix on [0..1] RGB.
            val a = cm.array
            val out = FloatArray(16)
            for (row in 0..2) {
                for (col in 0..2) {
                    out[col * 4 + row] = a[row * 5 + col]
                }
                // Translation in column 3 (indices 12, 13, 14) normalized to 0..1
                out[3 * 4 + row] = a[row * 5 + 4] / 255f
            }
            out[15] = 1f
            return out
        }
    }

    /**
     * Time-ranged BitmapOverlay rendering styled text overlays onto exported frames.
     */
    class TimedTextBitmapOverlay(
        private val overlay: VideoTextOverlay,
        private val canvasWidth: Int = 1080,
        private val canvasHeight: Int = 1080
    ) : BitmapOverlay() {

        private val emptyBitmap: Bitmap by lazy {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        }

        private val renderedBitmap: Bitmap by lazy {
            Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888).also { bmp ->
                val canvas = Canvas(bmp)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = overlay.colorArgb.toInt()
                    textSize = overlay.sizeSp * (canvasWidth / 380f)
                    textAlign = Paint.Align.CENTER
                    typeface = when (overlay.fontStyle) {
                        MarkupFontStyle.SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                        MarkupFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        MarkupFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        MarkupFontStyle.BOLD -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                    }
                    setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(165, 0, 0, 0))
                }
                val fm = paint.fontMetrics
                val baselineOffset = -(fm.ascent + fm.descent) / 2f
                canvas.drawText(
                    overlay.text,
                    overlay.normX * canvasWidth,
                    overlay.normY * canvasHeight + baselineOffset,
                    paint
                )
            }
        }

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            val ms = presentationTimeUs / 1000L
            return if (ms in overlay.startMs..overlay.endMs && overlay.text.isNotBlank()) {
                renderedBitmap
            } else {
                emptyBitmap
            }
        }
    }

    fun buildClipVideoEffects(
        clip: VideoClipItem,
        aspectRatio: VideoAspectRatio,
        colorAdjustments: VideoColorAdjustments,
        includeSpeedEffect: Boolean
    ): List<Effect> {
        val effects = ArrayList<Effect>()

        // 1. Rotation & Flip
        if (clip.rotationDegrees != 0 || clip.flipHorizontal) {
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(-clip.rotationDegrees.toFloat())
                    .setScale(if (clip.flipHorizontal) -1f else 1f, 1f)
                    .build()
            )
        }

        // 2. Stabilization crop + subtle optical motion smoothing
        if (clip.stabilize) {
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setScale(1.10f, 1.10f)
                    .build()
            )
            effects.add(MatrixTransformation { ptsUs ->
                val t = ptsUs / 1_000_000.0
                android.graphics.Matrix().apply {
                    val dx = (sin(t * 1.4) * 0.008).toFloat()
                    val dy = (sin(t * 1.1 + 1.2) * 0.008).toFloat()
                    postTranslate(dx, dy)
                }
            })
        }

        // 3. Aspect ratio crop
        aspectRatio.ratio?.let { targetRatio ->
            effects.add(
                Presentation.createForAspectRatio(
                    targetRatio,
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                )
            )
        }

        // 4. Color Grading (Brightness, Contrast, Saturation, Warmth, Filter)
        val hasColorEdit = colorAdjustments.filter != PhotoFilterPreset.ORIGINAL ||
            colorAdjustments.brightness != 0f ||
            colorAdjustments.contrast != 0f ||
            colorAdjustments.saturation != 0f ||
            colorAdjustments.warmth != 0f
        if (hasColorEdit) {
            effects.add(ColorGradeRgbMatrix(colorAdjustments))
        }

        // 5. Transition opening pulse if enabled
        when (clip.transitionAfter) {
            VideoTransitionStyle.FLASH -> {
                effects.add(MatrixTransformation { ptsUs ->
                    val tSec = ptsUs / 1_000_000f
                    android.graphics.Matrix().apply {
                        if (tSec < 0.25f) {
                            val pulse = 1.0f + (0.25f - tSec) * 0.24f
                            postScale(pulse, pulse)
                        }
                    }
                })
            }
            VideoTransitionStyle.SLIDE -> {
                effects.add(MatrixTransformation { ptsUs ->
                    val tSec = ptsUs / 1_000_000f
                    android.graphics.Matrix().apply {
                        if (tSec < 0.35f) {
                            val offset = ((0.35f - tSec) / 0.35f) * 0.35f
                            postTranslate(offset, 0f)
                        }
                    }
                })
            }
            else -> Unit
        }

        // 6. Speed
        if (includeSpeedEffect && abs(clip.speed - 1f) > 0.01f) {
            effects.add(SpeedChangeEffect(clip.speed.coerceIn(0.25f, 4f)))
        }

        return effects
    }

    fun buildClipAudioProcessors(
        speed: Float,
        originalVolume: Float
    ): List<AudioProcessor> {
        val processors = ArrayList<AudioProcessor>()
        if (abs(speed - 1f) > 0.01f) {
            processors.add(SonicAudioProcessor().apply {
                setSpeed(speed.coerceIn(0.25f, 4f))
            })
        }
        if (originalVolume < 0.99f) {
            processors.add(GainAudioProcessor(originalVolume.coerceIn(0f, 1f)))
        }
        return processors
    }

    fun buildCompositionEffects(
        overlays: List<VideoTextOverlay>,
        resolution: VideoExportResolution?,
        aspectRatio: VideoAspectRatio
    ): List<Effect> {
        val effects = ArrayList<Effect>()
        if (overlays.isNotEmpty()) {
            val ratio = aspectRatio.ratio ?: (16f / 9f)
            val h = 720
            val w = (h * ratio).toInt().coerceIn(360, 1920)
            val bitmapOverlays: List<TextureOverlay> = overlays.map { TimedTextBitmapOverlay(it, w, h) }
            effects.add(OverlayEffect(bitmapOverlays))
        }
        if (resolution != null) {
            effects.add(Presentation.createForHeight(resolution.targetHeight))
        }
        return effects
    }
}
