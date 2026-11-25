package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.ThemeMode
import com.example.domain.model.UiColorPalette

@Immutable
data class LuminaGlassColors(
    val isDark: Boolean,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassStrongFill: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val meshBase: Color,
    val meshOrb1: Color,
    val meshOrb2: Color,
    val meshOrb3: Color,
    val patternOverlay: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val accentGlow: Color,
    val meshOrb4: Color = Color.Transparent,
    val meshOrb5: Color = Color.Transparent,
    val glassHighlightTop: Color = Color.White.copy(alpha = 0.56f),
    val glassHighlightBottom: Color = Color.White.copy(alpha = 0.16f),
    val glassTopEdgeHighlight: Color = Color.White.copy(alpha = 0.85f),
    val glassBottomEdgeHighlight: Color = Color.White.copy(alpha = 0.22f),
    val glassSecondaryText: Color = NuviqInkMuted,
    val prismBorderStart: Color = Color.White.copy(alpha = 0.90f),
    val prismBorderMid: Color = PrismDispersionCyan.copy(alpha = 0.45f),
    val prismBorderEnd: Color = PrismDispersionViolet.copy(alpha = 0.35f),
    val editorialGold: Color = NuviqOlivePrimary,
    val solidFallbackSurface: Color = Color.White,
    val solidFallbackElevated: Color = Color(0xFFF1F5F9),
    val dispersionCool: Color = PrismDispersionCyan,
    val dispersionWarm: Color = PrismDispersionViolet
)

val LocalLuminaColors = staticCompositionLocalOf {
    LuminaGlassColors(
        isDark = false,
        glassFillTop = Color.White.copy(alpha = 0.64f),
        glassFillBottom = Color(0xFFF6FAFF),
        glassStrongFill = Color(0xCCFFFFFF),
        glassBorder = Color.White.copy(alpha = 0.72f),
        glassHighlight = NuviqOlivePrimary.copy(alpha = 0.18f),
        meshBase = NuviqCreamBg,
        meshOrb1 = NuviqLimeWash.copy(alpha = 0.78f),
        meshOrb2 = NuviqSageTint.copy(alpha = 0.65f),
        meshOrb3 = Color(0xFFE0E7FF).copy(alpha = 0.78f),
        meshOrb4 = Color(0xFFFCE7F3).copy(alpha = 0.52f),
        meshOrb5 = NuviqOliveLight.copy(alpha = 0.24f),
        patternOverlay = Color.Transparent,
        shimmerBase = Color(0xFFDCE7F5),
        shimmerHighlight = Color(0xFFEEF5FD),
        accentGlow = NuviqOlivePrimary,
        glassSecondaryText = NuviqInkMuted,
        prismBorderStart = Color.White,
        prismBorderMid = PrismDispersionCyan.copy(alpha = 0.50f),
        prismBorderEnd = PrismDispersionViolet.copy(alpha = 0.35f),
        editorialGold = NuviqOlivePrimary,
        solidFallbackSurface = Color.White,
        solidFallbackElevated = Color(0xFFF1F5F9),
        dispersionCool = PrismDispersionCyan,
        dispersionWarm = PrismDispersionViolet
    )
}

enum class VaultAtmosphere {
    GALLERY,
    VAULT_LEVEL_1,
    VAULT_LEVEL_2
}

private val LuminaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private data class GalleryPaletteSpec(
    val lightPrimary: Color,
    val onLightPrimary: Color,
    val lightSecondary: Color,
    val lightTextAccent: Color,
    val lightBg: Color,
    val lightGlassBottom: Color,
    val lightOrb1: Color,
    val lightOrb2: Color,
    val lightOrb3: Color,
    val lightOrb4: Color,
    val lightDispersionCool: Color,
    val lightDispersionWarm: Color,
    val darkPrimary: Color,
    val onDarkPrimary: Color,
    val darkSecondary: Color,
    val darkTextAccent: Color,
    val darkBg: Color,
    val darkSurface: Color,
    val darkGlassBottom: Color,
    val darkGlassStrong: Color,
    val darkOrb1: Color,
    val darkOrb2: Color,
    val darkOrb3: Color,
    val darkOrb4: Color,
    val darkOrb5: Color,
    val darkDispersionCool: Color,
    val darkDispersionWarm: Color
)

private fun galleryPaletteSpec(palette: UiColorPalette): GalleryPaletteSpec = when (palette) {
    UiColorPalette.MIDNIGHT_NAVY -> GalleryPaletteSpec(
        lightPrimary = MidnightNavy,
        onLightPrimary = Color.White,
        lightSecondary = SerenityBlue,
        lightTextAccent = MidnightNavy,
        lightBg = Color(0xFFEEF2FA),
        lightGlassBottom = Color(0xFFF3F6FC),
        lightOrb1 = SerenityBlue.copy(alpha = 0.56f),
        lightOrb2 = MidnightNavy.copy(alpha = 0.22f),
        lightOrb3 = BlushMist.copy(alpha = 0.52f),
        lightOrb4 = DuskSlateNavy.copy(alpha = 0.20f),
        lightDispersionCool = SerenityBlue,
        lightDispersionWarm = MidnightNavy,
        darkPrimary = Color(0xFF5D84DF),
        onDarkPrimary = Color.White,
        darkSecondary = SerenityBlue,
        darkTextAccent = Color(0xFFA9C5F7),
        darkBg = MidnightNavy,
        darkSurface = Color(0xFF13275E),
        darkGlassBottom = Color(0xFF152B66),
        darkGlassStrong = Color(0xCC162D6B),
        darkOrb1 = Color(0xFF213F92).copy(alpha = 0.90f),
        darkOrb2 = SerenityBlue.copy(alpha = 0.34f),
        darkOrb3 = AbyssalNavy.copy(alpha = 0.92f),
        darkOrb4 = Color(0xFF7FA6F2).copy(alpha = 0.26f),
        darkOrb5 = BlushMist.copy(alpha = 0.14f),
        darkDispersionCool = SerenityBlue,
        darkDispersionWarm = Color(0xFF7FA6F2)
    )
    UiColorPalette.ORCHID_MAUVE -> GalleryPaletteSpec(
        lightPrimary = OrchidPrimary,
        onLightPrimary = MidnightNavy,
        lightSecondary = SerenityBlue,
        lightTextAccent = OrchidDeepInk,
        lightBg = NuviqCreamBg,
        lightGlassBottom = Color(0xFFF5EEF4),
        lightOrb1 = SerenityBlue.copy(alpha = 0.48f),
        lightOrb2 = BlushMist.copy(alpha = 0.86f),
        lightOrb3 = OrchidPrimary.copy(alpha = 0.42f),
        lightOrb4 = DuskSlateNavy.copy(alpha = 0.18f),
        lightDispersionCool = SerenityBlue,
        lightDispersionWarm = OrchidPrimary,
        darkPrimary = OrchidPrimary,
        onDarkPrimary = MidnightNavy,
        darkSecondary = SerenityBlue,
        darkTextAccent = OrchidPrimary,
        darkBg = AbyssalNavy,
        darkSurface = IndigoSurface,
        darkGlassBottom = Color(0xFF142845),
        darkGlassStrong = Color(0xCC162B49),
        darkOrb1 = DuskSlateNavy.copy(alpha = 0.92f),
        darkOrb2 = OrchidPrimary.copy(alpha = 0.34f),
        darkOrb3 = MidnightNavy.copy(alpha = 0.88f),
        darkOrb4 = SerenityBlue.copy(alpha = 0.26f),
        darkOrb5 = BlushMist.copy(alpha = 0.18f),
        darkDispersionCool = SerenityBlue,
        darkDispersionWarm = OrchidPrimary
    )
    UiColorPalette.BLUSH_MIST -> GalleryPaletteSpec(
        lightPrimary = BlushMist,
        onLightPrimary = AbyssalNavy,
        lightSecondary = OrchidPrimary,
        lightTextAccent = Color(0xFF844D6B),
        lightBg = Color(0xFFF7EFF4),
        lightGlassBottom = Color(0xFFFAF3F7),
        lightOrb1 = BlushMist.copy(alpha = 0.92f),
        lightOrb2 = OrchidPrimary.copy(alpha = 0.38f),
        lightOrb3 = SerenityBlue.copy(alpha = 0.34f),
        lightOrb4 = BlushMist.copy(alpha = 0.72f),
        lightDispersionCool = SerenityBlue,
        lightDispersionWarm = OrchidPrimary,
        darkPrimary = BlushMist,
        onDarkPrimary = AbyssalNavy,
        darkSecondary = OrchidPrimary,
        darkTextAccent = BlushMist,
        darkBg = Color(0xFF140E21),
        darkSurface = Color(0xFF211734),
        darkGlassBottom = Color(0xFF231938),
        darkGlassStrong = Color(0xCC261B3D),
        darkOrb1 = Color(0xFF3E244E).copy(alpha = 0.86f),
        darkOrb2 = BlushMist.copy(alpha = 0.26f),
        darkOrb3 = MidnightNavy.copy(alpha = 0.76f),
        darkOrb4 = OrchidPrimary.copy(alpha = 0.30f),
        darkOrb5 = SerenityBlue.copy(alpha = 0.18f),
        darkDispersionCool = OrchidPrimary,
        darkDispersionWarm = BlushMist
    )
    UiColorPalette.SERENITY_BLUE -> GalleryPaletteSpec(
        lightPrimary = SerenityBlue,
        onLightPrimary = AbyssalNavy,
        lightSecondary = DuskSlateNavy,
        lightTextAccent = Color(0xFF2B527F),
        lightBg = Color(0xFFEDF3FA),
        lightGlassBottom = Color(0xFFF3F8FD),
        lightOrb1 = SerenityBlue.copy(alpha = 0.64f),
        lightOrb2 = BlushMist.copy(alpha = 0.56f),
        lightOrb3 = DuskSlateNavy.copy(alpha = 0.22f),
        lightOrb4 = SerenityBlue.copy(alpha = 0.42f),
        lightDispersionCool = SerenityBlue,
        lightDispersionWarm = DuskSlateNavy,
        darkPrimary = SerenityBlue,
        onDarkPrimary = AbyssalNavy,
        darkSecondary = BlushMist,
        darkTextAccent = SerenityBlue,
        darkBg = Color(0xFF0A1A30),
        darkSurface = Color(0xFF132A48),
        darkGlassBottom = Color(0xFF142D4E),
        darkGlassStrong = Color(0xCC163256),
        darkOrb1 = DuskSlateNavy.copy(alpha = 0.94f),
        darkOrb2 = SerenityBlue.copy(alpha = 0.38f),
        darkOrb3 = MidnightNavy.copy(alpha = 0.86f),
        darkOrb4 = BlushMist.copy(alpha = 0.18f),
        darkOrb5 = SerenityBlue.copy(alpha = 0.24f),
        darkDispersionCool = SerenityBlue,
        darkDispersionWarm = BlushMist
    )
    UiColorPalette.DUSK_SLATE -> GalleryPaletteSpec(
        lightPrimary = DuskSlateNavy,
        onLightPrimary = Color.White,
        lightSecondary = SerenityBlue,
        lightTextAccent = DuskSlateNavy,
        lightBg = Color(0xFFECF1F7),
        lightGlassBottom = Color(0xFFF3F6FA),
        lightOrb1 = DuskSlateNavy.copy(alpha = 0.28f),
        lightOrb2 = SerenityBlue.copy(alpha = 0.56f),
        lightOrb3 = BlushMist.copy(alpha = 0.48f),
        lightOrb4 = MidnightNavy.copy(alpha = 0.18f),
        lightDispersionCool = SerenityBlue,
        lightDispersionWarm = DuskSlateNavy,
        darkPrimary = Color(0xFF6E95BD),
        onDarkPrimary = AbyssalNavy,
        darkSecondary = SerenityBlue,
        darkTextAccent = Color(0xFFA7C7E8),
        darkBg = Color(0xFF142436),
        darkSurface = DuskSlateNavy,
        darkGlassBottom = Color(0xFF1E354E),
        darkGlassStrong = Color(0xCC223B56),
        darkOrb1 = DuskSlateNavy.copy(alpha = 0.96f),
        darkOrb2 = SerenityBlue.copy(alpha = 0.34f),
        darkOrb3 = AbyssalNavy.copy(alpha = 0.90f),
        darkOrb4 = BlushMist.copy(alpha = 0.18f),
        darkOrb5 = Color(0xFF6E95BD).copy(alpha = 0.22f),
        darkDispersionCool = SerenityBlue,
        darkDispersionWarm = Color(0xFFA7C7E8)
    )
    UiColorPalette.ABYSSAL_NAVY -> GalleryPaletteSpec(
        lightPrimary = AbyssalNavy,
        onLightPrimary = Color.White,
        lightSecondary = DuskSlateNavy,
        lightTextAccent = AbyssalNavy,
        lightBg = Color(0xFFEAEEF5),
        lightGlassBottom = Color(0xFFF2F5FA),
        lightOrb1 = DuskSlateNavy.copy(alpha = 0.30f),
        lightOrb2 = SerenityBlue.copy(alpha = 0.48f),
        lightOrb3 = AbyssalNavy.copy(alpha = 0.18f),
        lightOrb4 = BlushMist.copy(alpha = 0.44f),
        lightDispersionCool = SerenityBlue,
        lightDispersionWarm = AbyssalNavy,
        darkPrimary = Color(0xFF7CA3D1),
        onDarkPrimary = AbyssalNavy,
        darkSecondary = DuskSlateNavy,
        darkTextAccent = Color(0xFFB3D0F2),
        darkBg = Color(0xFF071122),
        darkSurface = AbyssalNavy,
        darkGlassBottom = Color(0xFF0E203C),
        darkGlassStrong = Color(0xCC112645),
        darkOrb1 = MidnightNavy.copy(alpha = 0.92f),
        darkOrb2 = DuskSlateNavy.copy(alpha = 0.72f),
        darkOrb3 = AbyssalNavy.copy(alpha = 0.96f),
        darkOrb4 = SerenityBlue.copy(alpha = 0.26f),
        darkOrb5 = BlushMist.copy(alpha = 0.14f),
        darkDispersionCool = SerenityBlue,
        darkDispersionWarm = Color(0xFF7CA3D1)
    )
}

@Composable
fun LuminaTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    uiColorPalette: UiColorPalette = UiColorPalette.ORCHID_MAUVE,
    atmosphere: VaultAtmosphere = VaultAtmosphere.GALLERY,
    dynamicAccent: Color? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val effectiveDark = if (atmosphere != VaultAtmosphere.GALLERY) true else isDark
    val paletteSpec = galleryPaletteSpec(uiColorPalette)

    val primaryAccent = when (atmosphere) {
        VaultAtmosphere.GALLERY -> dynamicAccent ?: if (effectiveDark) paletteSpec.darkPrimary else paletteSpec.lightPrimary
        VaultAtmosphere.VAULT_LEVEL_1 -> VaultL1Accent
        VaultAtmosphere.VAULT_LEVEL_2 -> VaultL2Accent
    }

    val onPrimaryAccent = when (atmosphere) {
        VaultAtmosphere.GALLERY -> if (effectiveDark) paletteSpec.onDarkPrimary else paletteSpec.onLightPrimary
        VaultAtmosphere.VAULT_LEVEL_1 -> AbyssalNavy
        VaultAtmosphere.VAULT_LEVEL_2 -> Color.White
    }

    val secondaryAccent = when (atmosphere) {
        VaultAtmosphere.GALLERY -> if (effectiveDark) paletteSpec.darkSecondary else paletteSpec.lightSecondary
        VaultAtmosphere.VAULT_LEVEL_1 -> TealAurora
        VaultAtmosphere.VAULT_LEVEL_2 -> VaultL2Secondary
    }

    val tertiaryAccent = when (atmosphere) {
        VaultAtmosphere.GALLERY -> if (effectiveDark) TealAurora else paletteSpec.lightDispersionWarm
        VaultAtmosphere.VAULT_LEVEL_1 -> VaultL1Glow
        VaultAtmosphere.VAULT_LEVEL_2 -> VaultL2Accent
    }

    val colorScheme = if (effectiveDark) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = onPrimaryAccent,
            primaryContainer = primaryAccent.copy(alpha = 0.24f),
            onPrimaryContainer = Color.White,
            secondary = secondaryAccent,
            onSecondary = AbyssalNavy,
            secondaryContainer = secondaryAccent.copy(alpha = 0.24f),
            onSecondaryContainer = TextPrimaryDark,
            tertiary = tertiaryAccent,
            onTertiary = AbyssalNavy,
            background = when (atmosphere) {
                VaultAtmosphere.VAULT_LEVEL_2 -> Color(0xFF12060C)
                VaultAtmosphere.VAULT_LEVEL_1 -> Color(0xFF040E0B)
                else -> paletteSpec.darkBg
            },
            onBackground = TextPrimaryDark,
            surface = when (atmosphere) {
                VaultAtmosphere.VAULT_LEVEL_2 -> Color(0xFF1D0B14)
                VaultAtmosphere.VAULT_LEVEL_1 -> Color(0xFF0A1A15)
                else -> paletteSpec.darkSurface
            },
            onSurface = TextPrimaryDark,
            surfaceVariant = DuskSlateNavy,
            onSurfaceVariant = TextSecondaryDark,
            outline = Color.White.copy(alpha = 0.28f),
            error = DangerRose,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = onPrimaryAccent,
            primaryContainer = primaryAccent.copy(alpha = 0.22f),
            onPrimaryContainer = AbyssalNavy,
            secondary = secondaryAccent,
            onSecondary = AbyssalNavy,
            secondaryContainer = secondaryAccent.copy(alpha = 0.26f),
            onSecondaryContainer = AbyssalNavy,
            tertiary = tertiaryAccent,
            onTertiary = AbyssalNavy,
            background = paletteSpec.lightBg,
            onBackground = AbyssalNavy,
            surface = NuviqCreamSurface,
            onSurface = AbyssalNavy,
            surfaceVariant = Color(0xFFEBE2EE),
            onSurfaceVariant = DuskSlateNavy,
            outline = Color.White.copy(alpha = 0.78f),
            error = DangerRose,
            onError = Color.White
        )
    }

    val glassColors = when {
        atmosphere == VaultAtmosphere.VAULT_LEVEL_1 -> LuminaGlassColors(
            isDark = true,
            glassFillTop = VaultL1Accent.copy(alpha = 0.16f),
            glassFillBottom = Color(0xFF071612),
            glassStrongFill = Color(0xCC081914),
            glassBorder = Color.White.copy(alpha = 0.30f),
            glassHighlight = VaultL1Accent.copy(alpha = 0.40f),
            meshBase = Color(0xFF040F0B),
            meshOrb1 = VaultL1Glow.copy(alpha = 0.48f),
            meshOrb2 = VaultL1Accent.copy(alpha = 0.20f),
            meshOrb3 = VaultL1MeshSecondary.copy(alpha = 0.50f),
            meshOrb4 = TealAurora.copy(alpha = 0.16f),
            patternOverlay = VaultL1Accent.copy(alpha = 0.06f),
            shimmerBase = Color(0xFF0D231C),
            shimmerHighlight = Color(0xFF173B2F),
            accentGlow = VaultL1Accent,
            glassSecondaryText = Color(0xFFCBD5E1),
            prismBorderStart = Color.White.copy(alpha = 0.52f),
            prismBorderMid = VaultL1Accent.copy(alpha = 0.50f),
            prismBorderEnd = TealAurora.copy(alpha = 0.40f),
            editorialGold = VaultL1Accent,
            solidFallbackSurface = Color(0xFF0A1B16),
            solidFallbackElevated = Color(0xFF112922),
            dispersionCool = VaultL1Accent,
            dispersionWarm = Color(0xFF2DD4BF)
        )
        atmosphere == VaultAtmosphere.VAULT_LEVEL_2 -> LuminaGlassColors(
            isDark = true,
            glassFillTop = VaultL2Accent.copy(alpha = 0.18f),
            glassFillBottom = Color(0xFF1C0913),
            glassStrongFill = Color(0xCC190710),
            glassBorder = Color.White.copy(alpha = 0.30f),
            glassHighlight = VaultL2Secondary.copy(alpha = 0.40f),
            meshBase = Color(0xFF11050A),
            meshOrb1 = VaultL2MeshSecondary.copy(alpha = 0.60f),
            meshOrb2 = VaultL2Secondary.copy(alpha = 0.20f),
            meshOrb3 = VaultL2Accent.copy(alpha = 0.28f),
            meshOrb4 = Color(0xFFA855F7).copy(alpha = 0.16f),
            patternOverlay = VaultL2Secondary.copy(alpha = 0.06f),
            shimmerBase = Color(0xFF28101A),
            shimmerHighlight = Color(0xFF3E1828),
            accentGlow = VaultL2Accent,
            glassSecondaryText = Color(0xFFCBD5E1),
            prismBorderStart = Color.White.copy(alpha = 0.52f),
            prismBorderMid = VaultL2Accent.copy(alpha = 0.52f),
            prismBorderEnd = VaultL2Secondary.copy(alpha = 0.44f),
            editorialGold = VaultL2Secondary,
            solidFallbackSurface = Color(0xFF1D0B14),
            solidFallbackElevated = Color(0xFF2B101E),
            dispersionCool = VaultL2Accent,
            dispersionWarm = VaultL2Secondary
        )
        effectiveDark -> LuminaGlassColors(
            isDark = true,
            glassFillTop = BlushMist.copy(alpha = 0.15f),
            glassFillBottom = paletteSpec.darkGlassBottom,
            glassStrongFill = paletteSpec.darkGlassStrong,
            glassBorder = paletteSpec.darkDispersionCool.copy(alpha = 0.32f),
            glassHighlight = (dynamicAccent ?: paletteSpec.darkPrimary).copy(alpha = 0.38f),
            meshBase = paletteSpec.darkBg,
            meshOrb1 = paletteSpec.darkOrb1,
            meshOrb2 = paletteSpec.darkOrb2,
            meshOrb3 = paletteSpec.darkOrb3,
            meshOrb4 = paletteSpec.darkOrb4,
            meshOrb5 = paletteSpec.darkOrb5,
            patternOverlay = Color.Transparent,
            shimmerBase = paletteSpec.darkSurface,
            shimmerHighlight = DuskSlateNavy,
            accentGlow = dynamicAccent ?: paletteSpec.darkPrimary,
            glassHighlightTop = Color.White.copy(alpha = 0.24f),
            glassHighlightBottom = paletteSpec.darkPrimary.copy(alpha = 0.08f),
            glassTopEdgeHighlight = BlushMist.copy(alpha = 0.62f),
            glassBottomEdgeHighlight = paletteSpec.darkDispersionCool.copy(alpha = 0.30f),
            glassSecondaryText = Color(0xFFD5DCE8),
            prismBorderStart = BlushMist.copy(alpha = 0.60f),
            prismBorderMid = paletteSpec.darkDispersionCool.copy(alpha = 0.50f),
            prismBorderEnd = paletteSpec.darkDispersionWarm.copy(alpha = 0.48f),
            editorialGold = paletteSpec.darkTextAccent,
            solidFallbackSurface = paletteSpec.darkSurface,
            solidFallbackElevated = DuskSlateNavy,
            dispersionCool = paletteSpec.darkDispersionCool,
            dispersionWarm = paletteSpec.darkDispersionWarm
        )
        else -> LuminaGlassColors(
            isDark = false,
            glassFillTop = Color.White.copy(alpha = 0.68f),
            glassFillBottom = paletteSpec.lightGlassBottom,
            glassStrongFill = Color(0xD2FFFFFF),
            glassBorder = Color.White.copy(alpha = 0.86f),
            glassHighlight = (dynamicAccent ?: paletteSpec.lightPrimary).copy(alpha = 0.24f),
            meshBase = paletteSpec.lightBg,
            meshOrb1 = paletteSpec.lightOrb1,
            meshOrb2 = paletteSpec.lightOrb2,
            meshOrb3 = paletteSpec.lightOrb3,
            meshOrb4 = paletteSpec.lightOrb4,
            meshOrb5 = BlushMist.copy(alpha = 0.56f),
            patternOverlay = Color.Transparent,
            shimmerBase = Color(0xFFE7DCE7),
            shimmerHighlight = Color(0xFFF7F1F7),
            accentGlow = dynamicAccent ?: paletteSpec.lightPrimary,
            glassHighlightTop = Color.White.copy(alpha = 0.92f),
            glassHighlightBottom = paletteSpec.lightOrb2.copy(alpha = 0.28f),
            glassTopEdgeHighlight = Color.White,
            glassBottomEdgeHighlight = paletteSpec.lightDispersionWarm.copy(alpha = 0.34f),
            glassSecondaryText = DuskSlateNavy,
            prismBorderStart = Color.White,
            prismBorderMid = paletteSpec.lightDispersionCool.copy(alpha = 0.56f),
            prismBorderEnd = paletteSpec.lightDispersionWarm.copy(alpha = 0.50f),
            editorialGold = paletteSpec.lightTextAccent,
            solidFallbackSurface = Color.White,
            solidFallbackElevated = Color(0xFFEDE4EE),
            dispersionCool = paletteSpec.lightDispersionCool,
            dispersionWarm = paletteSpec.lightDispersionWarm
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(view, effectiveDark) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !effectiveDark
            controller.isAppearanceLightNavigationBars = !effectiveDark
        }
    }

    val motionConfig = rememberMotionConfig()

    CompositionLocalProvider(
        LocalLuminaColors provides glassColors,
        LocalMotionConfig provides motionConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LuminaTypography,
            shapes = LuminaShapes,
            content = content
        )
    }
}
