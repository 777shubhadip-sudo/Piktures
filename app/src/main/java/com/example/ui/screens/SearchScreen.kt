package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.R
import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaItemRef
import com.example.ui.components.AnimatedEmptyState
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidGlassPillChip
import com.example.ui.components.LiquidGlassSegmentedControl
import com.example.ui.components.MediaSectionsGrid
import com.example.ui.components.liquidGlassSettle
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.DateGroupingUtils
import com.example.ui.viewmodel.SearchFilterChip

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    query: String,
    activeFilter: SearchFilterChip,
    results: List<GalleryMedia>,
    columns: Int,
    imageLoader: ImageLoader,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilterChip) -> Unit,
    onBack: () -> Unit,
    onOpenMedia: (MediaItemRef.Gallery, List<GalleryMedia>) -> Unit,
    suggestedAlbums: List<String> = emptyList(),
    onColumnsChanged: (Int) -> Unit = {},
    activeSharedKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val sections = remember(results) { DateGroupingUtils.groupGalleryByDate(results) }
    val gridState = rememberLazyGridState()
    val glass = LocalLuminaColors.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val filterOptions = remember {
        SearchFilterChip.entries.map { it.name to it.label }
    }
    val quickSuggestions = remember(suggestedAlbums) {
        buildList {
            add("Today")
            add("Yesterday")
            for (album in suggestedAlbums) {
                if (album.isNotBlank() && none { it.equals(album, ignoreCase = true) }) {
                    add(album)
                }
            }
            add("Photos")
            add("Videos")
        }.distinct().take(6)
    }

    LaunchedEffect(Unit) {
        if (query.isEmpty()) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    BackHandler {
        focusManager.clearFocus()
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .liquidGlassSettle(),
            shape = CircleShape,
            strongFill = true,
            elevation = 10.dp,
            enableBackdropBlur = false
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        runCatching { focusRequester.requestFocus() }
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onBack()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("search_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = glass.glassSecondaryText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("search_input_field"),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search date, album, name, or type…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.glassSecondaryText,
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_clear_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            }
        }

        LiquidGlassSegmentedControl(
            options = filterOptions,
            selectedKey = activeFilter.name,
            onSelect = { key ->
                focusManager.clearFocus()
                SearchFilterChip.entries.firstOrNull { it.name == key }?.let(onFilterChange)
            },
            enableBackdropBlur = false,
            tagProvider = { key -> "search_chip_${key.lowercase()}" },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (query.isEmpty() && quickSuggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickSuggestions.forEach { suggestion ->
                    LiquidGlassPillChip(
                        label = suggestion,
                        selected = false,
                        onClick = {
                            focusManager.clearFocus()
                            onQueryChange(suggestion)
                        },
                        modifier = Modifier.testTag("search_suggestion_${suggestion.lowercase()}")
                    )
                }
            }
        } else if (query.isNotBlank() || activeFilter != SearchFilterChip.ALL) {
            Text(
                text = if (results.size == 1) "1 match" else "${results.size} matches",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = glass.glassSecondaryText,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("search_results_count")
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (sections.isEmpty()) {
            val hasCriteria = query.isNotBlank() || activeFilter != SearchFilterChip.ALL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                AnimatedEmptyState(
                    illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                    title = "No Matches Found",
                    subtitle = "Try searching by month (e.g. November), album name (e.g. Camera), file type (e.g. photo, video, jpg), or adjust filters.",
                    ctaLabel = if (hasCriteria) "Clear Filters" else null,
                    onCtaClick = if (hasCriteria) {
                        {
                            onQueryChange("")
                            onFilterChange(SearchFilterChip.ALL)
                        }
                    } else {
                        null
                    }
                )
            }
        } else {
            MediaSectionsGrid(
                sections = sections,
                columns = columns,
                selectedKeys = emptySet(),
                imageLoader = imageLoader,
                dataProvider = { it.item.uri },
                onColumnsChanged = onColumnsChanged,
                onItemClick = { ref, _ ->
                    focusManager.clearFocus()
                    onOpenMedia(ref, results)
                },
                onToggleSelect = {},
                onSetSelect = { _, _ -> },
                allowSelection = false,
                gridState = gridState,
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 32.dp),
                sharedElementPrefix = "SEARCH",
                activeSharedKey = activeSharedKey,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}
