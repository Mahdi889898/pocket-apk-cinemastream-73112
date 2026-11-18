package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LibraryItem
import com.example.data.model.PlayerRequest
import com.example.data.model.UiState
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGoldContainer
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StreamCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    libraryState: UiState<List<LibraryItem>>,
    onPlayItem: (PlayerRequest) -> Unit,
    onToggleFavorite: (LibraryItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Favorites, 1: History

    val itemsList = (libraryState as? UiState.Success)?.data.orEmpty()
    val favoritesCount = itemsList.count { it.favorite }
    val historyCount = itemsList.count { it.lastWatchedAtMs != null || it.lastPositionMs > 0L }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Local Library Header Card
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp)),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CinemaGoldContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = CinemaGold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "کتابخانه شخصی شما",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "$favoritesCount فیلم علاقه‌مند • $historyCount مورد در تاریخچه تماشا",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("علاقه‌مندی‌ها") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CinemaGoldContainer,
                    selectedLabelColor = CinemaGold,
                ),
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("تاریخچه و ادامه پخش") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CinemaGoldContainer,
                    selectedLabelColor = CinemaGold,
                ),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (libraryState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CinemaGold)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = libraryState.message, color = LiveCrimson)
                }
            }
            is UiState.Success -> {
                val filtered = remember(libraryState.data, selectedTab) {
                    if (selectedTab == 0) {
                        libraryState.data.filter { it.favorite }
                    } else {
                        libraryState.data.filter { it.lastWatchedAtMs != null || it.lastPositionMs > 0L }
                    }
                }
                if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_sources),
                            contentDescription = "کتابخانه خالی",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (selectedTab == 0) "لیست علاقه‌مندی‌ها خالی است" else "هنوز فیلمی تماشا نکرده‌اید",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "هر فیلم یا کانال را نشانه‌گذاری کنید تا همیشه در دسترس شما باشد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            LibraryItemCard(
                                item = item,
                                onPlay = {
                                    onPlayItem(
                                        PlayerRequest(
                                            title = item.title,
                                            streamUrl = item.streamUrl,
                                            posterUrl = item.posterUrl,
                                            sourceName = item.sourceName,
                                            mediaType = item.resolvedMediaType(),
                                            startPositionMs = item.lastPositionMs,
                                        )
                                    )
                                },
                                onToggleFavorite = { onToggleFavorite(item) },
                                onDelete = { onDeleteItem(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!item.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Rounded.Movie, contentDescription = null, tint = CinemaGold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${item.sourceName} • ${item.resolvedMediaType().persianLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StreamCyan,
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = if (item.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "علاقه‌مندی",
                        tint = if (item.favorite) LiveCrimson else TextSecondary,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف",
                        tint = TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CinemaGold)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "پخش", tint = ObsidianBg)
                }
            }
            if (item.durationMs > 0L && item.lastPositionMs > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = (item.lastPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    color = CinemaGold,
                    trackColor = ObsidianElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
