package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.domain.model.AlbumFolder
import com.example.domain.model.GalleryMedia
import com.example.domain.model.GalleryTab
import com.example.domain.model.MediaItemRef
import com.example.domain.model.MediaPermissionState
import com.example.domain.model.MediaType
import com.example.ui.components.AnimatedEmptyState
import com.example.ui.components.CircularGlassIconButton
import com.example.ui.components.CollapsingGlassTopBar
import com.example.ui.components.FloatingGlassBottomBar
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassModalButton
import com.example.ui.components.LiquidGlassSegmentedControl
import com.example.ui.components.LocalHazeState
import com.example.ui.components.LocalReduceTransparency
import com.example.ui.components.MediaSectionsGrid
import com.example.ui.components.rememberSupportsLiquidBlur
import com.example.ui.theme.AbyssalNavy
import com.example.ui.theme.AnimatedFavoriteHeartIcon
import com.example.ui.theme.BlushMist
import com.example.ui.theme.DuskSlateNavy
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.SerenityBlue
import com.example.ui.theme.originAwareEntrance
import com.example.ui.theme.tactilePressMotion
import com.example.ui.theme.verticalRubberBand
import com.example.ui.viewmodel.DateGroupingUtils
import com.example.ui.viewmodel.DateSection
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Calendar

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    permissionState: MediaPermissionState,
    isLoading: Boolean = false,
    currentTab: GalleryTab,
    columns: Int,
    photoSections: List<DateSection<MediaItemRef.Gallery>>,
    filteredPhotoSections: List<DateSection<MediaItemRef.Gallery>>,
    selectedPhotoFilter: String,
    albums: List<AlbumFolder>,
    favorites: List<GalleryMedia>,
    favoriteSections: List<DateSection<MediaItemRef.Gallery>>,
    videos: List<GalleryMedia>,
    selectedIds: Set<Long>,
    imageLoader: ImageLoader,
    onRequestPermissions: () -> Unit,
    onSelectTab: (GalleryTab) -> Unit,
    onSelectPhotoFilter: (String) -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onOpenMedia: (MediaItemRef.Gallery, List<GalleryMedia>, String) -> Unit,
    onOpenAlbum: (AlbumFolder) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHiddenFolder: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSetSelect: (Long, Boolean) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onClearSelection: () -> Unit,
    onShareSelected: (List<GalleryMedia>) -> Unit,
    onFavoriteSelected: () -> Unit,
    onToggleFavoriteItem: (GalleryMedia) -> Unit = {},
    onHideSelected: (List<GalleryMedia>) -> Unit,
    onDeleteSelected: (List<GalleryMedia>) -> Unit,
    onLoadNextPage: () -> Unit = {},
    activeSharedKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val photosGridState = rememberLazyGridState()
    val albumsGridState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()

    // Spatial order matches FloatingGlassBottomBar left-to-right:
    // [ALBUMS] <-> [PHOTOS (Home)] <-> [FAVORITES] so sliding left shifts smoothly to Favorites.
    val pagerTabs = remember {
        listOf(GalleryTab.ALBUMS, GalleryTab.PHOTOS, GalleryTab.FAVORITES)
    }
    val pagerState = rememberPagerState(
        initialPage = pagerTabs.indexOf(currentTab).coerceAtLeast(0),
        pageCount = { pagerTabs.size }
    )
    val targetPage = remember(currentTab) {
        pagerTabs.indexOf(currentTab).coerceAtLeast(0)
    }
    var animatingToTarget by remember { mutableStateOf<Int?>(null) }
    val latestCurrentTab by rememberUpdatedState(currentTab)
    val latestOnSelectTab by rememberUpdatedState(onSelectTab)

    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage || pagerState.settledPage != targetPage) {
            animatingToTarget = targetPage
            try {
                pagerState.animateScrollToPage(targetPage)
            } finally {
                if (animatingToTarget == targetPage) {
                    animatingToTarget = null
                }
            }
        } else {
            animatingToTarget = null
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                val pending = animatingToTarget
                if (pending != null && settled != pending) return@collect
                if (pending == settled) {
                    animatingToTarget = null
                }
                val settledTab = pagerTabs.getOrNull(settled) ?: return@collect
                if (settledTab != latestCurrentTab) {
                    latestOnSelectTab(settledTab)
                }
            }
    }

    val displayTab = if (animatingToTarget == null && pagerState.isScrollInProgress) {
        pagerTabs.getOrNull(pagerState.currentPage) ?: currentTab
    } else {
        currentTab
    }

    val activeGridState = when (displayTab) {
        GalleryTab.PHOTOS -> photosGridState
        GalleryTab.ALBUMS -> albumsGridState
        GalleryTab.FAVORITES -> favoritesGridState
    }

    val isScrolled by remember(activeGridState) {
        derivedStateOf {
            activeGridState.firstVisibleItemIndex > 0 || activeGridState.firstVisibleItemScrollOffset > 20
        }
    }

    val primarySegments = remember {
        listOf(
            "ALL" to "All",
            "IMAGES" to "Photos",
            "VIDEOS" to "Videos"
        )
    }

    val moreAlbumOptions = remember(albums) {
        albums.take(6).map { album -> "ALBUM_${album.bucketId}" to album.name }
    }

    val photoFlatItems = remember(filteredPhotoSections) {
        filteredPhotoSections.flatMap { section -> section.items.map { it.item } }
    }

    var collectionChip by rememberSaveable { mutableStateOf("ALL") }

    val currentFlatItems = when (displayTab) {
        GalleryTab.PHOTOS -> photoFlatItems
        GalleryTab.ALBUMS -> when (collectionChip) {
            "VIDEOS" -> videos
            "FAVORITES" -> favorites
            else -> emptyList()
        }
        GalleryTab.FAVORITES -> favorites
    }

    val featuredItem = remember(favorites, filteredPhotoSections, selectedPhotoFilter) {
        if (selectedPhotoFilter != "ALL") null
        else {
            favorites.firstOrNull { it.mediaType == MediaType.IMAGE }
                ?: favorites.firstOrNull()
                ?: filteredPhotoSections.firstOrNull()?.items?.firstOrNull()?.item
        }
    }

    val deckBackingUris = remember(featuredItem, photoFlatItems) {
        if (featuredItem == null) emptyList()
        else photoFlatItems.asSequence()
            .filter { it.id != featuredItem.id }
            .map { it.uri }
            .distinct()
            .take(2)
            .toList()
    }

    val selectedMedia = remember(selectedIds, currentFlatItems) {
        if (selectedIds.isEmpty()) emptyList() else currentFlatItems.filter { selectedIds.contains(it.id) }
    }

    val selectedKeys = remember(selectedIds) {
        if (selectedIds.isEmpty()) emptySet() else selectedIds.map { "g_$it" }.toSet()
    }

    val motion = LocalMotionConfig.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeightDp = with(density) { headerHeightPx.toDp() }
    val contentTopPadding = if (headerHeightPx > 0) headerHeightDp + 6.dp else 108.dp

    val showInlinePhotoFilter = displayTab == GalleryTab.PHOTOS &&
        selectedPhotoFilter == "ALL" &&
        featuredItem != null &&
        filteredPhotoSections.isNotEmpty()

    val showTopPhotoFilter = displayTab == GalleryTab.PHOTOS &&
        photoSections.isNotEmpty() &&
        !showInlinePhotoFilter

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        onClearSelection()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (permissionState == MediaPermissionState.DENIED) {
            AnimatedEmptyState(
                illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                title = "Welcome to\nPiktures",
                subtitle = "Grant photo & video access so Piktures can organize your memories cleanly with private vault protection.",
                ctaLabel = "Allow Media Access",
                onCtaClick = onRequestPermissions
            )
        } else {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = selectedIds.isEmpty(),
                beyondViewportPageCount = 1,
                key = { pagerTabs[it].name },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (val tab = pagerTabs[page]) {
                    GalleryTab.PHOTOS -> {
                        if (filteredPhotoSections.isEmpty()) {
                            if (isLoading) {
                                MediaLoadingState(modifier = Modifier.padding(top = contentTopPadding))
                            } else {
                                AnimatedEmptyState(
                                    illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                                    title = "No Photos Yet",
                                    subtitle = "Photos and videos on your device will appear here automatically.",
                                    modifier = Modifier.padding(top = contentTopPadding * 0.5f)
                                )
                            }
                        } else {
                            MediaSectionsGrid(
                                sections = filteredPhotoSections,
                                columns = columns,
                                selectedKeys = selectedKeys,
                                imageLoader = imageLoader,
                                dataProvider = { it.item.uri },
                                onColumnsChanged = onColumnsChanged,
                                onItemClick = { ref, _ -> onOpenMedia(ref, photoFlatItems, "GALLERY_PHOTOS") },
                                onToggleSelect = { ref -> onToggleSelect(ref.item.id) },
                                onSetSelect = { ref, sel -> onSetSelect(ref.item.id, sel) },
                                gridState = photosGridState,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = contentTopPadding,
                                    bottom = 116.dp
                                ),
                                sharedElementPrefix = "GALLERY_PHOTOS",
                                activeSharedKey = activeSharedKey,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = if (tab == displayTab) animatedVisibilityScope else null,
                                onNearEnd = onLoadNextPage,
                                headerContent = if (featuredItem != null) {
                                    {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    alpha = if (selectedIds.isEmpty()) 1f else 0.55f
                                                },
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            PikturesGreetingHeroSection(
                                                item = featuredItem,
                                                backingUris = deckBackingUris,
                                                totalPhotosCount = photoFlatItems.size,
                                                imageLoader = imageLoader,
                                                onOpenFeatured = {
                                                    if (selectedIds.isEmpty()) {
                                                        onOpenMedia(
                                                            MediaItemRef.Gallery(featuredItem),
                                                            photoFlatItems,
                                                            "GALLERY_PHOTOS"
                                                        )
                                                    }
                                                },
                                                onToggleFavorite = {
                                                    if (selectedIds.isEmpty()) {
                                                        onToggleFavoriteItem(featuredItem)
                                                    }
                                                }
                                            )
                                            LiquidGlassSegmentedControl(
                                                options = primarySegments,
                                                selectedKey = selectedPhotoFilter,
                                                onSelect = { filter ->
                                                    if (selectedIds.isEmpty()) {
                                                        onSelectPhotoFilter(filter)
                                                    }
                                                },
                                                moreOptions = moreAlbumOptions,
                                                enableBackdropBlur = false
                                            )
                                        }
                                    }
                                } else null
                            )
                        }
                    }

                    GalleryTab.ALBUMS -> {
                        if (albums.isEmpty()) {
                            if (isLoading) {
                                MediaLoadingState(modifier = Modifier.padding(top = contentTopPadding))
                            } else {
                                AnimatedEmptyState(
                                    illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                                    title = "No Collections Yet",
                                    subtitle = "Folders containing photos or videos will be organized here.",
                                    modifier = Modifier.padding(top = contentTopPadding * 0.5f)
                                )
                            }
                        } else {
                            AlbumsGrid(
                                albums = albums,
                                favorites = favorites,
                                favoriteSections = favoriteSections,
                                videos = videos,
                                favoritesCount = favorites.size,
                                videosCount = videos.size,
                                selectedChip = collectionChip,
                                onSelectChip = { chip ->
                                    if (selectedIds.isEmpty()) {
                                        collectionChip = chip
                                    }
                                },
                                columns = columns,
                                selectedKeys = selectedKeys,
                                onColumnsChanged = onColumnsChanged,
                                onToggleSelect = { ref -> onToggleSelect(ref.item.id) },
                                onSetSelect = { ref, sel -> onSetSelect(ref.item.id, sel) },
                                imageLoader = imageLoader,
                                gridState = albumsGridState,
                                topPadding = contentTopPadding,
                                onSearchClick = onOpenSearch,
                                onAlbumClick = onOpenAlbum,
                                onOpenFavoriteMedia = { favItem ->
                                    onOpenMedia(MediaItemRef.Gallery(favItem), favorites, "GALLERY_FAVS")
                                },
                                onOpenFavoriteRef = { ref ->
                                    onOpenMedia(ref, favorites, "GALLERY_FAVS")
                                },
                                onOpenVideoRef = { ref ->
                                    onOpenMedia(ref, videos, "GALLERY_VIDEOS")
                                },
                                activeSharedKey = activeSharedKey,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = if (tab == displayTab) animatedVisibilityScope else null
                            )
                        }
                    }

                    GalleryTab.FAVORITES -> {
                        if (favoriteSections.isEmpty()) {
                            AnimatedEmptyState(
                                illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                                title = "No Favorites Yet",
                                subtitle = "Tap the heart on any photo or video to keep your best moments close.",
                                modifier = Modifier.padding(top = contentTopPadding * 0.5f)
                            )
                        } else {
                            MediaSectionsGrid(
                                sections = favoriteSections,
                                columns = columns,
                                selectedKeys = selectedKeys,
                                imageLoader = imageLoader,
                                dataProvider = { it.item.uri },
                                onColumnsChanged = onColumnsChanged,
                                onItemClick = { ref, _ -> onOpenMedia(ref, favorites, "GALLERY_FAVS") },
                                onToggleSelect = { ref -> onToggleSelect(ref.item.id) },
                                onSetSelect = { ref, sel -> onSetSelect(ref.item.id, sel) },
                                gridState = favoritesGridState,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = contentTopPadding,
                                    bottom = 116.dp
                                ),
                                sharedElementPrefix = "GALLERY_FAVS",
                                activeSharedKey = activeSharedKey,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = if (tab == displayTab) animatedVisibilityScope else null,
                                onNearEnd = onLoadNextPage
                            )
                        }
                    }
                }
            }

            // Floating Top Header
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height }
            ) {
                if (selectedIds.isNotEmpty()) {
                    SelectionTopBar(
                        selectedCount = selectedIds.size,
                        onClear = onClearSelection,
                        onSelectAll = { onSelectAll(currentFlatItems.map { it.id }) }
                    )
                } else {
                    val glass = LocalLuminaColors.current
                    val reduceTransparency = LocalReduceTransparency.current
                    val actionsBlock: @Composable () -> Unit = {
                        CircularGlassIconButton(
                            icon = Icons.Rounded.Search,
                            contentDescription = "Search media",
                            onClick = onOpenSearch,
                            size = 42.dp,
                            modifier = Modifier.testTag("search_button")
                        )

                        Box {
                            CircularGlassIconButton(
                                icon = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                onClick = { menuExpanded = true },
                                size = 42.dp,
                                modifier = Modifier.testTag("overflow_menu_button")
                            )
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                shape = RoundedCornerShape(22.dp),
                                shadowElevation = 18.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (reduceTransparency) glass.glassBorder else glass.prismBorderStart.copy(alpha = 0.72f)
                                ),
                                containerColor = if (reduceTransparency) glass.solidFallbackElevated else glass.glassStrongFill
                            ) {
                                Column(
                                    modifier = Modifier.originAwareEntrance(TransformOrigin(0.95f, 0f))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Hidden Folder") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onOpenHiddenFolder()
                                        },
                                        modifier = Modifier.testTag("menu_hidden_folder")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Trash") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onOpenTrash()
                                        },
                                        modifier = Modifier.testTag("menu_trash")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Settings, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onOpenSettings()
                                        },
                                        modifier = Modifier.testTag("menu_settings")
                                    )
                                }
                            }
                        }
                    }

                    if (displayTab == GalleryTab.PHOTOS) {
                        PikturesHomeTopBar(
                            isScrolled = isScrolled,
                            actions = { actionsBlock() }
                        )
                    } else {
                        CollapsingGlassTopBar(
                            eyebrow = "Piktures",
                            title = if (displayTab == GalleryTab.ALBUMS) "Collection" else "Favorites",
                            isLargeDisplayTitle = true,
                            isScrolled = isScrolled,
                            navigationIcon = {
                                IconButton(
                                    onClick = { onSelectTab(GalleryTab.PHOTOS) },
                                    modifier = Modifier.testTag("tab_back_home_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to Gallery"
                                    )
                                }
                            },
                            actions = { actionsBlock() }
                        )
                    }
                }

                if (showTopPhotoFilter) {
                    LiquidGlassSegmentedControl(
                        options = primarySegments,
                        selectedKey = selectedPhotoFilter,
                        onSelect = { filter ->
                            if (selectedIds.isEmpty()) {
                                onSelectPhotoFilter(filter)
                            }
                        },
                        moreOptions = moreAlbumOptions,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
                            .graphicsLayer {
                                alpha = if (selectedIds.isEmpty()) 1f else 0.55f
                            }
                    )
                }

                if (permissionState == MediaPermissionState.PARTIAL_ACCESS) {
                    PartialAccessBanner(onSelectMore = onRequestPermissions)
                }
            }

            // 3-part Floating Bottom Dock OR Contextual Selection Bar
            AnimatedVisibility(
                visible = selectedIds.isEmpty(),
                enter = motion.bottomSheetEnter(),
                exit = motion.bottomSheetExit(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingGlassBottomBar(
                    currentTab = displayTab,
                    onSelectTab = onSelectTab,
                    onOpenQuickMenu = { showQuickMenu = true }
                )
            }

            AnimatedVisibility(
                visible = selectedIds.isNotEmpty(),
                enter = motion.bottomSheetEnter(),
                exit = motion.bottomSheetExit(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SelectionActionPillBar(
                    onShare = { onShareSelected(selectedMedia) },
                    onFavorite = onFavoriteSelected,
                    onHide = { onHideSelected(selectedMedia) },
                    onDelete = { onDeleteSelected(selectedMedia) }
                )
            }
        }
    }

    if (showQuickMenu) {
        PikturesQuickActionsDialog(
            onDismiss = { showQuickMenu = false },
            onSearch = {
                showQuickMenu = false
                onOpenSearch()
            },
            onHiddenFolder = {
                showQuickMenu = false
                onOpenHiddenFolder()
            },
            onTrash = {
                showQuickMenu = false
                onOpenTrash()
            },
            onSettings = {
                showQuickMenu = false
                onOpenSettings()
            }
        )
    }
}

@Composable
private fun PikturesHomeTopBar(
    isScrolled: Boolean,
    actions: @Composable () -> Unit
) {
    val glass = LocalLuminaColors.current
    val hazeState = LocalHazeState.current
    val reduceTransparency = LocalReduceTransparency.current
    val realBlurEnabled = !reduceTransparency && rememberSupportsLiquidBlur() && hazeState != null
    val topBarStyle = remember(glass) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 20.dp,
            noiseFactor = 0f,
            tints = listOf(HazeTint(glass.glassFillBottom.copy(alpha = if (glass.isDark) 0.44f else 0.56f))),
            fallbackTint = HazeTint(glass.glassStrongFill)
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (isScrolled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        when {
                            reduceTransparency -> Modifier.background(glass.solidFallbackSurface)
                            realBlurEnabled && hazeState != null -> Modifier.hazeEffect(state = hazeState, style = topBarStyle) {
                                blurEnabled = true
                            }
                            else -> Modifier.background(glass.glassStrongFill)
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(glass.glassBorder.copy(alpha = 0.60f))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "piktures.",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.03).em
                ),
                color = glass.editorialGold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions()
            }
        }
    }
}

@Composable
private fun PikturesGreetingHeroSection(
    item: GalleryMedia,
    backingUris: List<Uri>,
    totalPhotosCount: Int,
    imageLoader: ImageLoader,
    onOpenFeatured: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val glass = LocalLuminaColors.current
    val greetingWord = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Morning"
            hour < 17 -> "Afternoon"
            else -> "Evening"
        }
    }
    val primaryAccent = MaterialTheme.colorScheme.primary
    val textAccent = glass.editorialGold
    val goodBrush = remember(glass.isDark, primaryAccent, textAccent, glass.glassTopEdgeHighlight) {
        if (glass.isDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    glass.glassTopEdgeHighlight
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    primaryAccent.copy(alpha = 0.42f),
                    textAccent
                )
            )
        }
    }
    val greetingBrush = remember(glass.isDark, primaryAccent, textAccent, glass.dispersionWarm) {
        if (glass.isDark) {
            Brush.verticalGradient(listOf(glass.dispersionWarm, textAccent))
        } else {
            Brush.verticalGradient(listOf(primaryAccent, textAccent))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 14.dp)
        ) {
            Text(
                text = "Good",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-0.03).em,
                    brush = goodBrush
                )
            )
            Text(
                text = greetingWord,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    lineHeight = 46.sp,
                    letterSpacing = (-0.03).em,
                    brush = greetingBrush
                ),
                modifier = Modifier.offset(y = (-6).dp)
            )
        }

        // Stacked fanned card peek behind the primary featured card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Back left & right tilted glass card peeks
            val leftTint = if (glass.isDark) MidnightNavy else SerenityBlue.copy(alpha = 0.65f)
            val rightTint = if (glass.isDark) DuskSlateNavy else BlushMist
            if (backingUris.isNotEmpty()) {
                val ctx = LocalContext.current
                val leftReq = remember(backingUris[0]) {
                    ImageRequest.Builder(ctx).data(backingUris[0]).size(256).crossfade(false).build()
                }
                AsyncImage(
                    model = leftReq,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(54.dp)
                        .offset(y = (-8).dp)
                        .graphicsLayer { rotationZ = -2.8f }
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, glass.prismBorderStart.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .height(48.dp)
                        .offset(y = (-8).dp)
                        .graphicsLayer { rotationZ = -2.6f }
                        .clip(RoundedCornerShape(24.dp))
                        .background(leftTint)
                )
            }

            if (backingUris.size >= 2) {
                val ctx = LocalContext.current
                val rightReq = remember(backingUris[1]) {
                    ImageRequest.Builder(ctx).data(backingUris[1]).size(256).crossfade(false).build()
                }
                AsyncImage(
                    model = rightReq,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .height(54.dp)
                        .offset(y = (-3).dp)
                        .graphicsLayer { rotationZ = 2.2f }
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, glass.prismBorderStart.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .height(48.dp)
                        .offset(y = (-3).dp)
                        .graphicsLayer { rotationZ = 2.0f }
                        .clip(RoundedCornerShape(24.dp))
                        .background(rightTint)
                )
            }

            FeaturedMediaCard(
                item = item,
                totalPhotosCount = totalPhotosCount,
                imageLoader = imageLoader,
                onOpenFeatured = onOpenFeatured,
                onToggleFavorite = onToggleFavorite,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun FeaturedMediaCard(
    item: GalleryMedia,
    totalPhotosCount: Int,
    imageLoader: ImageLoader,
    onOpenFeatured: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glass = LocalLuminaColors.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(30.dp)
    val placeholderPainter = remember(glass.shimmerBase) { ColorPainter(glass.shimmerBase) }

    val request = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .size(640)
            .crossfade(false)
            .build()
    }

    val subtitleLabel = remember(item) {
        val dateText = DateGroupingUtils.formatShortDate(item.effectiveDateMs)
        val bucket = item.bucketName.ifBlank { "Memory" }
        "$bucket · $dateText"
    }

    val cardInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(264.dp)
            .tactilePressMotion(cardInteraction, squashAmount = 0.03f)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = glass.accentGlow.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(shape)
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(glass.prismBorderStart, glass.prismBorderMid, glass.prismBorderEnd)
                ),
                shape = shape
            )
            .clickable(
                interactionSource = cardInteraction,
                indication = LocalIndication.current,
                onClick = onOpenFeatured
            )
            .testTag("featured_card")
    ) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        0.76f to Color(0x88060B16),
                        1.0f to Color(0xDC050912)
                    )
                )
        )

        // Top-left frosted optical glass badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color(0xFF0A101E).copy(alpha = 0.34f)
                        )
                    )
                )
                .border(0.9.dp, Color.White.copy(alpha = 0.56f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = "FEATURED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = Color.White
            )
        }

        // Frosted translucent optical dock inside featured card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.24f),
                            Color(0xFF0A101E).copy(alpha = 0.48f)
                        )
                    )
                )
                .border(
                    width = 1.1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.68f), glass.dispersionCool.copy(alpha = 0.42f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subtitleLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.displayName.substringBeforeLast('.').ifBlank { "Curated Moment" },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 24.sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val favInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .tactilePressMotion(favInteraction, squashAmount = 0.08f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.58f), CircleShape)
                        .clickable(
                            interactionSource = favInteraction,
                            indication = LocalIndication.current
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFavorite()
                        }
                        .testTag("featured_favorite_button"),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedFavoriteHeartIcon(
                        isFavorite = item.isFavorite,
                        contentDescription = if (item.isFavorite) "Remove favorite" else "Add favorite",
                        inactiveTint = Color.White,
                        iconSize = 20.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun PartialAccessBanner(onSelectMore: () -> Unit) {
    val glass = LocalLuminaColors.current
    val accent = glass.editorialGold
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Limited access enabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onSelectMore,
                colors = ButtonDefaults.textButtonColors(contentColor = accent)
            ) {
                Text("Select more", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = CircleShape,
        strongFill = true,
        elevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClear, modifier = Modifier.testTag("clear_selection_button")) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel selection")
            }
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSelectAll, modifier = Modifier.testTag("select_all_button")) {
                Icon(Icons.Rounded.DoneAll, contentDescription = "Select all")
            }
        }
    }
}

@Composable
fun SelectionActionPillBar(
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .height(64.dp),
            shape = CircleShape,
            strongFill = true,
            elevation = 18.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shareInt = remember { MutableInteractionSource() }
                val favInt = remember { MutableInteractionSource() }
                val hideInt = remember { MutableInteractionSource() }
                val delInt = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onShare,
                    interactionSource = shareInt,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .tactilePressMotion(shareInt)
                        .testTag("batch_share_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share selected")
                }
                IconButton(
                    onClick = onFavorite,
                    interactionSource = favInt,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .tactilePressMotion(favInt)
                        .testTag("batch_favorite_button")
                ) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Favorite selected")
                }
                IconButton(
                    onClick = onHide,
                    interactionSource = hideInt,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .tactilePressMotion(hideInt)
                        .testTag("batch_hide_button")
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Move to Hidden Folder")
                }
                IconButton(
                    onClick = onDelete,
                    interactionSource = delInt,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .tactilePressMotion(delInt)
                        .testTag("batch_delete_button")
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete selected",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 84.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            shape = CircleShape,
            strongFill = true,
            elevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Loading media...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AlbumsGrid(
    albums: List<AlbumFolder>,
    favorites: List<GalleryMedia>,
    favoriteSections: List<DateSection<MediaItemRef.Gallery>>,
    videos: List<GalleryMedia>,
    favoritesCount: Int,
    videosCount: Int,
    selectedChip: String,
    onSelectChip: (String) -> Unit,
    columns: Int,
    selectedKeys: Set<String>,
    onColumnsChanged: (Int) -> Unit,
    onToggleSelect: (MediaItemRef.Gallery) -> Unit,
    onSetSelect: (MediaItemRef.Gallery, Boolean) -> Unit,
    imageLoader: ImageLoader,
    gridState: LazyGridState,
    topPadding: Dp,
    onSearchClick: () -> Unit,
    onAlbumClick: (AlbumFolder) -> Unit,
    onOpenFavoriteMedia: (GalleryMedia) -> Unit,
    onOpenFavoriteRef: (MediaItemRef.Gallery) -> Unit,
    onOpenVideoRef: (MediaItemRef.Gallery) -> Unit,
    activeSharedKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val glass = LocalLuminaColors.current
    val hazeState = LocalHazeState.current
    val placeholderColor = glass.shimmerBase
    val placeholderPainter = remember(placeholderColor) { ColorPainter(placeholderColor) }

    val videoSections = remember(videos) { DateGroupingUtils.groupGalleryByDate(videos) }
    val videosGridState = rememberLazyGridState()
    val collectionFavsGridState = rememberLazyGridState()

    val heroAlbum = remember(albums) {
        albums.maxByOrNull { it.itemCount } ?: albums.firstOrNull()
    }
    val showcaseFavorite = remember(favorites) {
        favorites.firstOrNull { it.mediaType == MediaType.IMAGE } ?: favorites.firstOrNull()
    }
    val placesAlbum = remember(albums) {
        albums.firstOrNull {
            it.name.contains("camera", ignoreCase = true) ||
                it.name.contains("place", ignoreCase = true) ||
                it.name.contains("travel", ignoreCase = true) ||
                it.name.contains("dcim", ignoreCase = true)
        }
    }

    val filterPillsRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CollectionFilterChip(
                label = "All",
                selected = selectedChip == "ALL",
                onClick = { onSelectChip("ALL") }
            )
            CollectionFilterChip(
                label = "Albums",
                selected = selectedChip == "ALBUMS",
                onClick = { onSelectChip("ALBUMS") }
            )
            CollectionFilterChip(
                label = "Favorites",
                selected = selectedChip == "FAVORITES",
                onClick = { onSelectChip("FAVORITES") }
            )
            CollectionFilterChip(
                label = "Videos",
                selected = selectedChip == "VIDEOS",
                onClick = { onSelectChip("VIDEOS") }
            )
        }
    }

    if (selectedChip == "VIDEOS" || selectedChip == "FAVORITES") {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = 10.dp)
            ) {
                filterPillsRow()
            }
            if (selectedChip == "VIDEOS") {
                if (videoSections.isEmpty()) {
                    AnimatedEmptyState(
                        illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                        title = "No Videos Yet",
                        subtitle = "Videos on your device will appear here right inside Collection.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MediaSectionsGrid(
                        sections = videoSections,
                        columns = columns,
                        selectedKeys = selectedKeys,
                        imageLoader = imageLoader,
                        dataProvider = { it.item.uri },
                        onColumnsChanged = onColumnsChanged,
                        onItemClick = { ref, _ -> onOpenVideoRef(ref) },
                        onToggleSelect = onToggleSelect,
                        onSetSelect = onSetSelect,
                        gridState = videosGridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 116.dp),
                        sharedElementPrefix = "GALLERY_VIDEOS",
                        activeSharedKey = activeSharedKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                if (favoriteSections.isEmpty()) {
                    AnimatedEmptyState(
                        illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                        title = "No Favorites Yet",
                        subtitle = "Tap the heart on any photo or video to keep your best moments close.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MediaSectionsGrid(
                        sections = favoriteSections,
                        columns = columns,
                        selectedKeys = selectedKeys,
                        imageLoader = imageLoader,
                        dataProvider = { it.item.uri },
                        onColumnsChanged = onColumnsChanged,
                        onItemClick = { ref, _ -> onOpenFavoriteRef(ref) },
                        onToggleSelect = onToggleSelect,
                        onSetSelect = onSetSelect,
                        gridState = collectionFavsGridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 116.dp),
                        sharedElementPrefix = "GALLERY_FAVS",
                        activeSharedKey = activeSharedKey,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (hazeState != null) {
                    Modifier.hazeSource(state = hazeState, zIndex = 1f)
                } else {
                    Modifier
                }
            )
    ) {
        val gridColumns = if (maxWidth < 600.dp) {
            GridCells.Fixed(2)
        } else {
            GridCells.Adaptive(minSize = 180.dp)
        }

        LazyVerticalGrid(
            columns = gridColumns,
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 116.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalRubberBand()
                .testTag("albums_grid")
        ) {
            // Horizontal Collection Filter Pills (matching Bottom-Right screen)
            item(
                key = "collection_filter_pills",
                contentType = "filter_pills",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                filterPillsRow()
            }

            // Editorial Fanned 3-Card Story Deck + "Every Event Deserves Its Own Story!" White Quick Card
            if (selectedChip == "ALL" && heroAlbum != null) {
                item(
                    key = "hero_album_${heroAlbum.bucketId}",
                    contentType = "hero_album",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HeroAlbumCard(
                            albums = albums,
                            heroAlbum = heroAlbum,
                            imageLoader = imageLoader,
                            onClick = { onAlbumClick(heroAlbum) }
                        )

                        // Editorial mixed-italic heading
                        val emphasisAccent = glass.editorialGold
                        val headingText = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = glass.glassSecondaryText,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append("Every ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = emphasisAccent,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append("Event Deserves")
                            }
                            withStyle(
                                SpanStyle(
                                    color = glass.glassSecondaryText,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append("\nIts ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = emphasisAccent,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append("Own Story!")
                            }
                        }

                        Text(
                            text = headingText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = OutfitFontFamily,
                                fontSize = 25.sp,
                                lineHeight = 30.sp
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )

                        // Liquid Glass Quick-Actions Bento Card + Luminous Optical CTA Pill
                        GlassSurface(
                            shape = RoundedCornerShape(28.dp),
                            strongFill = false,
                            elevation = 12.dp,
                            enableBackdropBlur = false,
                            cornerRadiusHint = 28.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                QuickStoryActionRow(
                                    icon = Icons.Rounded.Search,
                                    label = "Search Memories & Dates",
                                    onClick = onSearchClick,
                                    modifier = Modifier.testTag("albums_search_bar")
                                )
                                QuickStoryActionRow(
                                    icon = Icons.Rounded.FavoriteBorder,
                                    label = "Favorites ($favoritesCount)",
                                    onClick = { onSelectChip("FAVORITES") },
                                    modifier = Modifier.testTag("smart_row_favorites")
                                )
                                QuickStoryActionRow(
                                    icon = Icons.Rounded.Videocam,
                                    label = "Video Reels ($videosCount)",
                                    onClick = { onSelectChip("VIDEOS") },
                                    modifier = Modifier.testTag("smart_row_videos")
                                )
                                QuickStoryActionRow(
                                    icon = Icons.Rounded.LocationOn,
                                    label = if (placesAlbum != null) "Places · ${placesAlbum.name}" else "Browse Folders",
                                    onClick = {
                                        if (placesAlbum != null) onAlbumClick(placesAlbum) else onSearchClick()
                                    },
                                    modifier = Modifier.testTag("smart_row_places")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val ctaInteraction = remember { MutableInteractionSource() }
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
                                val secondaryColor = if (onPrimaryColor == Color.White) {
                                    primaryColor.copy(alpha = 0.86f)
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                                val ctaBrush = remember(primaryColor, secondaryColor) {
                                    Brush.horizontalGradient(
                                        listOf(primaryColor, secondaryColor)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .tactilePressMotion(ctaInteraction, squashAmount = 0.03f)
                                        .shadow(
                                            elevation = 10.dp,
                                            shape = CircleShape,
                                            ambientColor = primaryColor.copy(alpha = 0.35f),
                                            spotColor = primaryColor.copy(alpha = 0.35f)
                                        )
                                        .clip(CircleShape)
                                        .background(ctaBrush)
                                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                        .clickable(
                                            interactionSource = ctaInteraction,
                                            indication = LocalIndication.current,
                                            role = Role.Button
                                        ) { onAlbumClick(heroAlbum) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Open ${heroAlbum.name}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }

                        // Large "Favourite"-style Collection Highlight Card
                        val highlightUri = showcaseFavorite?.uri ?: heroAlbum.coverUri
                        val highlightTitle = showcaseFavorite?.displayName?.substringBeforeLast('.')
                            ?: heroAlbum.name
                        val highlightSub = if (showcaseFavorite != null) {
                            "${showcaseFavorite.bucketName} · ${DateGroupingUtils.formatShortDate(showcaseFavorite.effectiveDateMs)}"
                        } else {
                            "${heroAlbum.itemCount} curated photos & videos in this collection"
                        }
                        FavouriteShowcaseCard(
                            badgeLabel = if (showcaseFavorite != null) "Favourite" else "Featured",
                            title = highlightTitle,
                            subtitle = highlightSub,
                            imageUri = highlightUri,
                            imageLoader = imageLoader,
                            onClick = {
                                if (showcaseFavorite != null) onOpenFavoriteMedia(showcaseFavorite)
                                else onAlbumClick(heroAlbum)
                            }
                        )
                    }
                }
            }

            // 2-Column Bento Album Cards with Liquid Glass Footer Plate
            itemsIndexed(
                items = albums,
                key = { _, it -> it.bucketId },
                contentType = { _, _ -> "album_card" }
            ) { _, album ->
                val request = remember(album.coverUri) {
                    ImageRequest.Builder(context)
                        .data(album.coverUri)
                        .size(320)
                        .crossfade(false)
                        .build()
                }
                val cardShape = remember { RoundedCornerShape(26.dp) }
                val albumInteraction = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.86f)
                        .tactilePressMotion(albumInteraction, squashAmount = 0.04f)
                        .shadow(
                            elevation = 12.dp,
                            shape = cardShape,
                            ambientColor = glass.accentGlow.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.18f)
                        )
                        .clip(cardShape)
                        .background(placeholderColor)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(glass.prismBorderStart, glass.prismBorderMid)
                            ),
                            shape = cardShape
                        )
                        .testTag("album_card_${album.bucketId}")
                        .clickable(
                            interactionSource = albumInteraction,
                            indication = LocalIndication.current
                        ) { onAlbumClick(album) }
                ) {
                    AsyncImage(
                        model = request,
                        imageLoader = imageLoader,
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.40f)
                                    )
                                )
                            )
                    )

                    // Frosted translucent Liquid Glass footer plate
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(10.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.24f),
                                        Color(0xFF0A101E).copy(alpha = 0.34f)
                                    )
                                )
                            )
                            .border(0.95.dp, Color.White.copy(alpha = 0.54f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${album.itemCount}+",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.92f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val glass = LocalLuminaColors.current
    val reduceTransparency = LocalReduceTransparency.current
    val interaction = remember { MutableInteractionSource() }
    val bgBrush = when {
        reduceTransparency && selected -> Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
        )
        reduceTransparency -> Brush.verticalGradient(
            listOf(glass.solidFallbackElevated, glass.solidFallbackElevated)
        )
        selected && !glass.isDark -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.68f))
        )
        selected -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.36f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                glass.glassFillTop.copy(alpha = if (glass.isDark) 0.14f else 0.46f),
                glass.glassFillBottom.copy(alpha = if (glass.isDark) 0.26f else 0.32f)
            )
        )
    }
    val textColor = when {
        reduceTransparency && selected -> MaterialTheme.colorScheme.onPrimary
        selected && !glass.isDark -> MaterialTheme.colorScheme.onBackground
        selected -> Color.White
        else -> glass.glassSecondaryText
    }

    Box(
        modifier = Modifier
            .height(40.dp)
            .minimumInteractiveComponentSize()
            .tactilePressMotion(interaction, squashAmount = 0.045f)
            .then(
                if (selected && !glass.isDark) {
                    Modifier.shadow(5.dp, CircleShape, ambientColor = glass.accentGlow.copy(alpha = 0.18f))
                } else Modifier
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (selected) {
                    if (glass.isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) else Color.White
                } else {
                    glass.glassBorder.copy(alpha = 0.72f)
                },
                shape = CircleShape
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickStoryActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLuminaColors.current
    val reduceTransparency = LocalReduceTransparency.current
    val interaction = remember { MutableInteractionSource() }
    val rowBg = if (reduceTransparency) {
        glass.solidFallbackElevated
    } else {
        glass.glassFillTop.copy(alpha = if (glass.isDark) 0.12f else 0.54f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .tactilePressMotion(interaction, squashAmount = 0.025f)
            .clip(RoundedCornerShape(18.dp))
            .background(rowBg)
            .border(
                width = 0.85.dp,
                color = if (reduceTransparency) glass.glassBorder else glass.prismBorderStart.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            glass.dispersionCool.copy(alpha = 0.14f)
                        )
                    )
                )
                .border(0.85.dp, glass.prismBorderStart.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glass.editorialGold,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = glass.glassSecondaryText.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FavouriteShowcaseCard(
    badgeLabel: String,
    title: String,
    subtitle: String,
    imageUri: Uri,
    imageLoader: ImageLoader,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalLuminaColors.current
    val shape = RoundedCornerShape(28.dp)
    val placeholderPainter = remember(glass.shimmerBase) { ColorPainter(glass.shimmerBase) }
    val request = remember(imageUri) {
        ImageRequest.Builder(context).data(imageUri).size(560).crossfade(false).build()
    }
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(258.dp)
            .tactilePressMotion(interaction, squashAmount = 0.03f)
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = glass.accentGlow.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.24f)
            )
            .clip(shape)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(glass.prismBorderStart, glass.prismBorderMid)
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            placeholder = placeholderPainter,
            error = placeholderPainter,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.18f),
                        0.45f to Color.Transparent,
                        0.78f to MidnightNavy.copy(alpha = 0.70f),
                        1.0f to AbyssalNavy.copy(alpha = 0.92f)
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.26f))
                    .border(0.9.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.26f))
                    .border(0.9.dp, Color.White.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NorthEast,
                    contentDescription = "Open highlight",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    lineHeight = 25.sp
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = OutfitFontFamily
                ),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroAlbumCard(
    albums: List<AlbumFolder>,
    heroAlbum: AlbumFolder,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val leftAlbum = albums.getOrNull(1) ?: heroAlbum
    val rightAlbum = albums.getOrNull(2) ?: heroAlbum
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag("hero_album_card"),
        contentAlignment = Alignment.Center
    ) {
        FannedDeckStoryCard(
            uri = leftAlbum.coverUri,
            eyebrow = "${leftAlbum.itemCount} items",
            title = leftAlbum.name,
            imageLoader = imageLoader,
            rotationDeg = -10f,
            offsetX = (-92).dp,
            offsetY = 14.dp,
            width = 132.dp,
            height = 160.dp,
            context = context,
            zOrder = 0f
        )
        FannedDeckStoryCard(
            uri = rightAlbum.coverUri,
            eyebrow = "${rightAlbum.itemCount} items",
            title = rightAlbum.name,
            imageLoader = imageLoader,
            rotationDeg = 10f,
            offsetX = 92.dp,
            offsetY = 14.dp,
            width = 132.dp,
            height = 160.dp,
            context = context,
            zOrder = 0.5f
        )
        FannedDeckStoryCard(
            uri = heroAlbum.coverUri,
            eyebrow = "Featured",
            title = heroAlbum.name,
            imageLoader = imageLoader,
            rotationDeg = 0f,
            offsetX = 0.dp,
            offsetY = (-6).dp,
            width = 148.dp,
            height = 182.dp,
            context = context,
            zOrder = 2f,
            highlightBorder = true
        )
    }
}

@Composable
private fun FannedDeckStoryCard(
    uri: Uri,
    eyebrow: String,
    title: String,
    imageLoader: ImageLoader,
    rotationDeg: Float,
    offsetX: Dp,
    offsetY: Dp,
    width: Dp,
    height: Dp,
    context: android.content.Context,
    zOrder: Float,
    highlightBorder: Boolean = false
) {
    val shape = remember { RoundedCornerShape(22.dp) }
    val request = remember(uri) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(300)
            .crossfade(false)
            .build()
    }
    Box(
        modifier = Modifier
            .zIndex(zOrder)
            .size(width = width, height = height)
            .graphicsLayer {
                rotationZ = rotationDeg
                translationX = offsetX.toPx()
                translationY = offsetY.toPx()
            }
            .shadow(10.dp, shape)
            .clip(shape)
            .border(
                width = if (highlightBorder) 1.5.dp else 1.dp,
                color = Color.White.copy(alpha = if (highlightBorder) 0.9f else 0.55f),
                shape = shape
            )
    ) {
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.50f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.72f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = OutfitFontFamily,
                    fontSize = 10.sp
                ),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PikturesQuickActionsDialog(
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onHiddenFolder: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit
) {
    LiquidGlassDialog(
        onDismissRequest = onDismiss,
        eyebrow = "PIKTURES STUDIO",
        title = "Quick Actions",
        icon = Icons.Rounded.AutoAwesome,
        origin = TransformOrigin(0.85f, 0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickStoryActionRow(
                icon = Icons.Rounded.Search,
                label = "Search Photos & Videos",
                onClick = onSearch
            )
            QuickStoryActionRow(
                icon = Icons.Rounded.VisibilityOff,
                label = "Hidden Vault Folder",
                onClick = onHiddenFolder
            )
            QuickStoryActionRow(
                icon = Icons.Rounded.DeleteOutline,
                label = "Recently Deleted Trash",
                onClick = onTrash
            )
            QuickStoryActionRow(
                icon = Icons.Rounded.Settings,
                label = "Appearance & Settings",
                onClick = onSettings
            )
        }
        LiquidGlassModalButton(
            label = "Open Search",
            leadingIcon = Icons.Rounded.Search,
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
