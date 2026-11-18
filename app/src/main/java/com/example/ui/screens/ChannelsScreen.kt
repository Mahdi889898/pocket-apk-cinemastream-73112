package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.M3uChannel
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.PlayerRequest
import com.example.data.model.SourceType
import com.example.data.model.UiState
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGoldContainer
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StreamCyan
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    sourcesState: UiState<List<MediaSource>>,
    activePlaylistTitle: String?,
    activePlaylistChannels: List<M3uChannel>,
    isLoadingPlaylist: Boolean,
    onAddSource: (title: String, url: String, type: SourceType, category: String, rawM3u: String?) -> Unit,
    onImportM3uText: (title: String, rawText: String) -> Unit,
    onOpenPlaylist: (MediaSource) -> Unit,
    onClosePlaylist: () -> Unit,
    onDeleteSource: (String) -> Unit,
    onPlayRequest: (PlayerRequest) -> Unit,
    onOpenWebsiteInBrowser: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val m3uFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val text = context.contentResolver.openInputStream(it)
                        ?.bufferedReader()
                        ?.use { reader -> reader.readText() }
                    if (!text.isNullOrBlank()) {
                        onImportM3uText("پلی‌لیست فایل انتخابی", text)
                    }
                } catch (_: Exception) {
                }
            }
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        // Header + Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "کانال‌ها و منابع من",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "مدیریت کانال‌های زنده، لیست M3U و سایت‌های شخصی",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { m3uFileLauncher.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "*/*")) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("import_m3u_file_button"),
                ) {
                    Icon(Icons.Rounded.FileOpen, contentDescription = "انتخاب فایل M3U", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فایل M3U", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_source_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        when (sourcesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CinemaGold)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = sourcesState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LiveCrimson,
                    )
                }
            }
            is UiState.Success -> {
                val sources = sourcesState.data
                if (sources.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_sources),
                                contentDescription = "تصویر افزودن کانال",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "هنوز کانال یا پلی‌لیستی اضافه نکرده‌اید",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "با دکمه «افزودن» می‌توانید لینک کانال زنده (.m3u8)، آدرس پلی‌لیست M3U، لینک فیلم یا سایت محبوب خود را ذخیره کنید.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = ObsidianBg),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("افزودن اولین کانال یا سایت")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(sources, key = { it.id }) { source ->
                            SourceCard(
                                source = source,
                                onClick = {
                                    when (source.resolvedType()) {
                                        SourceType.PLAYLIST_M3U -> onOpenPlaylist(source)
                                        SourceType.WEBSITE -> onOpenWebsiteInBrowser(source.url)
                                        SourceType.CHANNEL -> onPlayRequest(
                                            PlayerRequest(
                                                title = source.title,
                                                streamUrl = source.url,
                                                posterUrl = source.logoUrl,
                                                sourceName = source.category,
                                                mediaType = MediaType.LIVE_CHANNEL,
                                            )
                                        )
                                        SourceType.DIRECT_MOVIE -> onPlayRequest(
                                            PlayerRequest(
                                                title = source.title,
                                                streamUrl = source.url,
                                                posterUrl = source.logoUrl,
                                                sourceName = source.category,
                                                mediaType = MediaType.MOVIE,
                                            )
                                        )
                                    }
                                },
                                onDelete = { onDeleteSource(source.id) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, url, type, category, rawM3u ->
                showAddDialog = false
                onAddSource(title, url, type, category, rawM3u)
            },
        )
    }

    if (activePlaylistTitle != null) {
        ModalBottomSheet(
            onDismissRequest = onClosePlaylist,
            containerColor = ObsidianSurface,
        ) {
            var filter by remember { mutableStateOf("") }
            val filtered = remember(activePlaylistChannels, filter) {
                if (filter.isBlank()) activePlaylistChannels
                else activePlaylistChannels.filter {
                    it.title.contains(filter, ignoreCase = true) ||
                        it.groupTitle.contains(filter, ignoreCase = true)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = activePlaylistTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = CinemaGold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    placeholder = { Text("فیلتر کانال‌ها...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (isLoadingPlaylist) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = CinemaGold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.streamUrl }) { channel ->
                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onClosePlaylist()
                                        onPlayRequest(
                                            PlayerRequest(
                                                title = channel.title,
                                                streamUrl = channel.streamUrl,
                                                posterUrl = channel.logoUrl,
                                                sourceName = activePlaylistTitle,
                                                mediaType = MediaType.LIVE_CHANNEL,
                                            )
                                        )
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(CinemaGoldContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "پخش کانال",
                                            tint = CinemaGold,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = channel.groupTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StreamCyan,
                                        )
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
}

@Composable
private fun SourceCard(
    source: MediaSource,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val type = source.resolvedType()
    val icon = when (type) {
        SourceType.CHANNEL -> Icons.Rounded.LiveTv
        SourceType.PLAYLIST_M3U -> Icons.Rounded.PlaylistPlay
        SourceType.WEBSITE -> Icons.Rounded.Language
        SourceType.DIRECT_MOVIE -> Icons.Rounded.Movie
    }
    val accent = when (type) {
        SourceType.CHANNEL -> LiveCrimson
        SourceType.PLAYLIST_M3U -> CinemaGold
        SourceType.WEBSITE -> StreamCyan
        SourceType.DIRECT_MOVIE -> CinemaGold
    }

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("source_card_${source.id}"),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = source.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        color = ObsidianElevated,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = type.persianLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "حذف منبع",
                    tint = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, url: String, type: SourceType, category: String, rawM3u: String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("فیلم و سریال") }
    var selectedType by remember { mutableStateOf(SourceType.CHANNEL) }
    var rawM3uText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = { Text("افزودن کانال، لیست یا سایت", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(SourceType.CHANNEL, SourceType.PLAYLIST_M3U, SourceType.WEBSITE).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    text = when (type) {
                                        SourceType.CHANNEL -> "کانال"
                                        SourceType.PLAYLIST_M3U -> "لیست M3U"
                                        SourceType.WEBSITE -> "سایت"
                                        SourceType.DIRECT_MOVIE -> "فیلم"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinemaGoldContainer,
                                selectedLabelColor = CinemaGold,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان (مثلاً: کانال فیلم کلاسیک)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("source_title_input"),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = {
                        Text(
                            when (selectedType) {
                                SourceType.WEBSITE -> "آدرس سایت (https://...)"
                                SourceType.PLAYLIST_M3U -> "لینک M3U (یا پایین متن را بچسبانید)"
                                else -> "لینک استریم (.m3u8 / .mp4 / .mpd)"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("source_url_input"),
                )
                if (selectedType == SourceType.PLAYLIST_M3U) {
                    OutlinedTextField(
                        value = rawM3uText,
                        onValueChange = { rawM3uText = it },
                        label = { Text("محتوای #EXTM3U (اختیاری)") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("دسته‌بندی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        url,
                        selectedType,
                        category,
                        rawM3uText.takeIf { it.isNotBlank() },
                    )
                },
                enabled = title.isNotBlank() && (url.isNotBlank() || rawM3uText.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = CinemaGold, contentColor = ObsidianBg),
                modifier = Modifier.testTag("save_source_button"),
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}
