package com.example.ui.editor.video

import com.example.ui.editor.photo.MarkupFontStyle
import com.example.ui.editor.photo.PhotoFilterPreset
import java.util.UUID

enum class VideoEditorTab(val label: String) {
    TRIM("Trim"),
    SPEED("Speed"),
    FILTERS("Filters"),
    TEXT("Text"),
    MUSIC("Music"),
    AUDIO("Audio")
}

enum class VideoAspectRatio(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_1_1("1:1", 1f)
}

enum class VideoTransitionStyle(val label: String) {
    NONE("Cut"),
    CROSSFADE("Dissolve"),
    SLIDE("Slide"),
    FLASH("Flash")
}

enum class VideoExportResolution(val label: String, val targetHeight: Int, val bitrateHint: String) {
    HD_720P("720p HD", 720, "~8 Mbps"),
    FHD_1080P("1080p Full HD", 1080, "~16 Mbps"),
    UHD_4K("4K Ultra HD", 2160, "~45 Mbps")
}

data class VideoColorAdjustments(
    val filter: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
    val filterIntensity: Float = 1f,
    val brightness: Float = 0f,   // -100..100
    val contrast: Float = 0f,     // -100..100
    val saturation: Float = 0f,   // -100..100
    val warmth: Float = 0f        // -100..100
)

data class VideoClipItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs.coerceAtLeast(500L),
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val speed: Float = 1f,
    val stabilize: Boolean = false,
    val transitionAfter: VideoTransitionStyle = VideoTransitionStyle.NONE
) {
    val trimmedSourceDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(250L)

    val effectiveDurationMs: Long
        get() = (trimmedSourceDurationMs.toFloat() / speed.coerceIn(0.25f, 4f)).toLong().coerceAtLeast(200L)
}

data class VideoTextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fontStyle: MarkupFontStyle = MarkupFontStyle.SANS,
    val colorArgb: Long = 0xFFFFFFFF,
    val sizeSp: Float = 26f,
    val normX: Float = 0.5f,
    val normY: Float = 0.78f,
    val startMs: Long = 0L,
    val endMs: Long = 4000L
)

data class VideoAudioTrack(
    val id: String,
    val title: String,
    val uriString: String,
    val isBuiltIn: Boolean,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs.coerceAtLeast(1000L)
)

enum class VideoTemplatePreset(
    val title: String,
    val subtitle: String,
    val trackId: String,
    val transition: VideoTransitionStyle,
    val color: VideoColorAdjustments,
    val textLabel: String,
    val textFont: MarkupFontStyle,
    val textColorArgb: Long
) {
    CINEMATIC_TRAVEL(
        title = "Cinematic",
        subtitle = "Cinema LUT + Horizon",
        trackId = "builtin_horizon",
        transition = VideoTransitionStyle.CROSSFADE,
        color = VideoColorAdjustments(
            filter = PhotoFilterPreset.CINEMA,
            filterIntensity = 0.8f,
            contrast = 16f,
            saturation = 10f
        ),
        textLabel = "JOURNEY",
        textFont = MarkupFontStyle.SERIF,
        textColorArgb = 0xFFF8FAFC
    ),
    UPBEAT_REEL(
        title = "Upbeat Reel",
        subtitle = "Vivid + Flash + Drive",
        trackId = "builtin_drive",
        transition = VideoTransitionStyle.FLASH,
        color = VideoColorAdjustments(
            filter = PhotoFilterPreset.VIVID,
            filterIntensity = 0.85f,
            contrast = 20f,
            saturation = 24f
        ),
        textLabel = "HIGHLIGHTS",
        textFont = MarkupFontStyle.BOLD,
        textColorArgb = 0xFFF59E0B
    ),
    LOFI_MEMORY(
        title = "Lo-Fi Memory",
        subtitle = "Film + Warmth + Pulse",
        trackId = "builtin_lofi",
        transition = VideoTransitionStyle.CROSSFADE,
        color = VideoColorAdjustments(
            filter = PhotoFilterPreset.FILM,
            filterIntensity = 0.85f,
            warmth = 24f,
            contrast = -8f
        ),
        textLabel = "golden days",
        textFont = MarkupFontStyle.MONO,
        textColorArgb = 0xFFFDE68A
    ),
    GOLDEN_VLOG(
        title = "Golden Hour",
        subtitle = "Sunset + Acoustic",
        trackId = "builtin_acoustic",
        transition = VideoTransitionStyle.SLIDE,
        color = VideoColorAdjustments(
            filter = PhotoFilterPreset.GOLDEN,
            filterIntensity = 0.75f,
            brightness = 6f,
            warmth = 28f
        ),
        textLabel = "Captured Moments",
        textFont = MarkupFontStyle.SANS,
        textColorArgb = 0xFFFFFFFF
    )
}

data class VideoEditState(
    val clips: List<VideoClipItem> = emptyList(),
    val selectedClipId: String? = null,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.ORIGINAL,
    val colorAdjustments: VideoColorAdjustments = VideoColorAdjustments(),
    val textOverlays: List<VideoTextOverlay> = emptyList(),
    val selectedMusic: VideoAudioTrack? = null,
    val originalVolume: Float = 1f,
    val musicVolume: Float = 0.7f
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }.coerceAtLeast(500L)

    val selectedClip: VideoClipItem?
        get() = clips.firstOrNull { it.id == selectedClipId } ?: clips.firstOrNull()
}
