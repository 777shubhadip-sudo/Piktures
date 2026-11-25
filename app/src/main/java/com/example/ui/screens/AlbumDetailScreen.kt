package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaItemRef
import com.example.ui.components.CollapsingGlassTopBar
import com.example.ui.components.MediaSectionsGrid
import com.example.ui.theme.LocalMotionConfig
import com.example.ui.viewmodel.DateGroupingUtils

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    items: List<GalleryMedia>,
    columns: Int,
    selectedIds: Set<Long>,
    imageLoader: ImageLoader,
    onBack: () -> Unit,
    onColumnsChanged: (Int) -> Unit,
    onOpenMedia: (MediaItemRef.Gallery, List<GalleryMedia>) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSetSelect: (Long, Boolean) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onClearSelection: () -> Unit,
    onShareSelected: (List<GalleryMedia>) -> Unit,
    onFavoriteSelected: () -> Unit,
    onHideSelected: (List<GalleryMedia>) -> Unit,
    onDeleteSelected: (List<GalleryMedia>) -> Unit,
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
        116.dp
    }

    val isScrolled by remember(gridState) {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 20 }
    }
    val sections = remember(items) { DateGroupingUtils.groupGalleryByDate(items) }
    val selectedKeys = remember(selectedIds) { selectedIds.map { "g_$it" }.toSet() }
    val selectedMedia = remember(selectedIds, items) { items.filter { selectedIds.contains(it.id) } }

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        onClearSelection()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaSectionsGrid(
            sections = sections,
            columns = columns,
            selectedKeys = selectedKeys,
            imageLoader = imageLoader,
            dataProvider = { it.item.uri },
            onColumnsChanged = onColumnsChanged,
            onItemClick = { ref, _ -> onOpenMedia(ref, items) },
            onToggleSelect = { ref -> onToggleSelect(ref.item.id) },
            onSetSelect = { ref, sel -> onSetSelect(ref.item.id, sel) },
            gridState = gridState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = contentTopPadding, bottom = 96.dp),
            sharedElementPrefix = "ALBUM",
            activeSharedKey = activeSharedKey,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )

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
                    onSelectAll = { onSelectAll(items.map { it.id }) }
                )
            } else {
                CollapsingGlassTopBar(
                    eyebrow = "ALBUM",
                    title = albumName,
                    subtitle = "${items.size} items",
                    isLargeDisplayTitle = true,
                    isScrolled = isScrolled,
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("album_back_button")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
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
