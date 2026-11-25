package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.domain.model.UiColorPalette

// User Palette Tokens (#0D1E4C, #C48CB3, #E5C9D7, #83A6CE, #26415E, #0B1B32)
val OrchidPrimary = Color(0xFFC48CB3)
val OrchidDeepInk = Color(0xFF8A4F78)
val BlushMist = Color(0xFFE5C9D7)
val SerenityBlue = Color(0xFF83A6CE)
val DuskSlateNavy = Color(0xFF26415E)
val MidnightNavy = Color(0xFF0D1E4C)
val AbyssalNavy = Color(0xFF0B1B32)

fun UiColorPalette.swatchColor(): Color = when (this) {
    UiColorPalette.MIDNIGHT_NAVY -> MidnightNavy
    UiColorPalette.ORCHID_MAUVE -> OrchidPrimary
    UiColorPalette.BLUSH_MIST -> BlushMist
    UiColorPalette.SERENITY_BLUE -> SerenityBlue
    UiColorPalette.DUSK_SLATE -> DuskSlateNavy
    UiColorPalette.ABYSSAL_NAVY -> AbyssalNavy
}

// Liquid Glass Light Mode Palette (Blush Pearl, #C48CB3 Orchid & #83A6CE Serenity Blue)
val NuviqCreamBg = Color(0xFFF3EEF4)
val NuviqCreamSurface = Color(0xFFFFFFFF)
val NuviqLimeWash = BlushMist
val NuviqSageTint = SerenityBlue
val NuviqSagePill = Color(0xFFF6F1F6)
val NuviqOlivePrimary = OrchidPrimary
val NuviqOliveLight = SerenityBlue
val NuviqOliveDark = DuskSlateNavy
val NuviqDeepForest = MidnightNavy
val NuviqMossInk = AbyssalNavy
val NuviqInkMuted = DuskSlateNavy
val NuviqHairlineBorder = Color(0xFFE2D7E4)
val NuviqWarmAmber = OrchidDeepInk

// Liquid Glass Dark Mode Palette (#0B1B32 Abyssal Navy, #0D1E4C Midnight, #26415E Dusk & #C48CB3 Orchid)
val IndigoDeep = AbyssalNavy
val IndigoSurface = Color(0xFF132642)
val IndigoAccent = OrchidPrimary
val VioletGlow = SerenityBlue
val VioletDeep = MidnightNavy
val TealAurora = BlushMist
val TealDeep = DuskSlateNavy
val ObsidianInk = AbyssalNavy

// Atmospheric Glow Orbs & Dispersion Tints
const val ENABLE_GALLERY_EXTRA_ACCENT_ORBS = true
val GalleryOrbViolet = OrchidPrimary
val GalleryOrbRose = BlushMist
val GalleryOrbGold = SerenityBlue
val PrismDispersionCyan = SerenityBlue
val PrismDispersionViolet = OrchidPrimary

// Light Mode Aliases
val PearlBackground = NuviqCreamBg
val PearlSurface = NuviqCreamSurface
val IndigoLightPrimary = NuviqOlivePrimary
val VioletLightSecondary = NuviqOliveDark
val TealLightTertiary = NuviqWarmAmber

// Vault Level 1 ("Secret Folder") - Emerald Obsidian + Jade + Gold Seal
val VaultL1Accent = Color(0xFF2EE59D)
val VaultL1Glow = Color(0xFF059669)
val VaultL1MeshSecondary = Color(0xFF064E3B)

// Vault Level 2 ("Deep Vault") - Crimson Velvet + Ruby + Molten Gold
val VaultL2Accent = Color(0xFFFB7185)
val VaultL2Secondary = Color(0xFFFBBF24)
val VaultL2MeshPrimary = Color(0xFF2A0817)
val VaultL2MeshSecondary = Color(0xFF4C0519)

// Semantic Colors
val SuccessEmerald = Color(0xFF10B981)
val SnapSuccessMint = SerenityBlue
val WarningAmber = Color(0xFFF59E0B)
val DangerRose = Color(0xFFEF4444)
val FavoriteRose = OrchidPrimary
val DestructiveCoral = VaultL2Accent
val DarkInkOnAccent = AbyssalNavy

// Text Colors
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFFE5C9D7)
val TextTertiaryDark = SerenityBlue

val TextPrimaryLight = NuviqMossInk
val TextSecondaryLight = NuviqInkMuted
val TextTertiaryLight = Color(0xFF4A6482)
