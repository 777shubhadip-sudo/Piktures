package com.example.ui.editor.photo

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class PhotoEditorTab(val label: String) {
    SUGGESTIONS("Suggestions"),
    CROP("Crop"),
    ADJUST("Adjust"),
    FILTERS("Filters"),
    MARKUP("Markup"),
    TOOLS("Tools")
}

enum class PhotoAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    NINE_SIXTEEN("9:16", 9f / 16f)
}

enum class AdjustParameter(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    HIGHLIGHTS("Highlights"),
    SHADOWS("Shadows"),
    SATURATION("Saturation"),
    WARMTH("Warmth"),
    TINT("Tint"),
    SHARPEN("Sharpen"),
    VIGNETTE("Vignette")
}

data class PhotoAdjustments(
    val brightness: Float = 0f,   // -100..100
    val contrast: Float = 0f,     // -100..100
    val highlights: Float = 0f,   // -100..100
    val shadows: Float = 0f,      // -100..100
    val saturation: Float = 0f,   // -100..100
    val warmth: Float = 0f,       // -100..100
    val tint: Float = 0f,         // -100..100
    val sharpen: Float = 0f,      // -100..100
    val vignette: Float = 0f      // -100..100
) {
    fun get(param: AdjustParameter): Float = when (param) {
        AdjustParameter.BRIGHTNESS -> brightness
        AdjustParameter.CONTRAST -> contrast
        AdjustParameter.HIGHLIGHTS -> highlights
        AdjustParameter.SHADOWS -> shadows
        AdjustParameter.SATURATION -> saturation
        AdjustParameter.WARMTH -> warmth
        AdjustParameter.TINT -> tint
        AdjustParameter.SHARPEN -> sharpen
        AdjustParameter.VIGNETTE -> vignette
    }

    fun set(param: AdjustParameter, value: Float): PhotoAdjustments {
        val clamped = value.coerceIn(-100f, 100f)
        return when (param) {
            AdjustParameter.BRIGHTNESS -> copy(brightness = clamped)
            AdjustParameter.CONTRAST -> copy(contrast = clamped)
            AdjustParameter.HIGHLIGHTS -> copy(highlights = clamped)
            AdjustParameter.SHADOWS -> copy(shadows = clamped)
            AdjustParameter.SATURATION -> copy(saturation = clamped)
            AdjustParameter.WARMTH -> copy(warmth = clamped)
            AdjustParameter.TINT -> copy(tint = clamped)
            AdjustParameter.SHARPEN -> copy(sharpen = clamped)
            AdjustParameter.VIGNETTE -> copy(vignette = clamped)
        }
    }

    val isDefault: Boolean
        get() = brightness == 0f && contrast == 0f && highlights == 0f && shadows == 0f &&
            saturation == 0f && warmth == 0f && tint == 0f && sharpen == 0f && vignette == 0f
}

enum class PhotoFilterPreset(
    val label: String,
    val rScale: Float,
    val gScale: Float,
    val bScale: Float,
    val satBoost: Float,
    val contrastBoost: Float,
    val monochrome: Boolean = false
) {
    ORIGINAL("Original", 1f, 1f, 1f, 0f, 0f, false),
    VIVID("Vivid", 1.04f, 1.02f, 1.0f, 0.36f, 0.12f, false),
    GOLDEN("Golden", 1.16f, 1.04f, 0.82f, 0.18f, 0.08f, false),
    FILM("Film", 1.08f, 1.01f, 0.90f, -0.18f, 0.16f, false),
    NOIR("Noir", 1f, 1f, 1f, -1f, 0.28f, true),
    FROST("Frost", 0.88f, 1.02f, 1.18f, -0.08f, 0.06f, false),
    CINEMA("Cinema", 0.96f, 1.05f, 1.12f, 0.10f, 0.22f, false),
    EMERALD("Emerald", 0.90f, 1.14f, 1.02f, 0.12f, 0.05f, false),
    ROSE("Rosewood", 1.14f, 0.95f, 1.06f, 0.10f, 0.10f, false)
}

enum class PhotoSuggestionPreset(
    val title: String,
    val subtitle: String,
    val adjustments: PhotoAdjustments,
    val filter: PhotoFilterPreset,
    val filterIntensity: Float,
    val autoEnhanced: Boolean = false
) {
    NONE(
        title = "Original",
        subtitle = "Reset look",
        adjustments = PhotoAdjustments(),
        filter = PhotoFilterPreset.ORIGINAL,
        filterIntensity = 1f
    ),
    AUTO_ENHANCE(
        title = "Enhance",
        subtitle = "Balanced AI tone",
        adjustments = PhotoAdjustments(
            brightness = 8f,
            contrast = 18f,
            highlights = -18f,
            shadows = 22f,
            saturation = 14f,
            sharpen = 16f
        ),
        filter = PhotoFilterPreset.VIVID,
        filterIntensity = 0.45f,
        autoEnhanced = true
    ),
    VIVID_POP(
        title = "Dynamic",
        subtitle = "Rich color & punch",
        adjustments = PhotoAdjustments(
            contrast = 26f,
            saturation = 28f,
            shadows = 14f,
            sharpen = 22f,
            vignette = 16f
        ),
        filter = PhotoFilterPreset.VIVID,
        filterIntensity = 0.75f
    ),
    GOLDEN_HOUR(
        title = "Golden",
        subtitle = "Warm sunset glow",
        adjustments = PhotoAdjustments(
            brightness = 6f,
            warmth = 34f,
            shadows = 16f,
            highlights = -14f,
            saturation = 12f
        ),
        filter = PhotoFilterPreset.GOLDEN,
        filterIntensity = 0.7f
    ),
    PORTRAIT_SOFT(
        title = "Portrait",
        subtitle = "Soft highlights",
        adjustments = PhotoAdjustments(
            brightness = 10f,
            highlights = -24f,
            shadows = 26f,
            warmth = 10f,
            vignette = 24f
        ),
        filter = PhotoFilterPreset.ROSE,
        filterIntensity = 0.35f
    ),
    CRISP_MONO(
        title = "B&W Crisp",
        subtitle = "Editorial monochrome",
        adjustments = PhotoAdjustments(
            contrast = 32f,
            highlights = -12f,
            shadows = 10f,
            sharpen = 24f,
            vignette = 28f
        ),
        filter = PhotoFilterPreset.NOIR,
        filterIntensity = 1f
    )
}

enum class MarkupTool(val label: String) {
    PEN("Pen"),
    HIGHLIGHTER("Highlighter"),
    TEXT("Text"),
    ERASER("Eraser")
}

enum class MarkupFontStyle(val label: String) {
    SANS("Clean"),
    SERIF("Classic"),
    MONO("Mono"),
    BOLD("Impact")
}

data class NormalizedPoint(val x: Float, val y: Float)

data class MarkupStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NormalizedPoint>,
    val colorArgb: Long,
    val widthFraction: Float,
    val alpha: Float,
    val isEraser: Boolean = false
)

data class MarkupTextItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val normX: Float = 0.5f,
    val normY: Float = 0.5f,
    val colorArgb: Long = 0xFFFFFFFF,
    val sizeSp: Float = 26f,
    val fontStyle: MarkupFontStyle = MarkupFontStyle.SANS
)

data class MagicEraserStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NormalizedPoint>,
    val radiusFraction: Float = 0.045f
)

data class PhotoCropState(
    val aspectRatio: PhotoAspectRatio = PhotoAspectRatio.FREE,
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
    val rotation90: Int = 0,
    val flipHorizontal: Boolean = false,
    val straightenDegrees: Float = 0f
) {
    val hasActiveBoundsCrop: Boolean
        get() = left > 0.002f || top > 0.002f || right < 0.998f || bottom < 0.998f
}

data class PhotoToolsState(
    val autoEnhanced: Boolean = false,
    val backgroundBlurAmount: Float = 0f,    // 0f..1f
    val denoiseUnblurStrength: Float = 0f,   // 0f..1f
    val eraserStrokes: List<MagicEraserStroke> = emptyList()
)

data class PhotoEditState(
    val crop: PhotoCropState = PhotoCropState(),
    val adjustments: PhotoAdjustments = PhotoAdjustments(),
    val filter: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
    val filterIntensity: Float = 1f,
    val activeSuggestion: PhotoSuggestionPreset = PhotoSuggestionPreset.NONE,
    val markupStrokes: List<MarkupStroke> = emptyList(),
    val markupTexts: List<MarkupTextItem> = emptyList(),
    val tools: PhotoToolsState = PhotoToolsState()
) {
    val hasChanges: Boolean
        get() = crop != PhotoCropState() ||
            !adjustments.isDefault ||
            filter != PhotoFilterPreset.ORIGINAL ||
            activeSuggestion != PhotoSuggestionPreset.NONE ||
            markupStrokes.isNotEmpty() ||
            markupTexts.isNotEmpty() ||
            tools != PhotoToolsState()
}

val MARKUP_PALETTE_COLORS: List<Long> = listOf(
    0xFFFFFFFF,
    0xFF0A0A0F,
    0xFFEF4444,
    0xFFF59E0B,
    0xFF10B981,
    0xFF38BDF8,
    0xFF6366F1,
    0xFFEC4899
)

fun Long.toComposeColor(): Color = Color(this.toInt())
