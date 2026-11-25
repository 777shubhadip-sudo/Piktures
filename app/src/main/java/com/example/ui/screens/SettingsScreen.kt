package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.RelockTimeout
import com.example.domain.model.ThemeMode
import com.example.domain.model.UiColorPalette
import com.example.ui.components.CollapsingGlassTopBar
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassModalButton
import com.example.ui.components.LiquidGlassModalSecondaryButton
import com.example.ui.components.LiquidGlassPillChip
import com.example.ui.components.LiquidGlassSegmentedControl
import com.example.ui.components.LocalHazeState
import com.example.ui.components.LocalReduceTransparency
import com.example.ui.theme.AbyssalNavy
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.originAwareEntrance
import com.example.ui.theme.staggeredEntrance
import com.example.ui.theme.swatchColor
import com.example.ui.theme.tactilePressMotion
import com.example.ui.theme.verticalRubberBand
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsScreen(
    settings: AppSettings,
    hasExtraPass: Boolean,
    onBack: () -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onUiColorPaletteChanged: (UiColorPalette) -> Unit = {},
    onGridDensityChanged: (Int) -> Unit,
    onRelockTimeoutChanged: (RelockTimeout) -> Unit,
    onReduceTransparencyChanged: (Boolean) -> Unit = {},
    onOpenTrash: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onRequireFreshAuthForExtraPass: (onAuthenticated: () -> Unit) -> Unit,
    onSaveExtraPass: (String) -> Unit,
    onClearExtraPass: () -> Unit
) {
    val listState = rememberLazyListState()
    val glass = LocalLuminaColors.current
    val hazeState = LocalHazeState.current
    val effectiveReduceTransparency = LocalReduceTransparency.current
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val contentTopPadding = if (headerHeightPx > 0) {
        with(density) { headerHeightPx.toDp() } + 8.dp
    } else {
        112.dp
    }

    val isScrolled by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20 }
    }

    var showPassConfigDialog by remember { mutableStateOf(false) }
    var passInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var passError by remember { mutableStateOf<String?>(null) }

    val themeOptions = remember { ThemeMode.entries.map { it.name to it.label } }
    val densityOptions = remember { listOf(2, 3, 4, 5).map { "$it" to "${it}×" } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentTopPadding,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalRubberBand()
                .then(
                    if (hazeState != null) {
                        Modifier.hazeSource(state = hazeState, zIndex = 1f)
                    } else {
                        Modifier
                    }
                )
        ) {
            // Liquid Glass Studio Overview Bento Header
            item {
                GlassSurface(
                    shape = RoundedCornerShape(28.dp),
                    strongFill = false,
                    elevation = 14.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 28.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(0)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (glass.isDark) 0.16f else 0.10f),
                                            glass.dispersionCool.copy(alpha = if (glass.isDark) 0.10f else 0.08f),
                                            glass.dispersionWarm.copy(alpha = if (glass.isDark) 0.10f else 0.06f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "PIKTURES STUDIO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            letterSpacing = 1.1.sp
                                        ),
                                        color = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Piktures Preferences",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.app_icon_fg_piktures),
                                    contentDescription = "Piktures logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .border(1.dp, glass.prismBorderStart.copy(alpha = 0.78f), RoundedCornerShape(13.dp))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudioStatChip(
                                    label = "CANVAS",
                                    value = settings.themeMode.label.uppercase(),
                                    accent = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioStatChip(
                                    label = "MATERIAL",
                                    value = if (effectiveReduceTransparency) "SOLID" else "OPTICAL",
                                    accent = if (glass.isDark) glass.dispersionCool else glass.glassSecondaryText,
                                    modifier = Modifier.weight(1f)
                                )
                                StudioStatChip(
                                    label = "VAULT",
                                    value = if (hasExtraPass) "2-TIER" else "AES-256",
                                    accent = glass.editorialGold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Cloud Sync & Backup (Firebase Auth + Firestore)
            item {
                val cloudInteraction = remember { MutableInteractionSource() }
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(1)
                        .tactilePressMotion(cloudInteraction, squashAmount = 0.03f)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("setting_cloud_sync")
                        .clickable(
                            interactionSource = cloudInteraction,
                            indication = LocalIndication.current,
                            onClick = onOpenCloudSync
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingIconBadge(icon = Icons.Rounded.CloudSync)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Sync & Backup",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sign in with Google to sync preferences, favorites & notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.glassSecondaryText
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = glass.glassSecondaryText
                        )
                    }
                }
            }

            // Theme Mode & UI Color Palette
            item {
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 24.dp,
                    modifier = Modifier.staggeredEntrance(2)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingHeader(
                            icon = Icons.Rounded.Palette,
                            title = "Appearance",
                            subtitle = "Switch canvas mode & Liquid Glass UI color"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassSegmentedControl(
                            options = themeOptions,
                            selectedKey = settings.themeMode.name,
                            onSelect = { key ->
                                ThemeMode.entries.firstOrNull { it.name == key }?.let(onThemeChanged)
                            },
                            enableBackdropBlur = false,
                            tagProvider = { key -> "setting_theme_${key.lowercase()}" }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(glass.glassBorder.copy(alpha = 0.45f))
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UI Color",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (effectiveReduceTransparency) glass.solidFallbackSurface
                                        else glass.glassFillTop.copy(alpha = if (glass.isDark) 0.14f else 0.56f)
                                    )
                                    .border(0.85.dp, glass.prismBorderStart.copy(alpha = 0.65f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(settings.uiColorPalette.swatchColor())
                                        .border(0.6.dp, Color.White.copy(alpha = 0.72f), CircleShape)
                                )
                                Text(
                                    text = "${settings.uiColorPalette.label} · ${settings.uiColorPalette.hexCode}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp
                                    ),
                                    color = glass.glassSecondaryText,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val haptics = LocalHapticFeedback.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UiColorPalette.entries.forEach { palette ->
                                val isSelected = settings.uiColorPalette == palette
                                val swatchColor = palette.swatchColor()
                                val checkTint = if (swatchColor.luminance() > 0.34f) AbyssalNavy else Color.White
                                val interaction = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .tactilePressMotion(interaction, squashAmount = 0.06f)
                                        .semantics {
                                            contentDescription = "${palette.label} ${palette.hexCode}"
                                            selected = isSelected
                                        }
                                        .testTag("setting_color_${palette.name.lowercase()}")
                                        .clickable(
                                            interactionSource = interaction,
                                            indication = null,
                                            role = Role.RadioButton
                                        ) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onUiColorPaletteChanged(palette)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (glass.isDark) Color.White else AbyssalNavy,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 36.dp else 38.dp)
                                            .shadow(
                                                elevation = if (isSelected) 8.dp else 3.dp,
                                                shape = CircleShape,
                                                ambientColor = swatchColor.copy(alpha = 0.45f),
                                                spotColor = swatchColor.copy(alpha = 0.35f)
                                            )
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = if (isSelected) 0.85f else 0.45f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = checkTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Accessibility: Reduce Transparency
            item {
                val reduceInteraction = remember { MutableInteractionSource() }
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(3)
                        .tactilePressMotion(reduceInteraction, squashAmount = 0.025f)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("setting_reduce_transparency")
                        .clickable(
                            interactionSource = reduceInteraction,
                            indication = LocalIndication.current
                        ) {
                            onReduceTransparencyChanged(!settings.reduceTransparency)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingIconBadge(
                            icon = if (settings.reduceTransparency) Icons.Rounded.BlurOff else Icons.Rounded.BlurOn
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reduce Transparency",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use solid high-contrast panels instead of glass blur & refraction",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.glassSecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = settings.reduceTransparency,
                            onCheckedChange = onReduceTransparencyChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Grid Density
            item {
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 24.dp,
                    modifier = Modifier.staggeredEntrance(4)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingHeader(
                            icon = Icons.Rounded.GridView,
                            title = "Grid Density",
                            subtitle = "Default columns (also adjustable by pinching)"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassSegmentedControl(
                            options = densityOptions,
                            selectedKey = "${settings.gridColumns}",
                            onSelect = { key -> key.toIntOrNull()?.let(onGridDensityChanged) },
                            enableBackdropBlur = false,
                            tagProvider = { key -> "setting_grid_$key" }
                        )
                    }
                }
            }

            // Biometric Re-lock Timeout
            item {
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 24.dp,
                    modifier = Modifier.staggeredEntrance(5)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingHeader(
                            icon = Icons.Rounded.Timer,
                            title = "Vault Re-lock Timeout",
                            subtitle = "Re-lock hidden folders when app leaves foreground"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RelockTimeout.entries.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { option ->
                                        LiquidGlassPillChip(
                                            label = option.label,
                                            selected = settings.relockTimeout == option,
                                            onClick = { onRelockTimeoutChanged(option) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Trash Retention
            item {
                val trashInteraction = remember { MutableInteractionSource() }
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(6)
                        .tactilePressMotion(trashInteraction, squashAmount = 0.03f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = trashInteraction,
                            indication = LocalIndication.current,
                            onClick = onOpenTrash
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingIconBadge(icon = Icons.Rounded.DeleteOutline)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trash Retention",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${settings.trashRetentionDays} days before permanent removal",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.glassSecondaryText
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = glass.glassSecondaryText
                        )
                    }
                }
            }

            // Extra Secret Folder Pass (always requires fresh biometric/screen lock auth)
            item {
                val extraPassInteraction = remember { MutableInteractionSource() }
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 8.dp,
                    enableBackdropBlur = false,
                    cornerRadiusHint = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(7)
                        .tactilePressMotion(extraPassInteraction, squashAmount = 0.03f)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("setting_extra_pass")
                        .clickable(
                            interactionSource = extraPassInteraction,
                            indication = LocalIndication.current
                        ) {
                            onRequireFreshAuthForExtraPass {
                                passInput = ""
                                confirmInput = ""
                                passError = null
                                showPassConfigDialog = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingIconBadge(icon = Icons.Rounded.Key)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Extra Secret Folder Pass",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasExtraPass) "Configured (salted PBKDF2 hash)" else "Not set",
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.glassSecondaryText
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = glass.glassSecondaryText
                        )
                    }
                }
            }
        }

        CollapsingGlassTopBar(
            eyebrow = "PREFERENCES",
            title = "Settings",
            isLargeDisplayTitle = true,
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }

    if (showPassConfigDialog) {
        LiquidGlassDialog(
            onDismissRequest = { showPassConfigDialog = false },
            eyebrow = "LEVEL 2 VAULT",
            title = if (hasExtraPass) "Update Extra Secret Pass" else "Create Extra Secret Pass",
            icon = Icons.Rounded.Key
        ) {
            OutlinedTextField(
                value = passInput,
                onValueChange = {
                    passInput = it
                    passError = null
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("New pass (min 4 characters)") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = glass.glassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("extra_pass_input")
            )
            OutlinedTextField(
                value = confirmInput,
                onValueChange = {
                    confirmInput = it
                    passError = null
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Confirm pass") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = glass.glassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("extra_pass_confirm")
            )
            if (passError != null) {
                Text(
                    text = passError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasExtraPass) {
                    LiquidGlassModalSecondaryButton(
                        label = "Remove",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            onClearExtraPass()
                            showPassConfigDialog = false
                        },
                        modifier = Modifier.testTag("extra_pass_remove")
                    )
                }
                LiquidGlassModalSecondaryButton(
                    label = "Cancel",
                    onClick = { showPassConfigDialog = false },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassModalButton(
                    label = "Save",
                    onClick = {
                        when {
                            passInput.length < 4 -> passError = "Use at least 4 characters"
                            passInput != confirmInput -> passError = "Passes do not match"
                            else -> {
                                onSaveExtraPass(passInput)
                                showPassConfigDialog = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("extra_pass_save")
                )
            }
        }
    }
}

@Composable
private fun StudioStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val glass = LocalLuminaColors.current
    val reduceTransparency = LocalReduceTransparency.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (reduceTransparency) glass.solidFallbackSurface
                else glass.glassFillTop.copy(alpha = if (glass.isDark) 0.14f else 0.56f)
            )
            .border(0.9.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                letterSpacing = 0.7.sp
            ),
            color = glass.glassSecondaryText,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = accent,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingIconBadge(icon: ImageVector) {
    val glass = LocalLuminaColors.current
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        glass.dispersionCool.copy(alpha = 0.16f)
                    )
                )
            )
            .border(
                0.9.dp,
                Brush.linearGradient(listOf(glass.prismBorderStart, glass.prismBorderMid)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val glass = LocalLuminaColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = glass.glassSecondaryText
            )
        }
    }
}
