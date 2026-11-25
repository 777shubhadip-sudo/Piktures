package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.example.R
import com.example.data.repository.VaultImageRequest
import com.example.domain.model.MediaItemRef
import com.example.domain.model.VaultMedia
import com.example.security.PassVerifyResult
import com.example.ui.components.AnimatedEmptyState
import com.example.ui.components.CircularGlassIconButton
import com.example.ui.components.CollapsingGlassTopBar
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassModalButton
import com.example.ui.components.LiquidGlassModalSecondaryButton
import com.example.ui.components.MediaSectionsGrid
import com.example.ui.components.liquidGlassSettle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.PlayfairFontFamily
import com.example.ui.theme.SyneFontFamily
import com.example.ui.theme.tactilePressMotion
import com.example.ui.viewmodel.DateSection
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VaultGalleryScreen(
    level: Int,
    sections: List<DateSection<MediaItemRef.Vault>>,
    allItems: List<VaultMedia>,
    columns: Int,
    selectedIds: Set<String>,
    hasExtraPass: Boolean,
    imageLoader: ImageLoader,
    thumbProvider: (VaultMedia) -> VaultImageRequest,
    onBackAndLock: () -> Unit,
    onTriggerLevel2: () -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onOpenVaultMedia: (MediaItemRef.Vault, List<VaultMedia>) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSetSelect: (String, Boolean) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onUnhideSelected: (List<String>) -> Unit,
    onDeleteSelectedPermanently: (List<String>) -> Unit,
    activeSharedKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val gridState = rememberLazyGridState()
    val motion = LocalMotionConfig.current
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val contentTopPadding = if (headerHeightPx > 0) {
        with(density) { headerHeightPx.toDp() } + 8.dp
    } else {
        118.dp
    }
    val isScrolled by remember(gridState) {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 20 }
    }
    val selectedKeys = remember(selectedIds) { selectedIds.map { "v_$it" }.toSet() }
    val tapTimestamps = remember { mutableStateListOf<Long>() }
    var confirmDeleteDialog by remember { mutableStateOf(false) }

    val titleText = if (level == 1) "Secret Folder" else "Deep Vault"

    Box(modifier = Modifier.fillMaxSize()) {
        if (sections.isEmpty()) {
            AnimatedEmptyState(
                illustrationRes = R.drawable.img_empty_vault_1794970584909,
                title = if (level == 1) "Secret Folder is Empty" else "Deep Vault is Empty",
                subtitle = if (level == 1) {
                    "Select photos or videos in your gallery and hide them here with AES-256-GCM encryption."
                } else {
                    "Items moved here from your hidden viewer stay encrypted separately."
                },
                modifier = Modifier.padding(top = contentTopPadding * 0.45f)
            )
        } else {
            MediaSectionsGrid(
                sections = sections,
                columns = columns,
                selectedKeys = selectedKeys,
                imageLoader = imageLoader,
                dataProvider = { ref -> thumbProvider(ref.item) },
                onColumnsChanged = onColumnsChanged,
                onItemClick = { ref, _ -> onOpenVaultMedia(ref, allItems) },
                onToggleSelect = { ref -> onToggleSelect(ref.item.id) },
                onSetSelect = { ref, sel -> onSetSelect(ref.item.id, sel) },
                gridState = gridState,
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = contentTopPadding, bottom = 104.dp),
                sharedElementPrefix = if (level == 1) "VAULT_L1" else "VAULT_L2",
                activeSharedKey = activeSharedKey,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
        ) {
            if (selectedIds.isNotEmpty()) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onClear = onClearSelection,
                    onSelectAll = { onSelectAll(allItems.map { it.id }) }
                )
            } else {
                CollapsingGlassTopBar(
                    eyebrow = if (level == 2) "AES-256-GCM • LEVEL 2" else "AES-256-GCM • ENCRYPTED",
                    title = titleText,
                    subtitle = "${allItems.size} encrypted items",
                    isLargeDisplayTitle = true,
                    isScrolled = isScrolled,
                    onTitleClick = if (level == 1) {
                        {
                            // Silent 5-tap within 3 seconds; does nothing if no Extra Pass is set
                            if (hasExtraPass) {
                                val now = SystemClock.elapsedRealtime()
                                tapTimestamps.add(now)
                                tapTimestamps.removeAll { now - it > 3000L }
                                if (tapTimestamps.size >= 5) {
                                    tapTimestamps.clear()
                                    onTriggerLevel2()
                                }
                            }
                        }
                    } else null,
                    navigationIcon = {
                        IconButton(
                            onClick = onBackAndLock,
                            modifier = Modifier.testTag("vault_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back and lock")
                        }
                    }
                ) {
                    CircularGlassIconButton(
                        icon = Icons.Rounded.Lock,
                        contentDescription = "Lock vault",
                        onClick = onBackAndLock,
                        size = 46.dp,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("vault_lock_button")
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = motion.bottomSheetEnter(),
            exit = motion.bottomSheetExit(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            GlassSurface(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .height(64.dp),
                shape = CircleShape,
                strongFill = true,
                elevation = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val unhideInteraction = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = { onUnhideSelected(selectedIds.toList()) },
                        interactionSource = unhideInteraction,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(unhideInteraction, squashAmount = 0.045f)
                            .testTag("vault_batch_unhide")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unhide")
                    }
                    val deleteInteraction = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = { confirmDeleteDialog = true },
                        interactionSource = deleteInteraction,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .tactilePressMotion(deleteInteraction, squashAmount = 0.045f)
                            .testTag("vault_batch_delete")
                    ) {
                        Icon(
                            Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDeleteDialog) {
        val glass = LocalLuminaColors.current
        LiquidGlassDialog(
            onDismissRequest = { confirmDeleteDialog = false },
            eyebrow = "ZERO-TRACE WIPE",
            title = "Delete Permanently?",
            icon = Icons.Rounded.DeleteForever,
            iconTint = MaterialTheme.colorScheme.error
        ) {
            Text(
                text = "${selectedIds.size} encrypted item(s) will be securely wiped and cannot be restored.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.glassSecondaryText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassModalSecondaryButton(
                    label = "Cancel",
                    onClick = { confirmDeleteDialog = false },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassModalButton(
                    label = "Wipe Forever",
                    isDestructive = true,
                    onClick = {
                        confirmDeleteDialog = false
                        onDeleteSelectedPermanently(selectedIds.toList())
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VaultPasscodeScreen(
    lockoutRemainingSeconds: Long,
    onVerifyPasscode: (String, (PassVerifyResult) -> Unit) -> Unit,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val glass = LocalLuminaColors.current
    val motion = LocalMotionConfig.current
    val scope = rememberCoroutineScope()

    var entered by remember { mutableStateOf("") }
    var useTextKeyboard by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val triggerWrongFeedback: (String) -> Unit = { msg ->
        errorMessage = msg
        entered = ""
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!motion.prefersReducedMotion) {
            scope.launch {
                listOf(-18f, 18f, -12f, 12f, -6f, 0f).forEach { target ->
                    shakeOffset.animateTo(
                        target,
                        tween(durationMillis = 42, easing = MotionTokens.EaseOutQuint)
                    )
                }
            }
        }
    }

    val submitCode = {
        if (entered.isNotEmpty() && lockoutRemainingSeconds <= 0L) {
            onVerifyPasscode(entered) { result ->
                when (result) {
                    is PassVerifyResult.Success -> {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSuccess()
                    }
                    is PassVerifyResult.Failed -> {
                        triggerWrongFeedback("Incorrect pass • ${result.attemptsRemainingBeforeLockout} left")
                    }
                    is PassVerifyResult.LockedOut -> {
                        triggerWrongFeedback("Locked out for ${result.remainingSeconds}s")
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularGlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onCancel,
                size = 46.dp,
                modifier = Modifier.testTag("passcode_back")
            )
            CircularGlassIconButton(
                icon = Icons.Rounded.Keyboard,
                contentDescription = "Switch keyboard mode",
                onClick = { useTextKeyboard = !useTextKeyboard },
                size = 46.dp,
                modifier = Modifier.testTag("passcode_toggle_keyboard")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .size(106.dp)
                .liquidGlassSettle(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        0.8.dp,
                        Brush.linearGradient(listOf(glass.prismBorderStart, glass.prismBorderMid, Color.Transparent)),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f), CircleShape)
            )
            GlassSurface(
                modifier = Modifier.size(66.dp),
                shape = CircleShape,
                strongFill = true,
                elevation = 16.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(glass.editorialGold)
            )
            Text(
                text = "AES-256-GCM · LEVEL 2",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    letterSpacing = 1.0.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Deep Vault Access",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                letterSpacing = (-0.015).em
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (lockoutRemainingSeconds > 0L) {
                "Too many attempts. Try again in ${lockoutRemainingSeconds}s"
            } else {
                errorMessage ?: "Enter your Extra Secret Folder Pass"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (lockoutRemainingSeconds > 0L || errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                glass.glassSecondaryText
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (useTextKeyboard) {
            OutlinedTextField(
                value = entered,
                onValueChange = { if (lockoutRemainingSeconds <= 0L) entered = it },
                singleLine = true,
                enabled = lockoutRemainingSeconds <= 0L,
                visualTransformation = PasswordVisualTransformation(),
                label = { Text("Secret Pass") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitCode() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .testTag("passcode_text_field")
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = submitCode,
                enabled = entered.isNotEmpty() && lockoutRemainingSeconds <= 0L,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("passcode_submit_button")
            ) {
                Text("Unlock")
            }
        } else {
            // Obscured glass dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .padding(vertical = 8.dp)
            ) {
                val dotCount = entered.length.coerceAtLeast(4).coerceAtMost(10)
                for (i in 0 until dotCount) {
                    val filled = i < entered.length
                    val dotScale by animateFloatAsState(
                        targetValue = when {
                            motion.prefersReducedMotion -> 1f
                            filled -> 1.16f
                            else -> 0.92f
                        },
                        animationSpec = motion.floatSpring(allowOvershoot = true),
                        label = "passcode_dot_scale_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer {
                                scaleX = dotScale
                                scaleY = dotScale
                            }
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else glass.glassFillTop
                            )
                            .border(1.dp, glass.glassBorder, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tactile glass numeric pad housed inside a sculpted Liquid Glass tray
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BACK", "0", "GO")
            )
            GlassSurface(
                shape = RoundedCornerShape(36.dp),
                strongFill = false,
                elevation = 16.dp,
                enableBackdropBlur = false,
                cornerRadiusHint = 36.dp,
                modifier = Modifier.widthIn(max = 332.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                val enabled = lockoutRemainingSeconds <= 0L
                                val keyInteraction = remember { MutableInteractionSource() }
                                val isGoPrimary = key == "GO" && entered.isNotEmpty()
                                GlassSurface(
                                    shape = CircleShape,
                                    strongFill = isGoPrimary,
                                    enableBackdropBlur = false,
                                    elevation = if (isGoPrimary) 8.dp else 4.dp,
                                    cornerRadiusHint = 35.dp,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .tactilePressMotion(
                                            interactionSource = keyInteraction,
                                            enabled = enabled,
                                            squashAmount = 0.04f,
                                            highFrequency = true
                                        )
                                        .clip(CircleShape)
                                        .testTag("keypad_$key")
                                        .clickable(
                                            interactionSource = keyInteraction,
                                            indication = LocalIndication.current,
                                            enabled = enabled
                                        ) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            errorMessage = null
                                            when (key) {
                                                "BACK" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                                "GO" -> submitCode()
                                                else -> if (entered.length < 16) entered += key
                                            }
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        when (key) {
                                            "BACK" -> Icon(
                                                Icons.AutoMirrored.Rounded.Backspace,
                                                contentDescription = "Backspace"
                                            )
                                            "GO" -> Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = "Submit",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            else -> Text(
                                                text = key,
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    fontFamily = SyneFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 22.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
