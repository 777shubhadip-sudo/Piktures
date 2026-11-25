package com.example.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.domain.model.GalleryMedia
import com.example.ui.components.AnimatedEmptyState
import com.example.ui.components.CollapsingGlassTopBar
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassModalButton
import com.example.ui.components.LiquidGlassModalSecondaryButton
import com.example.ui.components.LocalHazeState
import com.example.ui.components.liquidGlassSettle
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.staggeredEntrance
import com.example.ui.theme.tactilePressMotion
import com.example.ui.theme.verticalRubberBand
import com.example.ui.viewmodel.DateGroupingUtils
import dev.chrisbanes.haze.hazeSource

@Composable
fun TrashScreen(
    trashedItems: List<GalleryMedia>,
    retentionDays: Int,
    imageLoader: ImageLoader,
    onLoadTrash: () -> Unit,
    onBack: () -> Unit,
    onRestore: (List<GalleryMedia>) -> Unit,
    onDeleteForever: (List<GalleryMedia>) -> Unit
) {
    LaunchedEffect(Unit) { onLoadTrash() }

    val gridState = rememberLazyGridState()
    val glass = LocalLuminaColors.current
    val hazeState = LocalHazeState.current
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
    var confirmEmptyDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (trashedItems.isEmpty()) {
            AnimatedEmptyState(
                illustrationRes = R.drawable.img_empty_gallery_1794970584727,
                title = "Trash is Empty",
                subtitle = "Deleted photos and videos stay here for $retentionDays days before being permanently removed.",
                modifier = Modifier.padding(top = contentTopPadding * 0.5f)
            )
        } else {
            val context = LocalContext.current
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = contentTopPadding,
                    bottom = 104.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalRubberBand()
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeSource(state = hazeState, zIndex = 1f)
                        } else {
                            Modifier
                        }
                    )
            ) {
                itemsIndexed(trashedItems, key = { _, it -> it.id }) { index, item ->
                    val daysLeft = DateGroupingUtils.remainingTrashDays(item.dateExpiresSec)
                    val thumbReq = remember(item.uri) {
                        ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(256)
                            .crossfade(false)
                            .build()
                    }
                    GlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        elevation = 4.dp,
                        enableBackdropBlur = false
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            ) {
                                AsyncImage(
                                    model = thumbReq,
                                    imageLoader = imageLoader,
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                GlassSurface(
                                    shape = CircleShape,
                                    strongFill = true,
                                    elevation = 2.dp,
                                    enableBackdropBlur = false,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "${daysLeft}d left",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onRestore(listOf(item)) },
                                    modifier = Modifier.testTag("restore_trash_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Rounded.RestoreFromTrash,
                                        contentDescription = "Restore",
                                        tint = if (glass.isDark) MaterialTheme.colorScheme.primary else glass.editorialGold
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteForever(listOf(item)) },
                                    modifier = Modifier.testTag("delete_trash_${item.id}")
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteForever,
                                        contentDescription = "Delete permanently",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .liquidGlassSettle(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val restoreAllInteraction = remember { MutableInteractionSource() }
                GlassSurface(
                    shape = CircleShape,
                    strongFill = true,
                    elevation = 12.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .tactilePressMotion(restoreAllInteraction, squashAmount = 0.04f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = restoreAllInteraction,
                            indication = LocalIndication.current,
                            role = Role.Button
                        ) { onRestore(trashedItems) }
                        .testTag("restore_all_trash")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.RestoreFromTrash,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Restore All",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                val deleteAllInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { confirmEmptyDialog = true },
                    interactionSource = deleteAllInteraction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .tactilePressMotion(deleteAllInteraction, squashAmount = 0.04f)
                        .testTag("delete_all_trash")
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete All", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        CollapsingGlassTopBar(
            eyebrow = "RECENTLY DELETED",
            title = "Trash",
            subtitle = "Items permanently deleted after $retentionDays days",
            isLargeDisplayTitle = true,
            isScrolled = isScrolled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("trash_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        ) {
            if (trashedItems.isNotEmpty()) {
                val emptyInteraction = remember { MutableInteractionSource() }
                GlassSurface(
                    shape = CircleShape,
                    elevation = 6.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .tactilePressMotion(emptyInteraction, squashAmount = 0.045f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = emptyInteraction,
                            indication = LocalIndication.current,
                            role = Role.Button
                        ) { confirmEmptyDialog = true }
                        .testTag("empty_trash_button")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Empty",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (confirmEmptyDialog) {
        val glass = LocalLuminaColors.current
        LiquidGlassDialog(
            onDismissRequest = { confirmEmptyDialog = false },
            eyebrow = "PERMANENT REMOVAL",
            title = "Empty Trash?",
            icon = Icons.Rounded.DeleteForever,
            iconTint = MaterialTheme.colorScheme.error
        ) {
            Text(
                text = "All ${trashedItems.size} items in Trash will be permanently deleted and cannot be recovered.",
                style = MaterialTheme.typography.bodyMedium,
                color = glass.glassSecondaryText
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassModalSecondaryButton(
                    label = "Cancel",
                    onClick = { confirmEmptyDialog = false },
                    modifier = Modifier.weight(1f)
                )
                LiquidGlassModalButton(
                    label = "Delete Forever",
                    isDestructive = true,
                    onClick = {
                        confirmEmptyDialog = false
                        onDeleteForever(trashedItems)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
