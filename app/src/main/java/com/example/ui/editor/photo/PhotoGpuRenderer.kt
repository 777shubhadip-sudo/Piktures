package com.example.ui.editor.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.exifinterface.media.ExifInterface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object PhotoGpuRenderer {

    private const val AGSL_PHOTO_SHADER = """
        uniform shader inputShader;
        uniform float2 resolution;
        uniform float brightness;
        uniform float contrast;
        uniform float highlights;
        uniform float shadows;
        uniform float saturation;
        uniform float warmth;
        uniform float tint;
        uniform float sharpen;
        uniform float vignette;
        uniform float3 filterScale;
        uniform float filterSat;
        uniform float filterContrast;
        uniform float filterMono;
        uniform float filterIntensity;

        float luma(float3 c) {
            return dot(c, float3(0.2126, 0.7152, 0.0722));
        }

        half4 main(float2 coord) {
            half4 src = inputShader.eval(coord);
            float a = src.a;
            float3 rgb = src.rgb / max(a, 0.0001);

            // Sharpen (unsharp mask via 4-neighbor tap)
            if (abs(sharpen) > 0.001) {
                float3 n = inputShader.eval(coord + float2(0.0, -1.0)).rgb;
                float3 s = inputShader.eval(coord + float2(0.0, 1.0)).rgb;
                float3 e = inputShader.eval(coord + float2(1.0, 0.0)).rgb;
                float3 w = inputShader.eval(coord + float2(-1.0, 0.0)).rgb;
                float3 blur = (n + s + e + w) * 0.25;
                rgb = rgb + (rgb - blur) * (sharpen * 1.4);
            }

            // Brightness
            rgb += brightness * 0.35;

            // Contrast
            float cFactor = 1.0 + contrast * 0.65;
            rgb = (rgb - 0.5) * cFactor + 0.5;

            // Highlights & Shadows tone curve
            float lum = luma(clamp(rgb, 0.0, 1.0));
            float hiMask = smoothstep(0.48, 0.98, lum);
            float shMask = 1.0 - smoothstep(0.02, 0.52, lum);
            rgb += highlights * 0.32 * hiMask;
            rgb += shadows * 0.32 * shMask;

            // Warmth (red/blue opponent) & Tint (green/magenta)
            rgb.r += warmth * 0.16;
            rgb.b -= warmth * 0.16;
            rgb.g += tint * 0.14;
            rgb.r -= tint * 0.07;
            rgb.b -= tint * 0.07;

            // Saturation
            float gray = luma(rgb);
            float satFactor = max(0.0, 1.0 + saturation);
            rgb = mix(float3(gray), rgb, satFactor);

            // Filter color grade blended by filterIntensity
            if (filterIntensity > 0.001) {
                float3 graded = rgb * filterScale;
                float gGray = luma(graded);
                if (filterMono > 0.5) {
                    graded = float3(gGray);
                } else {
                    graded = mix(float3(gGray), graded, max(0.0, 1.0 + filterSat));
                }
                graded = (graded - 0.5) * (1.0 + filterContrast) + 0.5;
                rgb = mix(rgb, graded, filterIntensity);
            }

            // Vignette
            if (abs(vignette) > 0.001 && resolution.x > 1.0 && resolution.y > 1.0) {
                float2 uv = (coord / resolution) - 0.5;
                float dist = length(uv) * 1.4142;
                float vigMask = smoothstep(0.35, 1.05, dist);
                rgb *= (1.0 - vigMask * vignette * 0.72);
            }

            rgb = clamp(rgb, 0.0, 1.0);
            return half4(rgb * a, a);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33ShaderCache {
        val threadLocal: ThreadLocal<RuntimeShader> = object : ThreadLocal<RuntimeShader>() {
            override fun initialValue(): RuntimeShader = RuntimeShader(AGSL_PHOTO_SHADER)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun buildGpuRenderEffect(
        widthPx: Float,
        heightPx: Float,
        adjustments: PhotoAdjustments,
        filter: PhotoFilterPreset,
        filterIntensity: Float
    ): RenderEffect? = runCatching {
        val shader = (Api33ShaderCache.threadLocal.get() ?: RuntimeShader(AGSL_PHOTO_SHADER)).apply {
            setFloatUniform("resolution", maxOf(1f, widthPx), maxOf(1f, heightPx))
            setFloatUniform("brightness", adjustments.brightness / 100f)
            setFloatUniform("contrast", adjustments.contrast / 100f)
            setFloatUniform("highlights", adjustments.highlights / 100f)
            setFloatUniform("shadows", adjustments.shadows / 100f)
            setFloatUniform("saturation", adjustments.saturation / 100f)
            setFloatUniform("warmth", adjustments.warmth / 100f)
            setFloatUniform("tint", adjustments.tint / 100f)
            setFloatUniform("sharpen", adjustments.sharpen / 100f)
            setFloatUniform("vignette", adjustments.vignette / 100f)
            setFloatUniform("filterScale", filter.rScale, filter.gScale, filter.bScale)
            setFloatUniform("filterSat", filter.satBoost)
            setFloatUniform("filterContrast", filter.contrastBoost)
            setFloatUniform("filterMono", if (filter.monochrome) 1f else 0f)
            setFloatUniform("filterIntensity", filterIntensity.coerceIn(0f, 1f))
        }
        RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
    }.getOrNull()

    fun buildFallbackColorMatrixValues(
        adjustments: PhotoAdjustments,
        filter: PhotoFilterPreset,
        filterIntensity: Float
    ): FloatArray {
        val cm = ColorMatrix()

        // Brightness + Contrast
        val b = (adjustments.brightness / 100f) * 85f
        val c = (1f + (adjustments.contrast / 100f) * 0.55f).coerceIn(0.3f, 1.8f)
        val t = (-0.5f * c + 0.5f) * 255f + b
        cm.postConcat(
            ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        // Highlights + Shadows approximation via subtle exposure + gamma shift
        val shadowLift = (adjustments.shadows / 100f) * 24f
        val hiTrim = (adjustments.highlights / 100f) * 0.14f
        if (abs(shadowLift) > 0.1f || abs(hiTrim) > 0.001f) {
            val scale = (1f + hiTrim).coerceIn(0.75f, 1.25f)
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, shadowLift,
                        0f, scale, 0f, 0f, shadowLift,
                        0f, 0f, scale, 0f, shadowLift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // Warmth + Tint
        val wShift = (adjustments.warmth / 100f) * 32f
        val tShift = (adjustments.tint / 100f) * 26f
        if (abs(wShift) > 0.1f || abs(tShift) > 0.1f) {
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, wShift - tShift * 0.4f,
                        0f, 1f, 0f, 0f, tShift,
                        0f, 0f, 1f, 0f, -wShift - tShift * 0.4f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        // Saturation
        val sat = (1f + adjustments.saturation / 100f).coerceIn(0f, 2f)
        cm.postConcat(ColorMatrix().apply { setSaturation(sat) })

        // Filter blend
        if (filter != PhotoFilterPreset.ORIGINAL && filterIntensity > 0.001f) {
            val fCm = ColorMatrix()
            val mix = filterIntensity.coerceIn(0f, 1f)
            val rS = 1f + (filter.rScale - 1f) * mix
            val gS = 1f + (filter.gScale - 1f) * mix
            val bS = 1f + (filter.bScale - 1f) * mix
            fCm.setScale(rS, gS, bS, 1f)
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
        return cm.array
    }

    fun toComposeColorMatrix(values: FloatArray): ComposeColorMatrix = ComposeColorMatrix(values)

    fun loadOrientedBitmap(
        context: Context,
        uri: Uri,
        maxEdge: Int
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoded = runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    if (w > 0 && h > 0 && maxOf(w, h) > maxEdge) {
                        val scale = maxEdge.toFloat() / maxOf(w, h).toFloat()
                        decoder.setTargetSize(
                            (w * scale).toInt().coerceAtLeast(1),
                            (h * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
            if (decoded != null) return decoded
        }

        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            if (maxDim > 0) {
                while (maxDim / sample > maxEdge) sample *= 2
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val orientation = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }
            if (matrix.isIdentity) decoded
            else {
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                decoded.recycle()
                rotated
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Generates live filter thumbnails for the Filters strip.
     */
    fun generateFilterThumbnails(
        source: Bitmap,
        thumbSize: Int = 112
    ): Map<PhotoFilterPreset, Bitmap> {
        val scale = min(thumbSize.toFloat() / source.width, thumbSize.toFloat() / source.height)
        val w = (source.width * scale).roundToInt().coerceAtLeast(1)
        val h = (source.height * scale).roundToInt().coerceAtLeast(1)
        val mini = Bitmap.createScaledBitmap(source, w, h, true)

        val result = LinkedHashMap<PhotoFilterPreset, Bitmap>(PhotoFilterPreset.entries.size)
        val neutral = PhotoAdjustments()
        for (preset in PhotoFilterPreset.entries) {
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val matrix = ColorMatrix(buildFallbackColorMatrixValues(neutral, preset, 1f))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(mini, 0f, 0f, paint)
            result[preset] = out
        }
        if (mini !== source) mini.recycle()
        return result
    }

    /**
     * Generates small visual preview thumbnails for each Adjust parameter (showing a clear sample boost).
     */
    fun generateAdjustThumbnails(
        source: Bitmap,
        thumbSize: Int = 88
    ): Map<AdjustParameter, Bitmap> {
        val scale = min(thumbSize.toFloat() / source.width, thumbSize.toFloat() / source.height)
        val w = (source.width * scale).roundToInt().coerceAtLeast(1)
        val h = (source.height * scale).roundToInt().coerceAtLeast(1)
        val mini = Bitmap.createScaledBitmap(source, w, h, true)

        val result = LinkedHashMap<AdjustParameter, Bitmap>(AdjustParameter.entries.size)
        for (param in AdjustParameter.entries) {
            val sampleAdj = PhotoAdjustments().set(
                param,
                when (param) {
                    AdjustParameter.HIGHLIGHTS -> -65f
                    AdjustParameter.VIGNETTE -> 75f
                    else -> 58f
                }
            )
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val matrix = ColorMatrix(buildFallbackColorMatrixValues(sampleAdj, PhotoFilterPreset.ORIGINAL, 1f))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(mini, 0f, 0f, paint)
            if (param == AdjustParameter.VIGNETTE) {
                val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f, h * 0.5f, maxOf(w, h) * 0.65f,
                        intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb(170, 0, 0, 0)),
                        floatArrayOf(0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vigPaint)
            }
            result[param] = out
        }
        if (mini !== source) mini.recycle()
        return result
    }

    /**
     * Generates live preview thumbnails for the Suggestions tab.
     */
    fun generateSuggestionThumbnails(
        source: Bitmap,
        thumbSize: Int = 104
    ): Map<PhotoSuggestionPreset, Bitmap> {
        val scale = min(thumbSize.toFloat() / source.width, thumbSize.toFloat() / source.height)
        val w = (source.width * scale).roundToInt().coerceAtLeast(1)
        val h = (source.height * scale).roundToInt().coerceAtLeast(1)
        val mini = Bitmap.createScaledBitmap(source, w, h, true)

        val result = LinkedHashMap<PhotoSuggestionPreset, Bitmap>(PhotoSuggestionPreset.entries.size)
        for (preset in PhotoSuggestionPreset.entries) {
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val matrix = ColorMatrix(
                buildFallbackColorMatrixValues(preset.adjustments, preset.filter, preset.filterIntensity)
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(mini, 0f, 0f, paint)
            result[preset] = out
        }
        if (mini !== source) mini.recycle()
        return result
    }

    /**
     * Applies Geometry (Rotate 90, Flip, Straighten) + Crop.
     */
    fun applyGeometryAndCrop(
        source: Bitmap,
        crop: PhotoCropState,
        applyCropBounds: Boolean
    ): Bitmap {
        val mat = Matrix().apply {
            if (crop.flipHorizontal) postScale(-1f, 1f)
            if (crop.rotation90 != 0) postRotate(crop.rotation90.toFloat())
        }
        val oriented = if (mat.isIdentity) source else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, mat, true)
        }

        val straightened = if (abs(crop.straightenDegrees) > 0.25f) {
            val rad = Math.toRadians(abs(crop.straightenDegrees.toDouble()))
            val zoom = (cos(rad) + sin(rad) * maxOf(
                oriented.width.toFloat() / oriented.height.toFloat(),
                oriented.height.toFloat() / oriented.width.toFloat()
            )).toFloat().coerceIn(1f, 1.75f)
            val out = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            val m = Matrix().apply {
                postTranslate(-oriented.width / 2f, -oriented.height / 2f)
                postRotate(crop.straightenDegrees)
                postScale(zoom, zoom)
                postTranslate(oriented.width / 2f, oriented.height / 2f)
            }
            c.drawBitmap(oriented, m, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            if (oriented !== source) oriented.recycle()
            out
        } else {
            oriented
        }

        if (!applyCropBounds || !crop.hasActiveBoundsCrop) {
            return straightened
        }

        val x = (crop.left * straightened.width).toInt().coerceIn(0, straightened.width - 1)
        val y = (crop.top * straightened.height).toInt().coerceIn(0, straightened.height - 1)
        val w = ((crop.right - crop.left) * straightened.width).toInt().coerceIn(1, straightened.width - x)
        val h = ((crop.bottom - crop.top) * straightened.height).toInt().coerceIn(1, straightened.height - y)
        val cropped = Bitmap.createBitmap(straightened, x, y, w, h)
        if (straightened !== source) straightened.recycle()
        return cropped
    }

    /**
     * Applies AI/Pixel Tools (Magic Eraser content-aware fill, Background Blur, Unblur/Denoise)
     * in software cleanly and fast so both preview and final export match 1:1.
     */
    fun applyPixelTools(
        input: Bitmap,
        tools: PhotoToolsState
    ): Bitmap {
        var working = input
        val needsCopy = tools.eraserStrokes.isNotEmpty() ||
            tools.backgroundBlurAmount > 0.02f ||
            tools.denoiseUnblurStrength > 0.02f
        if (!needsCopy) return input

        working = working.copy(Bitmap.Config.ARGB_8888, true)

        // 1. Magic Eraser: content-aware inward ring interpolation across brushed mask
        if (tools.eraserStrokes.isNotEmpty()) {
            applyMagicEraserInpaint(working, tools.eraserStrokes)
        }

        // 2. Unblur / Denoise: bilateral-like smoothing + crisp detail enhancement
        if (tools.denoiseUnblurStrength > 0.02f) {
            applyUnblurDenoise(working, tools.denoiseUnblurStrength)
        }

        // 3. Background Blur: radial subject-preserving depth blur
        if (tools.backgroundBlurAmount > 0.02f) {
            applyRadialBackgroundBlur(working, tools.backgroundBlurAmount)
        }

        return working
    }

    private fun applyMagicEraserInpaint(
        bitmap: Bitmap,
        strokes: List<MagicEraserStroke>
    ) {
        val w = bitmap.width
        val h = bitmap.height
        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(maskBitmap)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val baseDim = min(w, h).toFloat()
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            maskPaint.strokeWidth = (stroke.radiusFraction * baseDim * 2f).coerceAtLeast(8f)
            if (stroke.points.size == 1) {
                val p = stroke.points.first()
                maskCanvas.drawCircle(p.x * w, p.y * h, maskPaint.strokeWidth / 2f, maskPaint.apply { style = Paint.Style.FILL })
                maskPaint.style = Paint.Style.STROKE
            } else {
                val path = Path()
                path.moveTo(stroke.points.first().x * w, stroke.points.first().y * h)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x * w, stroke.points[i].y * h)
                }
                maskCanvas.drawPath(path, maskPaint)
            }
        }

        val totalPixels = w * h
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val maskBytes = IntArray(totalPixels)
        maskBitmap.getPixels(maskBytes, 0, w, 0, 0, w, h)
        maskBitmap.recycle()

        val isMasked = BooleanArray(totalPixels)
        val remaining = BooleanArray(totalPixels)
        var maskedCount = 0
        for (i in 0 until totalPixels) {
            val m = (maskBytes[i] ushr 24) > 32
            isMasked[i] = m
            remaining[i] = m
            if (m) maskedCount++
        }
        if (maskedCount == 0) return

        // Pre-allocate two buffers and a flat primitive index queue to avoid 28x copyOf() + ArrayList boxing
        val nextPixels = IntArray(totalPixels)
        val resolvedQueue = IntArray(maskedCount)
        val offsets = intArrayOf(-1, 1, -w, w, -w - 1, -w + 1, w - 1, w + 1)

        repeat(28) {
            var resolvedCount = 0
            for (y in 1 until h - 1) {
                val row = y * w
                for (x in 1 until w - 1) {
                    val idx = row + x
                    if (!remaining[idx]) continue
                    var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                    for (off in offsets) {
                        val nIdx = idx + off
                        if (!remaining[nIdx]) {
                            val c = pixels[nIdx]
                            rSum += (c shr 16) and 0xFF
                            gSum += (c shr 8) and 0xFF
                            bSum += c and 0xFF
                            count++
                        }
                    }
                    if (count > 0) {
                        val a = (pixels[idx] ushr 24) and 0xFF
                        nextPixels[idx] = (a shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
                        resolvedQueue[resolvedCount++] = idx
                    }
                }
            }
            if (resolvedCount == 0) return@repeat
            for (i in 0 until resolvedCount) {
                val idx = resolvedQueue[i]
                pixels[idx] = nextPixels[idx]
                remaining[idx] = false
            }
        }

        // Two-pass seamless smoothing over originally masked interior using pre-allocated buffer
        var readBuf = pixels
        var writeBuf = nextPixels
        readBuf.copyInto(writeBuf)
        repeat(2) {
            for (y in 1 until h - 1) {
                val row = y * w
                for (x in 1 until w - 1) {
                    val idx = row + x
                    if (!isMasked[idx]) continue
                    var r = 0; var g = 0; var b = 0
                    for (off in offsets) {
                        val c = readBuf[idx + off]
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                    }
                    val orig = readBuf[idx]
                    val a = (orig ushr 24) and 0xFF
                    writeBuf[idx] = (a shl 24) or ((r / 8) shl 16) or ((g / 8) shl 8) or (b / 8)
                }
            }
            val tmp = readBuf
            readBuf = writeBuf
            writeBuf = tmp
        }
        bitmap.setPixels(readBuf, 0, w, 0, 0, w, h)
    }

    private fun applyUnblurDenoise(bitmap: Bitmap, strength: Float) {
        val w = bitmap.width
        val h = bitmap.height
        val downW = (w / 2).coerceAtLeast(1)
        val downH = (h / 2).coerceAtLeast(1)
        val down = Bitmap.createScaledBitmap(bitmap, downW, downH, true)
        val smooth = Bitmap.createScaledBitmap(down, w, h, true)
        down.recycle()

        val origPx = IntArray(w * h)
        val smoothPx = IntArray(w * h)
        bitmap.getPixels(origPx, 0, w, 0, 0, w, h)
        smooth.getPixels(smoothPx, 0, w, 0, 0, w, h)
        smooth.recycle()

        val detailAmount = 0.55f * strength
        val denoiseBlend = 0.35f * strength
        for (i in origPx.indices) {
            val o = origPx[i]
            val s = smoothPx[i]
            val a = (o ushr 24) and 0xFF
            val or = (o shr 16) and 0xFF
            val og = (o shr 8) and 0xFF
            val ob = o and 0xFF
            val sr = (s shr 16) and 0xFF
            val sg = (s shr 8) and 0xFF
            val sb = s and 0xFF

            val diff = abs(or - sr) + abs(og - sg) + abs(ob - sb)
            val rOut: Int
            val gOut: Int
            val bOut: Int
            if (diff < 28) {
                // Flat region: smooth sensor noise
                rOut = (or * (1f - denoiseBlend) + sr * denoiseBlend).roundToInt()
                gOut = (og * (1f - denoiseBlend) + sg * denoiseBlend).roundToInt()
                bOut = (ob * (1f - denoiseBlend) + sb * denoiseBlend).roundToInt()
            } else {
                // Edge region: sharpen micro-detail (unblur)
                rOut = (or + (or - sr) * detailAmount).roundToInt().coerceIn(0, 255)
                gOut = (og + (og - sg) * detailAmount).roundToInt().coerceIn(0, 255)
                bOut = (ob + (ob - sb) * detailAmount).roundToInt().coerceIn(0, 255)
            }
            origPx[i] = (a shl 24) or (rOut shl 16) or (gOut shl 8) or bOut
        }
        bitmap.setPixels(origPx, 0, w, 0, 0, w, h)
    }

    private fun applyRadialBackgroundBlur(bitmap: Bitmap, amount: Float) {
        val w = bitmap.width
        val h = bitmap.height
        val factor = (0.14f - amount * 0.08f).coerceIn(0.04f, 0.25f)
        val small = Bitmap.createScaledBitmap(
            bitmap,
            (w * factor).roundToInt().coerceAtLeast(2),
            (h * factor).roundToInt().coerceAtLeast(2),
            true
        )
        val blurred = Bitmap.createScaledBitmap(small, w, h, true)
        small.recycle()

        // Punch crisp radial center focus window through blurred layer
        val layer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        lc.drawBitmap(blurred, 0f, 0f, null)
        blurred.recycle()

        val radius = maxOf(w, h) * 0.48f
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            shader = RadialGradient(
                w * 0.5f,
                h * 0.45f,
                radius,
                intArrayOf(android.graphics.Color.BLACK, android.graphics.Color.BLACK, android.graphics.Color.TRANSPARENT),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        lc.drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)

        val outCanvas = Canvas(bitmap)
        outCanvas.drawBitmap(layer, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (amount.coerceIn(0f, 1f) * 245).roundToInt()
        })
        layer.recycle()
    }

    /**
     * Composites markup strokes and text onto a bitmap using the same normalized coordinate system.
     */
    fun drawMarkupOntoBitmap(
        bitmap: Bitmap,
        strokes: List<MarkupStroke>,
        texts: List<MarkupTextItem>
    ) {
        if (strokes.isEmpty() && texts.isEmpty()) return
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val baseDim = min(w, h)

        // Separate offscreen layer for strokes so eraser erases only markup strokes, not photo pixels!
        val strokeLayer = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val strokeCanvas = Canvas(strokeLayer)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            paint.strokeWidth = (stroke.widthFraction * baseDim).coerceAtLeast(3f)
            if (stroke.isEraser) {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                paint.color = android.graphics.Color.TRANSPARENT
                paint.alpha = 255
            } else {
                paint.xfermode = null
                paint.color = stroke.colorArgb.toInt()
                paint.alpha = (stroke.alpha * 255).roundToInt().coerceIn(0, 255)
            }

            if (stroke.points.size == 1) {
                val p = stroke.points.first()
                strokeCanvas.drawPoint(p.x * w, p.y * h, paint)
            } else {
                val path = Path()
                path.moveTo(stroke.points.first().x * w, stroke.points.first().y * h)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x * w, stroke.points[i].y * h)
                }
                strokeCanvas.drawPath(path, paint)
            }
        }

        val mainCanvas = Canvas(bitmap)
        mainCanvas.drawBitmap(strokeLayer, 0f, 0f, null)
        strokeLayer.recycle()

        // Text items
        val densityScale = (baseDim / 400f).coerceAtLeast(0.8f)
        for (item in texts) {
            if (item.text.isBlank()) continue
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorArgb.toInt()
                textSize = item.sizeSp * densityScale
                textAlign = Paint.Align.CENTER
                typeface = when (item.fontStyle) {
                    MarkupFontStyle.SANS -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    MarkupFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    MarkupFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    MarkupFontStyle.BOLD -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                }
                setShadowLayer(4f * densityScale, 0f, 2f * densityScale, android.graphics.Color.argb(140, 0, 0, 0))
            }
            val fm = textPaint.fontMetrics
            val baselineOffset = -(fm.ascent + fm.descent) / 2f
            mainCanvas.drawText(item.text, item.normX * w, item.normY * h + baselineOffset, textPaint)
        }
    }

    /**
     * Full non-destructive high-res render used on Save (Copy or Overwrite Original).
     */
    fun renderFinalBitmap(
        source: Bitmap,
        state: PhotoEditState
    ): Bitmap {
        val withTools = applyPixelTools(source, state.tools)
        val cropped = applyGeometryAndCrop(withTools, state.crop, applyCropBounds = true)
        if (withTools !== source && withTools !== cropped) withTools.recycle()

        val w = cropped.width
        val h = cropped.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Apply adjustments + filter
        val adj = state.adjustments
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(buildFallbackColorMatrixValues(adj, state.filter, state.filterIntensity))
            )
        }
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        if (cropped !== source) cropped.recycle()

        // Pixel-level highlights/shadows tone curve matching AGSL smoothstep masks
        if (abs(adj.highlights) > 1f || abs(adj.shadows) > 1f) {
            applyHighlightsShadowsToneCurve(
                output,
                highlights = adj.highlights / 100f,
                shadows = adj.shadows / 100f
            )
        }

        // Pixel-level sharpen enhancement for parity with GPU preview
        if (abs(adj.sharpen) > 2f) {
            applyUnblurDenoise(output, (adj.sharpen / 100f).coerceIn(0f, 1f))
        }

        // Radial vignette overlay
        if (abs(adj.vignette) > 1f) {
            val strength = (adj.vignette / 100f).coerceIn(-1f, 1f)
            val vigRadius = sqrt((w * w + h * h).toDouble()).toFloat() * 0.56f
            val vigColor = if (strength >= 0f) {
                android.graphics.Color.argb((strength * 185).roundToInt(), 0, 0, 0)
            } else {
                android.graphics.Color.argb((-strength * 150).roundToInt(), 255, 255, 255)
            }
            val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    w * 0.5f,
                    h * 0.5f,
                    vigRadius,
                    intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT, vigColor),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vigPaint)
        }

        // Markup strokes & text
        drawMarkupOntoBitmap(output, state.markupStrokes, state.markupTexts)
        return output
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun applyHighlightsShadowsToneCurve(
        bitmap: Bitmap,
        highlights: Float,
        shadows: Float
    ) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val hiShift = highlights * 0.32f * 255f
        val shShift = shadows * 0.32f * 255f

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
            val hiMask = smoothStep(0.48f, 0.98f, lum)
            val shMask = 1f - smoothStep(0.02f, 0.52f, lum)
            val delta = (hiShift * hiMask + shShift * shMask).roundToInt()

            if (delta != 0) {
                val nr = (r + delta).coerceIn(0, 255)
                val ng = (g + delta).coerceIn(0, 255)
                val nb = (b + delta).coerceIn(0, 255)
                pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}
