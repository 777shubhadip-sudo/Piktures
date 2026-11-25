package com.example.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.authStateFlow
import com.example.data.remote.onGoogleSignInClicked
import com.example.data.remote.signOutFromCloud
import com.example.data.repository.CloudSyncRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.CloudUiState
import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaType
import com.example.domain.model.RelockTimeout
import com.example.domain.model.SyncedMediaItem
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserCloudProfile
import com.example.ui.components.CollapsingGlassTopBar
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassModalButton
import com.example.ui.components.LiquidGlassModalSecondaryButton
import com.example.ui.components.LocalHazeState
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.staggeredEntrance
import com.example.ui.theme.tactilePressMotion
import com.example.ui.theme.verticalRubberBand
import com.example.ui.viewmodel.CloudSyncViewModel
import com.example.ui.viewmodel.DateGroupingUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import dev.chrisbanes.haze.hazeSource

@Composable
fun CloudSyncScreen(
    settings: AppSettings,
    favorites: List<GalleryMedia>,
    onBack: () -> Unit,
    onApplyCloudSettings: (ThemeMode, Int, RelockTimeout) -> Unit
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        CloudAuthScreen(
            credentialManager = credentialManager,
            onBack = onBack
        )
    } else {
        key(user.uid) {
            CloudSyncAuthenticatedPane(
                currentUserId = user.uid,
                userDisplayName = user.displayName,
                userEmail = user.email,
                settings = settings,
                favorites = favorites,
                credentialManager = credentialManager,
                onBack = onBack,
                onApplyCloudSettings = onApplyCloudSettings
            )
        }
    }
}

@Composable
private fun CloudAuthScreen(
    credentialManager: CredentialManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = LocalLuminaColors.current
    val hazeState = LocalHazeState.current
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val contentTopPadding = if (headerHeightPx > 0) {
        with(density) { headerHeightPx.toDp() } + 16.dp
    } else {
        118.dp
    }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .then(
                    if (hazeState != null) Modifier.hazeSource(state = hazeState, zIndex = 1f)
                    else Modifier
                )
                .padding(top = contentTopPadding, start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassSurface(
                shape = RoundedCornerShape(28.dp),
                elevation = 8.dp,
                enableBackdropBlur = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Sign in to Cloud Sync",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Back up your Piktures theme & vault preferences, favorite bookmarks, and private photo notes securely to Firebase Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.glassSecondaryText
                    )

                    authError?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("cloud_auth_error")
                        )
                    }

                    Button(
                        onClick = {
                            authError = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = { isLoading = false },
                                onAuthError = { msg ->
                                    isLoading = false
                                    authError = msg
                                },
                                scope = scope,
                                onAuthCancelled = { isLoading = false }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        CollapsingGlassTopBar(
            eyebrow = "FIREBASE CLOUD",
            title = "Cloud Sync",
            isLargeDisplayTitle = true,
            isScrolled = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("cloud_sync_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }
}

@Composable
private fun CloudSyncAuthenticatedPane(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?,
    settings: AppSettings,
    favorites: List<GalleryMedia>,
    credentialManager: CredentialManager,
    onBack: () -> Unit,
    onApplyCloudSettings: (ThemeMode, Int, RelockTimeout) -> Unit
) {
    val cloudViewModel: CloudSyncViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CloudSyncViewModel(CloudSyncRepository(db), currentUserId)
            }
        }
    )

    val profileState by cloudViewModel.profileState.collectAsStateWithLifecycle()
    val syncedItemsState by cloudViewModel.syncedItemsState.collectAsStateWithLifecycle()
    val isSyncing by cloudViewModel.isSyncing.collectAsStateWithLifecycle()
    val statusBanner by cloudViewModel.statusBanner.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val glass = LocalLuminaColors.current
    val hazeState = LocalHazeState.current
    val listState = rememberLazyListState()
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

    var noteTarget by remember { mutableStateOf<SyncedMediaItem?>(null) }
    var noteInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentTopPadding,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalRubberBand()
                .then(
                    if (hazeState != null) Modifier.hazeSource(state = hazeState, zIndex = 1f)
                    else Modifier
                )
        ) {
            statusBanner?.let { banner ->
                item {
                    GlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        strongFill = true,
                        enableBackdropBlur = false
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = banner,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = cloudViewModel::clearStatusBanner,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold
                                )
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Account Card
            item {
                GlassSurface(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 4.dp,
                    enableBackdropBlur = false,
                    modifier = Modifier.staggeredEntrance(0)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userDisplayName?.takeIf { it.isNotBlank() } ?: "Google Account",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = userEmail ?: currentUserId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.glassSecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    signOutFromCloud(credentialManager, scope)
                                },
                                modifier = Modifier.testTag("cloud_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "Sign out",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    cloudViewModel.syncPreferencesNow(userDisplayName, userEmail, settings)
                                },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sync_preferences_button")
                            ) {
                                Text("Sync Settings")
                            }
                            OutlinedButton(
                                onClick = { cloudViewModel.backupFavoritesNow(favorites) },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("backup_favorites_button")
                            ) {
                                Text("Backup Favorites (${favorites.size})")
                            }
                        }
                    }
                }
            }

            // Cloud Preferences Snapshot
            item {
                CloudProfileSection(
                    state = profileState,
                    modifier = Modifier.staggeredEntrance(1),
                    onRestoreCloudSettings = { profile ->
                        val theme = ThemeMode.entries.firstOrNull { it.name == profile.themeMode } ?: ThemeMode.DARK
                        val cols = profile.gridColumns.toInt().coerceIn(2, 5)
                        val relock = RelockTimeout.entries.firstOrNull { it.name == profile.relockTimeout } ?: RelockTimeout.IMMEDIATE
                        onApplyCloudSettings(theme, cols, relock)
                    }
                )
            }

            // Synced Favorites & Notes Header
            item {
                Text(
                    text = "Cloud Favorites & Notes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            when (val itemsUi = syncedItemsState) {
                is CloudUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is CloudUiState.Error -> {
                    item {
                        GlassSurface(
                            shape = RoundedCornerShape(20.dp),
                            enableBackdropBlur = false
                        ) {
                            Text(
                                text = itemsUi.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is CloudUiState.Success -> {
                    if (itemsUi.data.isEmpty()) {
                        item {
                            GlassSurface(
                                shape = RoundedCornerShape(20.dp),
                                enableBackdropBlur = false
                            ) {
                                Text(
                                    text = "No cloud favorites backed up yet. Tap \"Backup Favorites\" above to save your starred photos & videos to Firestore.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.glassSecondaryText,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        itemsIndexed(itemsUi.data, key = { _, it -> it.id }) { index, item ->
                            SyncedMediaCard(
                                item = item,
                                onEditNote = {
                                    noteInput = item.note.orEmpty()
                                    noteTarget = item
                                },
                                onDelete = { cloudViewModel.removeSyncedItem(item.id) }
                            )
                        }
                    }
                }
            }
        }

        CollapsingGlassTopBar(
            eyebrow = "FIREBASE CLOUD",
            title = "Cloud Sync",
            isLargeDisplayTitle = true,
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("cloud_sync_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }

    noteTarget?.let { target ->
        LiquidGlassDialog(
            onDismissRequest = { noteTarget = null },
            eyebrow = "CLOUD MEMORY NOTE",
            title = "Photo Note",
            icon = Icons.Rounded.EditNote
        ) {
            OutlinedTextField(
                value = noteInput,
                onValueChange = { if (it.length <= 500) noteInput = it },
                label = { Text("Memory note (max 500 chars)") },
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = glass.glassBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassModalSecondaryButton(
                    label = "Cancel",
                    onClick = { noteTarget = null },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassModalButton(
                    label = "Save",
                    onClick = {
                        cloudViewModel.saveItemNote(target.id, noteInput)
                        noteTarget = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CloudProfileSection(
    state: CloudUiState<UserCloudProfile?>,
    onRestoreCloudSettings: (UserCloudProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLuminaColors.current
    GlassSurface(
        shape = RoundedCornerShape(24.dp),
        elevation = 4.dp,
        enableBackdropBlur = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Synced Preferences",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            when (state) {
                is CloudUiState.Loading -> {
                    Text(
                        text = "Checking cloud profile...",
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.glassSecondaryText
                    )
                }
                is CloudUiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is CloudUiState.Success -> {
                    val profile = state.data
                    if (profile == null) {
                        Text(
                            text = "No cloud preferences saved yet. Tap \"Sync Settings\" to store your theme, grid density, and vault timeout.",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.glassSecondaryText
                        )
                    } else {
                        Text(
                            text = "Theme: ${profile.themeMode}  •  Grid: ${profile.gridColumns}×  •  Vault lock: ${profile.relockTimeout}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.glassSecondaryText
                        )
                        OutlinedButton(
                            onClick = { onRestoreCloudSettings(profile) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Cloud Preferences")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedMediaCard(
    item: SyncedMediaItem,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLuminaColors.current
    val cardInteraction = remember { MutableInteractionSource() }
    GlassSurface(
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp,
        enableBackdropBlur = false,
        modifier = modifier
            .tactilePressMotion(cardInteraction, squashAmount = 0.03f)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = cardInteraction,
                    indication = LocalIndication.current,
                    onClick = onEditNote
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.mediaType == MediaType.VIDEO.name) Icons.Rounded.Videocam else Icons.Rounded.Image,
                    contentDescription = null,
                    tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.favorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${item.bucketName.ifBlank { "Gallery" }}  •  ${DateGroupingUtils.formatFileSize(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.glassSecondaryText,
                    maxLines = 1
                )
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onEditNote) {
                Icon(Icons.Rounded.EditNote, contentDescription = "Edit note")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove from cloud",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
