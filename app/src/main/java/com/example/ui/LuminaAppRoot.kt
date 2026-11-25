package com.example.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil.ImageLoader
import com.example.data.repository.MediaStoreRepository
import com.example.data.repository.StagedHideBatch
import com.example.domain.model.GalleryMedia
import com.example.domain.model.GalleryTab
import com.example.domain.model.MediaItemRef
import com.example.security.AuthResult
import com.example.security.BiometricAuthHelper
import com.example.ui.components.LocalHazeState
import com.example.ui.components.LocalReduceTransparency
import com.example.ui.components.LocalSupportsLiquidBlur
import com.example.ui.components.MeshGradientBackground
import com.example.ui.components.rememberSystemReduceTransparency
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.VaultGalleryScreen
import com.example.ui.screens.VaultPasscodeScreen
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.LuminaTheme
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.PeakSuccessOverlay
import com.example.ui.theme.VaultAtmosphere
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.PendingMediaRequest
import com.example.ui.viewmodel.VaultHideEvent
import com.example.ui.viewmodel.VaultViewModel
import dev.chrisbanes.haze.rememberHazeState
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LuminaAppRoot(
    activity: FragmentActivity,
    imageLoader: ImageLoader,
    galleryViewModel: GalleryViewModel = hiltViewModel(),
    vaultViewModel: VaultViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var peakSuccessEvent by remember { mutableStateOf<Pair<String, String?>?>(null) }

    LaunchedEffect(peakSuccessEvent) {
        if (peakSuccessEvent != null) {
            delay(1150L)
            peakSuccessEvent = null
        }
    }

    val permissionState by galleryViewModel.permissionState.collectAsStateWithLifecycle()
    val isInitialLoading by galleryViewModel.isInitialLoading.collectAsStateWithLifecycle()
    val currentTab by galleryViewModel.currentTab.collectAsStateWithLifecycle()
    val settings by galleryViewModel.settings.collectAsStateWithLifecycle()
    val gridColumns by galleryViewModel.gridColumns.collectAsStateWithLifecycle()
    val photoSections by galleryViewModel.photoSections.collectAsStateWithLifecycle()
    val filteredPhotoSections by galleryViewModel.filteredPhotoSections.collectAsStateWithLifecycle()
    val selectedPhotoFilter by galleryViewModel.photoFilter.collectAsStateWithLifecycle()
    val allMedia by galleryViewModel.allMedia.collectAsStateWithLifecycle()
    val albums by galleryViewModel.albums.collectAsStateWithLifecycle()
    val favorites by galleryViewModel.favorites.collectAsStateWithLifecycle()
    val favoriteSections by galleryViewModel.favoriteSections.collectAsStateWithLifecycle()
    val videos by galleryViewModel.videos.collectAsStateWithLifecycle()
    val selectedIds by galleryViewModel.selectedIds.collectAsStateWithLifecycle()
    val dynamicAccent by galleryViewModel.dynamicAccent.collectAsStateWithLifecycle()

    val level1Unlocked by vaultViewModel.level1Unlocked.collectAsStateWithLifecycle()
    val level2Unlocked by vaultViewModel.level2Unlocked.collectAsStateWithLifecycle()
    val hasExtraPass by vaultViewModel.hasExtraPass.collectAsStateWithLifecycle()
    val lockoutRemaining by vaultViewModel.lockoutRemainingSeconds.collectAsStateWithLifecycle()
    val level1Items by vaultViewModel.level1Items.collectAsStateWithLifecycle()
    val level2Items by vaultViewModel.level2Items.collectAsStateWithLifecycle()
    val level1Sections by vaultViewModel.level1Sections.collectAsStateWithLifecycle()
    val level2Sections by vaultViewModel.level2Sections.collectAsStateWithLifecycle()
    val selectedVaultIds by vaultViewModel.selectedVaultIds.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val currentViewerSource = remember(navBackStackEntry) {
        if (currentDest?.hasRoute<MediaViewerRoute>() == true) {
            navBackStackEntry?.toRoute<MediaViewerRoute>()?.source
        } else null
    }

    val isVaultScreen = remember(currentDest, currentViewerSource) {
        currentDest?.hasRoute<VaultLevel1Route>() == true ||
            currentDest?.hasRoute<VaultPasscodeRoute>() == true ||
            currentDest?.hasRoute<VaultLevel2Route>() == true ||
            currentViewerSource == "VAULT_L1" ||
            currentViewerSource == "VAULT_L2"
    }

    val atmosphere = remember(currentDest, currentViewerSource) {
        when {
            currentDest?.hasRoute<VaultLevel2Route>() == true || currentViewerSource == "VAULT_L2" ->
                VaultAtmosphere.VAULT_LEVEL_2
            currentDest?.hasRoute<VaultLevel1Route>() == true ||
                currentDest?.hasRoute<VaultPasscodeRoute>() == true ||
                currentViewerSource == "VAULT_L1" ->
                VaultAtmosphere.VAULT_LEVEL_1
            else -> VaultAtmosphere.GALLERY
        }
    }

    // Enforce FLAG_SECURE on all vault screens
    DisposableEffect(isVaultScreen) {
        if (isVaultScreen) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    // Re-lock vault when app goes to background & refresh MediaStore on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                galleryViewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        val processObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> vaultViewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> vaultViewModel.onAppForegrounded()
                else -> Unit
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver) }
    }

    // Pop vault screens automatically if locked while on a vault route
    LaunchedEffect(level1Unlocked, level2Unlocked, currentDest, currentViewerSource) {
        val onLevel1OrViewer = currentDest?.hasRoute<VaultLevel1Route>() == true ||
            currentDest?.hasRoute<VaultPasscodeRoute>() == true ||
            currentViewerSource == "VAULT_L1"
        val onLevel2OrViewer = currentDest?.hasRoute<VaultLevel2Route>() == true ||
            currentViewerSource == "VAULT_L2"

        if (onLevel2OrViewer && !level2Unlocked) {
            navController.popBackStack(GalleryRoute, inclusive = false)
        } else if (onLevel1OrViewer && !level1Unlocked) {
            navController.popBackStack(GalleryRoute, inclusive = false)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        galleryViewModel.refreshPermissionsAndMedia()
    }

    // Pending MediaStore intent launcher (trash, restore, delete, rename write)
    var activePendingMediaRequest by remember { mutableStateOf<PendingMediaRequest?>(null) }
    val mediaIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val req = activePendingMediaRequest
        activePendingMediaRequest = null
        if (result.resultCode == Activity.RESULT_OK) {
            when (req) {
                is PendingMediaRequest.RenameWrite -> {
                    galleryViewModel.completePendingRename(req.uri, req.newName)
                }
                is PendingMediaRequest.Trash -> {
                    galleryViewModel.emitMessage("Moved to Trash")
                    galleryViewModel.refreshPermissionsAndMedia()
                }
                is PendingMediaRequest.RestoreFromTrash -> {
                    galleryViewModel.emitMessage("Restored to gallery")
                    galleryViewModel.loadTrash()
                    galleryViewModel.refreshPermissionsAndMedia()
                }
                is PendingMediaRequest.PermanentDelete -> {
                    galleryViewModel.emitMessage("Permanently deleted")
                    galleryViewModel.loadTrash()
                    galleryViewModel.refreshPermissionsAndMedia()
                }
                null -> Unit
            }
        }
    }

    // Pending Vault Hide-Delete confirmation launcher
    var activeHideBatch by remember { mutableStateOf<StagedHideBatch?>(null) }
    val hideDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val batch = activeHideBatch
        activeHideBatch = null
        if (batch != null) {
            vaultViewModel.onHideDeleteConfirmed(batch, result.resultCode == Activity.RESULT_OK)
        }
    }

    // Legacy device-credential fallback launcher
    var pendingLegacyAuthSuccess by remember { mutableStateOf<(() -> Unit)?>(null) }
    val legacyCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingLegacyAuthSuccess
        pendingLegacyAuthSuccess = null
        if (result.resultCode == Activity.RESULT_OK) {
            callback?.invoke()
        }
    }

    val runBiometricAuth: (String, String, () -> Unit) -> Unit = { title, subtitle, onSuccess ->
        BiometricAuthHelper.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle
        ) { authResult ->
            when (authResult) {
                is AuthResult.Success -> onSuccess()
                is AuthResult.NeedLegacyCredentialIntent -> {
                    pendingLegacyAuthSuccess = onSuccess
                    legacyCredentialLauncher.launch(authResult.intent)
                }
                is AuthResult.Error -> {
                    galleryViewModel.emitMessage(authResult.message)
                }
                is AuthResult.Cancelled -> Unit
            }
        }
    }

    // Collect one-shot events
    LaunchedEffect(Unit) {
        galleryViewModel.pendingMediaRequest.collect { req ->
            activePendingMediaRequest = req
            mediaIntentLauncher.launch(IntentSenderRequest.Builder(req.pendingIntent.intentSender).build())
        }
    }

    LaunchedEffect(Unit) {
        vaultViewModel.hideEvents.collect { event ->
            when (event) {
                is VaultHideEvent.NeedDeleteConfirmation -> {
                    activeHideBatch = event.batch
                    hideDeleteLauncher.launch(
                        IntentSenderRequest.Builder(event.pendingIntent.intentSender).build()
                    )
                }
                is VaultHideEvent.Completed -> {
                    galleryViewModel.clearSelection()
                    galleryViewModel.refreshPermissionsAndMedia()
                    peakSuccessEvent = "Hidden Safely" to "Encrypted ${event.count} item(s) in Secret Folder"
                }
                is VaultHideEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        galleryViewModel.messages.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    LaunchedEffect(Unit) {
        vaultViewModel.vaultMessages.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    val shareGalleryMedia: (List<GalleryMedia>) -> Unit = { list ->
        if (list.isNotEmpty()) {
            val intent = if (list.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = list.first().mimeType
                    putExtra(Intent.EXTRA_STREAM, list.first().uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                val uris = ArrayList<Uri>(list.map { it.uri })
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }
    }

    var activeSharedKey by remember { mutableStateOf<String?>(null) }
    val sysReduceTransparency = rememberSystemReduceTransparency()
    val reduceTransparency = settings.reduceTransparency || sysReduceTransparency
    val supportsBlur = !reduceTransparency && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val rootHazeState = rememberHazeState(blurEnabled = supportsBlur)

    LuminaTheme(
        themeMode = settings.themeMode,
        uiColorPalette = settings.uiColorPalette,
        atmosphere = atmosphere,
        dynamicAccent = null
    ) {
        val motion = LocalMotionConfig.current
        CompositionLocalProvider(
            LocalHazeState provides rootHazeState,
            LocalSupportsLiquidBlur provides supportsBlur,
            LocalReduceTransparency provides reduceTransparency
        ) {
            MeshGradientBackground(useDotPattern = atmosphere == VaultAtmosphere.VAULT_LEVEL_2) {
            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = GalleryRoute,
                    enterTransition = { motion.fadeEnter(MotionTokens.MicroFastMs + 20) },
                    exitTransition = { motion.fadeExit(MotionTokens.MicroFastMs) },
                    popEnterTransition = { motion.fadeEnter(MotionTokens.MicroFastMs + 20) },
                    popExitTransition = { motion.fadeExit(MotionTokens.MicroFastMs) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<GalleryRoute> {
                        GalleryScreen(
                            permissionState = permissionState,
                            isLoading = isInitialLoading,
                            currentTab = currentTab,
                            columns = gridColumns,
                            photoSections = photoSections,
                            filteredPhotoSections = filteredPhotoSections,
                            selectedPhotoFilter = selectedPhotoFilter,
                            albums = albums,
                            favorites = favorites,
                            favoriteSections = favoriteSections,
                            videos = videos,
                            selectedIds = selectedIds,
                            imageLoader = imageLoader,
                            onRequestPermissions = {
                                permissionLauncher.launch(galleryViewModel.requiredPermissions())
                            },
                            onSelectTab = galleryViewModel::selectTab,
                            onSelectPhotoFilter = galleryViewModel::selectPhotoFilter,
                            onColumnsChanged = galleryViewModel::updatePinchGridColumns,
                            onLoadNextPage = galleryViewModel::loadNextMediaPage,
                            onOpenMedia = { ref, list, src ->
                                val idx = list.indexOfFirst { it.id == ref.item.id }.coerceAtLeast(0)
                                activeSharedKey = "${src}_${ref.key}"
                                navController.navigate(MediaViewerRoute(source = src, initialIndex = idx))
                            },
                            onOpenAlbum = { album ->
                                galleryViewModel.clearSelection()
                                navController.navigate(AlbumDetailRoute(album.bucketId, album.name))
                            },
                            onOpenSearch = { navController.navigate(SearchRoute) },
                            onOpenHiddenFolder = {
                                runBiometricAuth(
                                    "Unlock Hidden Folder",
                                    "Authenticate to access Secret Folder"
                                ) {
                                    vaultViewModel.unlockLevel1()
                                    navController.navigate(VaultLevel1Route)
                                }
                            },
                            onOpenTrash = { navController.navigate(TrashRoute) },
                            onOpenSettings = { navController.navigate(SettingsRoute) },
                            onToggleSelect = galleryViewModel::toggleSelection,
                            onSetSelect = galleryViewModel::setSelection,
                            onSelectAll = galleryViewModel::selectAll,
                            onClearSelection = galleryViewModel::clearSelection,
                            onShareSelected = shareGalleryMedia,
                            onFavoriteSelected = galleryViewModel::favoriteSelected,
                            onToggleFavoriteItem = galleryViewModel::toggleFavorite,
                            onHideSelected = { list -> vaultViewModel.hideGalleryItems(list) },
                            onDeleteSelected = galleryViewModel::requestTrash,
                            activeSharedKey = activeSharedKey,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }

                    composable<AlbumDetailRoute> { backStack ->
                        val route = backStack.toRoute<AlbumDetailRoute>()
                        LaunchedEffect(Unit) {
                            galleryViewModel.ensureAllMediaLoaded()
                        }
                        val albumItems = remember(allMedia, route.bucketId) {
                            allMedia.filter { it.bucketId == route.bucketId }
                        }
                        AlbumDetailScreen(
                            albumName = route.albumName,
                            items = albumItems,
                            columns = gridColumns,
                            selectedIds = selectedIds,
                            imageLoader = imageLoader,
                            onBack = {
                                galleryViewModel.clearSelection()
                                navController.popBackStack()
                            },
                            onColumnsChanged = galleryViewModel::updatePinchGridColumns,
                            onOpenMedia = { ref, list ->
                                val idx = list.indexOfFirst { it.id == ref.item.id }.coerceAtLeast(0)
                                activeSharedKey = "ALBUM_${ref.key}"
                                navController.navigate(
                                    MediaViewerRoute(
                                        source = "ALBUM",
                                        initialIndex = idx,
                                        bucketId = route.bucketId
                                    )
                                )
                            },
                            onToggleSelect = galleryViewModel::toggleSelection,
                            onSetSelect = galleryViewModel::setSelection,
                            onSelectAll = galleryViewModel::selectAll,
                            onClearSelection = galleryViewModel::clearSelection,
                            onShareSelected = shareGalleryMedia,
                            onFavoriteSelected = galleryViewModel::favoriteSelected,
                            onHideSelected = { list -> vaultViewModel.hideGalleryItems(list) },
                            onDeleteSelected = galleryViewModel::requestTrash,
                            activeSharedKey = activeSharedKey,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }

                    composable<SearchRoute> {
                        LaunchedEffect(Unit) {
                            galleryViewModel.ensureAllMediaLoaded()
                        }
                        val searchQuery by galleryViewModel.searchQuery.collectAsStateWithLifecycle()
                        val searchFilter by galleryViewModel.searchFilter.collectAsStateWithLifecycle()
                        val searchResults by galleryViewModel.searchResults.collectAsStateWithLifecycle()
                        val suggestedAlbumNames = remember(albums) { albums.take(4).map { it.name } }
                        SearchScreen(
                            query = searchQuery,
                            activeFilter = searchFilter,
                            results = searchResults,
                            columns = gridColumns,
                            imageLoader = imageLoader,
                            onQueryChange = galleryViewModel::updateSearchQuery,
                            onFilterChange = galleryViewModel::updateSearchFilter,
                            onBack = {
                                galleryViewModel.resetSearch()
                                navController.popBackStack()
                            },
                            onOpenMedia = { ref, list ->
                                val idx = list.indexOfFirst { it.id == ref.item.id }.coerceAtLeast(0)
                                activeSharedKey = "SEARCH_${ref.key}"
                                navController.navigate(MediaViewerRoute(source = "SEARCH", initialIndex = idx))
                            },
                            suggestedAlbums = suggestedAlbumNames,
                            onColumnsChanged = galleryViewModel::updatePinchGridColumns,
                            activeSharedKey = activeSharedKey,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }

                    composable<TrashRoute> {
                        val trashedMedia by galleryViewModel.trashedMedia.collectAsStateWithLifecycle()
                        TrashScreen(
                            trashedItems = trashedMedia,
                            retentionDays = settings.trashRetentionDays,
                            imageLoader = imageLoader,
                            onLoadTrash = galleryViewModel::loadTrash,
                            onBack = { navController.popBackStack() },
                            onRestore = galleryViewModel::requestRestoreFromTrash,
                            onDeleteForever = galleryViewModel::requestPermanentDelete
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            settings = settings,
                            hasExtraPass = hasExtraPass,
                            onBack = { navController.popBackStack() },
                            onThemeChanged = galleryViewModel::setThemeMode,
                            onUiColorPaletteChanged = galleryViewModel::setUiColorPalette,
                            onGridDensityChanged = galleryViewModel::setDefaultGridColumns,
                            onRelockTimeoutChanged = galleryViewModel::setRelockTimeout,
                            onReduceTransparencyChanged = galleryViewModel::setReduceTransparency,
                            onOpenTrash = { navController.navigate(TrashRoute) },
                            onOpenCloudSync = { navController.navigate(CloudSyncRoute) },
                            onRequireFreshAuthForExtraPass = { onAuthed ->
                                runBiometricAuth(
                                    "Verify Identity",
                                    "Authenticate to manage Extra Secret Folder Pass"
                                ) {
                                    onAuthed()
                                }
                            },
                            onSaveExtraPass = vaultViewModel::setExtraPass,
                            onClearExtraPass = vaultViewModel::clearExtraPass
                        )
                    }

                    composable<CloudSyncRoute> {
                        CloudSyncScreen(
                            settings = settings,
                            favorites = favorites,
                            onBack = { navController.popBackStack() },
                            onApplyCloudSettings = { theme, cols, relock ->
                                galleryViewModel.setThemeMode(theme)
                                galleryViewModel.setDefaultGridColumns(cols)
                                galleryViewModel.setRelockTimeout(relock)
                                peakSuccessEvent = "Preferences Applied" to "Synced from your cloud profile"
                            }
                        )
                    }

                    composable<VaultLevel1Route> {
                        VaultGalleryScreen(
                            level = 1,
                            sections = level1Sections,
                            allItems = level1Items,
                            columns = gridColumns,
                            selectedIds = selectedVaultIds,
                            hasExtraPass = hasExtraPass,
                            imageLoader = imageLoader,
                            thumbProvider = vaultViewModel::thumbnailRequest,
                            onBackAndLock = {
                                vaultViewModel.lockAll()
                                navController.popBackStack(GalleryRoute, inclusive = false)
                            },
                            onTriggerLevel2 = {
                                vaultViewModel.refreshPassState()
                                navController.navigate(VaultPasscodeRoute)
                            },
                            onColumnsChanged = galleryViewModel::updatePinchGridColumns,
                            onOpenVaultMedia = { ref, list ->
                                val idx = list.indexOfFirst { it.id == ref.item.id }.coerceAtLeast(0)
                                activeSharedKey = "VAULT_L1_${ref.key}"
                                navController.navigate(MediaViewerRoute(source = "VAULT_L1", initialIndex = idx))
                            },
                            onToggleSelect = vaultViewModel::toggleVaultSelection,
                            onSetSelect = vaultViewModel::setVaultSelection,
                            onSelectAll = vaultViewModel::selectAllVault,
                            onClearSelection = vaultViewModel::clearVaultSelection,
                            onUnhideSelected = { ids -> vaultViewModel.unhideItems(ids) },
                            onDeleteSelectedPermanently = { ids ->
                                vaultViewModel.deleteFromVaultPermanently(ids)
                            },
                            activeSharedKey = activeSharedKey,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }

                    composable<VaultPasscodeRoute> {
                        VaultPasscodeScreen(
                            lockoutRemainingSeconds = lockoutRemaining,
                            onVerifyPasscode = vaultViewModel::verifyLevel2Passcode,
                            onSuccess = {
                                navController.navigate(VaultLevel2Route) {
                                    popUpTo<VaultPasscodeRoute> { inclusive = true }
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    composable<VaultLevel2Route> {
                        VaultGalleryScreen(
                            level = 2,
                            sections = level2Sections,
                            allItems = level2Items,
                            columns = gridColumns,
                            selectedIds = selectedVaultIds,
                            hasExtraPass = hasExtraPass,
                            imageLoader = imageLoader,
                            thumbProvider = vaultViewModel::thumbnailRequest,
                            onBackAndLock = {
                                vaultViewModel.lockLevel2Only()
                                navController.popBackStack()
                            },
                            onTriggerLevel2 = {},
                            onColumnsChanged = galleryViewModel::updatePinchGridColumns,
                            onOpenVaultMedia = { ref, list ->
                                val idx = list.indexOfFirst { it.id == ref.item.id }.coerceAtLeast(0)
                                activeSharedKey = "VAULT_L2_${ref.key}"
                                navController.navigate(MediaViewerRoute(source = "VAULT_L2", initialIndex = idx))
                            },
                            onToggleSelect = vaultViewModel::toggleVaultSelection,
                            onSetSelect = vaultViewModel::setVaultSelection,
                            onSelectAll = vaultViewModel::selectAllVault,
                            onClearSelection = vaultViewModel::clearVaultSelection,
                            onUnhideSelected = { ids -> vaultViewModel.unhideItems(ids) },
                            onDeleteSelectedPermanently = { ids ->
                                vaultViewModel.deleteFromVaultPermanently(ids)
                            },
                            activeSharedKey = activeSharedKey,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }

                    composable<MediaViewerRoute> { backStack ->
                        val route = backStack.toRoute<MediaViewerRoute>()
                        val searchResultsForViewer = if (route.source == "SEARCH") {
                            galleryViewModel.searchResults.collectAsStateWithLifecycle().value
                        } else {
                            emptyList()
                        }
                        val viewerItems: List<MediaItemRef> = remember(
                            route.source,
                            route.bucketId,
                            filteredPhotoSections,
                            favorites,
                            videos,
                            allMedia,
                            searchResultsForViewer,
                            level1Items,
                            level2Items
                        ) {
                            when (route.source) {
                                "GALLERY_PHOTOS" -> filteredPhotoSections.flatMap { it.items }
                                "GALLERY_FAVS" -> favorites.map { MediaItemRef.Gallery(it) }
                                "GALLERY_VIDEOS" -> videos.map { MediaItemRef.Gallery(it) }
                                "ALBUM" -> allMedia.filter { it.bucketId == route.bucketId }.map { MediaItemRef.Gallery(it) }
                                "SEARCH" -> searchResultsForViewer.map { MediaItemRef.Gallery(it) }
                                "VAULT_L1" -> level1Items.map { MediaItemRef.Vault(it) }
                                "VAULT_L2" -> level2Items.map { MediaItemRef.Vault(it) }
                                else -> emptyList()
                            }
                        }

                        val vaultLevel = when (route.source) {
                            "VAULT_L1" -> 1
                            "VAULT_L2" -> 2
                            else -> null
                        }

                        DisposableEffect(Unit) {
                            onDispose { galleryViewModel.clearDynamicAccent() }
                        }

                        MediaViewerScreen(
                            items = viewerItems,
                            initialIndex = route.initialIndex,
                            imageLoader = imageLoader,
                            cryptoManager = vaultViewModel.cryptoManager,
                            vaultFileProvider = vaultViewModel::getVaultEncryptedFile,
                            vaultLevel = vaultLevel,
                            dynamicAccent = dynamicAccent,
                            onClose = { navController.popBackStack() },
                            onCurrentItemChanged = { ref ->
                                if (ref != null) {
                                    activeSharedKey = "${route.source}_${ref.key}"
                                    if (route.source.startsWith("GALLERY_") && viewerItems.isNotEmpty()) {
                                        val curIdx = viewerItems.indexOfFirst { it.key == ref.key }
                                        if (curIdx >= viewerItems.size - 8) {
                                            galleryViewModel.loadNextMediaPage()
                                        }
                                    }
                                }
                                if (ref is MediaItemRef.Gallery) {
                                    galleryViewModel.updateDynamicAccentForMedia(ref.item)
                                } else {
                                    galleryViewModel.clearDynamicAccent()
                                }
                            },
                            onShareGallery = { shareGalleryMedia(listOf(it)) },
                            onToggleFavorite = galleryViewModel::toggleFavorite,
                            onEditPhoto = { item ->
                                navController.navigate(
                                    PhotoEditorRoute(
                                        uriString = item.uri.toString(),
                                        displayName = item.displayName
                                    )
                                )
                            },
                            onEditVideo = { item ->
                                navController.navigate(
                                    VideoTrimmerRoute(
                                        uriString = item.uri.toString(),
                                        displayName = item.displayName,
                                        durationMs = item.durationMs
                                    )
                                )
                            },
                            onTrashGallery = { item ->
                                galleryViewModel.requestTrash(listOf(item))
                                navController.popBackStack()
                            },
                            onRenameGallery = galleryViewModel::renameItem,
                            onSetWallpaper = galleryViewModel::setAsWallpaper,
                            onHideToSecretFolder = { item ->
                                vaultViewModel.hideGalleryItems(listOf(item))
                                navController.popBackStack()
                            },
                            onLoadGalleryDetails = galleryViewModel::getDetails,
                            onUnhideVault = { vaultItem ->
                                vaultViewModel.unhideItems(listOf(vaultItem.id)) {
                                    navController.popBackStack()
                                }
                            },
                            onMoveToDoubleHidden = { vaultItem ->
                                vaultViewModel.moveToDoubleHiddenFolder(vaultItem.id) { ok ->
                                    if (ok) navController.popBackStack()
                                }
                            },
                            onDeleteVaultPermanently = { vaultItem ->
                                vaultViewModel.deleteFromVaultPermanently(listOf(vaultItem.id)) {
                                    navController.popBackStack()
                                }
                            },
                            sharedElementPrefix = route.source,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }

                    composable<PhotoEditorRoute> { backStack ->
                        val route = backStack.toRoute<PhotoEditorRoute>()
                        var pendingOverwrite by remember { mutableStateOf<Pair<Uri, File>?>(null) }
                        var pendingDoneCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
                        val overwriteScope = rememberCoroutineScope()

                        DisposableEffect(Unit) {
                            onDispose {
                                pendingOverwrite?.second?.delete()
                            }
                        }

                        val overwriteWriteLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult()
                        ) { res ->
                            val target = pendingOverwrite
                            val done = pendingDoneCallback
                            pendingOverwrite = null
                            pendingDoneCallback = null
                            if (res.resultCode == Activity.RESULT_OK && target != null) {
                                galleryViewModel.overwriteEditedFile(target.first, target.second) { result ->
                                    target.second.delete()
                                    done?.invoke()
                                    if (result is MediaStoreRepository.OverwriteImageResult.Success) {
                                        peakSuccessEvent = "Photo Saved" to "Updated original in gallery"
                                        navController.popBackStack()
                                    }
                                }
                            } else {
                                target?.second?.delete()
                                done?.invoke()
                            }
                        }

                        PhotoEditorScreen(
                            uri = Uri.parse(route.uriString),
                            displayName = route.displayName,
                            onCancel = { navController.popBackStack() },
                            onSaveCopy = { bitmap, name, done ->
                                galleryViewModel.saveEditedBitmap(bitmap, name) { savedUri ->
                                    done()
                                    if (savedUri != null) {
                                        peakSuccessEvent = "Photo Saved" to "Saved copy to your gallery"
                                        navController.popBackStack()
                                    }
                                }
                            },
                            onOverwriteOriginal = { targetUri, bitmap, done ->
                                galleryViewModel.overwriteEditedBitmap(targetUri, bitmap) { result ->
                                    when (result) {
                                        is MediaStoreRepository.OverwriteImageResult.Success -> {
                                            done()
                                            peakSuccessEvent = "Photo Saved" to "Updated original in gallery"
                                            navController.popBackStack()
                                        }
                                        is MediaStoreRepository.OverwriteImageResult.NeedsWritePermission -> {
                                            overwriteScope.launch {
                                                val stagedFile = withContext(Dispatchers.IO) {
                                                    val outFile = File(
                                                        context.cacheDir,
                                                        "pending_overwrite_${System.currentTimeMillis()}.jpg"
                                                    )
                                                    val ok = runCatching {
                                                        FileOutputStream(outFile).use { stream ->
                                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                                                        }
                                                    }.getOrDefault(false)
                                                    if (ok && outFile.length() > 0L) {
                                                        outFile
                                                    } else {
                                                        outFile.delete()
                                                        null
                                                    }
                                                }
                                                if (stagedFile != null) {
                                                    pendingOverwrite?.second?.delete()
                                                    pendingOverwrite = targetUri to stagedFile
                                                    pendingDoneCallback = done
                                                    overwriteWriteLauncher.launch(
                                                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                                    )
                                                } else {
                                                    done()
                                                }
                                            }
                                        }
                                        is MediaStoreRepository.OverwriteImageResult.Failure -> {
                                            done()
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable<VideoTrimmerRoute> { backStack ->
                        val route = backStack.toRoute<VideoTrimmerRoute>()
                        VideoEditorScreen(
                            uri = Uri.parse(route.uriString),
                            displayName = route.displayName,
                            initialDurationMs = route.durationMs,
                            onCancel = { navController.popBackStack() },
                            onPublishExportedVideo = { file, name, done ->
                                galleryViewModel.publishExportedVideoFile(file, name) { savedUri ->
                                    done()
                                    if (savedUri != null) {
                                        peakSuccessEvent = "Video Exported" to "Saved to Piktures gallery"
                                        navController.popBackStack()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            PeakSuccessOverlay(
                visible = peakSuccessEvent != null,
                title = peakSuccessEvent?.first.orEmpty(),
                subtitle = peakSuccessEvent?.second
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
            )
            }
        }
    }
}
