package com.example.domain.model

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class RelockTimeout(val label: String, val millis: Long) {
    IMMEDIATE("Immediate", 0L),
    THIRTY_SECONDS("30 seconds", 30_000L),
    ONE_MINUTE("1 minute", 60_000L),
    FIVE_MINUTES("5 minutes", 300_000L)
}

enum class MediaPermissionState {
    FULL_ACCESS,
    PARTIAL_ACCESS,
    DENIED
}

enum class GalleryTab(val label: String) {
    PHOTOS("Photos"),
    ALBUMS("Albums"),
    FAVORITES("Favorites")
}

enum class UiColorPalette(val label: String, val hexCode: String) {
    MIDNIGHT_NAVY("Midnight Navy", "#0D1E4C"),
    ORCHID_MAUVE("Orchid Mauve", "#C48CB3"),
    BLUSH_MIST("Blush Mist", "#E5C9D7"),
    SERENITY_BLUE("Serenity Blue", "#83A6CE"),
    DUSK_SLATE("Dusk Slate", "#26415E"),
    ABYSSAL_NAVY("Abyssal Navy", "#0B1B32")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val uiColorPalette: UiColorPalette = UiColorPalette.ORCHID_MAUVE,
    val gridColumns: Int = 3,
    val trashRetentionDays: Int = 30,
    val relockTimeout: RelockTimeout = RelockTimeout.IMMEDIATE,
    val reduceTransparency: Boolean = false
)
