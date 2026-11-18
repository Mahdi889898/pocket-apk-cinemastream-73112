package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.CinemaDatabase
import com.example.data.local.CinemaRepository
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.PlayerRequest
import com.example.data.model.SourceType
import com.example.data.model.UiState
import com.example.data.remote.MediaDiscoveryService
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.WebBrowserScreen
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaGoldContainer
import com.example.ui.theme.CinemaStreamTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CinemaViewModel

private data class NavTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CinemaStreamTheme {
                    CinemaAppShell()
                }
            }
        }
    }
}

@Composable
private fun CinemaAppShell() {
    val viewModel: CinemaViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dao = CinemaDatabase.getInstance(app).cinemaDao()
                CinemaViewModel(
                    repository = CinemaRepository(dao),
                    discoveryService = MediaDiscoveryService(),
                )
            }
        },
    )

    val sourcesState by viewModel.sourcesState.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val internetMovies by viewModel.internetMovies.collectAsStateWithLifecycle()
    val internetWebPages by viewModel.internetWebPages.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchStatusText by viewModel.searchStatusText.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val activePlaylistTitle by viewModel.activePlaylistTitle.collectAsStateWithLifecycle()
    val activePlaylistChannels by viewModel.activePlaylistChannels.collectAsStateWithLifecycle()
    val isLoadingPlaylist by viewModel.isLoadingPlaylist.collectAsStateWithLifecycle()
    val browserUrl by viewModel.browserUrl.collectAsStateWithLifecycle()
    val detectedWebStreams by viewModel.detectedWebStreams.collectAsStateWithLifecycle()
    val isExtractingUrl by viewModel.isExtractingUrl.collectAsStateWithLifecycle()
    val extractedQuickStreams by viewModel.extractedQuickStreams.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val activePlayerRequest by viewModel.activePlayerRequest.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val savedSourcesList = (sourcesState as? UiState.Success<List<MediaSource>>)?.data.orEmpty()
    val libraryItemsList = (libraryState as? UiState.Success)?.data.orEmpty()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        NavTab("کشف فیلم", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_discover"),
        NavTab("کانال‌ها", Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_channels"),
        NavTab("مرورگر سایت", Icons.Filled.Language, Icons.Outlined.Language, "nav_browser"),
        NavTab("کتابخانه", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_library"),
    )

    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 700

    if (activePlayerRequest != null) {
        val req = activePlayerRequest!!
        val isFav = libraryItemsList.any { it.streamUrl == req.streamUrl && it.favorite }
        PlayerScreen(
            request = req,
            isFavorite = isFav,
            onToggleFavorite = {
                viewModel.toggleFavorite(
                    title = req.title,
                    streamUrl = req.streamUrl,
                    posterUrl = req.posterUrl,
                    sourceName = req.sourceName,
                    mediaType = req.mediaType,
                    currentlyFavorite = isFav,
                )
            },
            onSaveProgress = { pos, dur ->
                viewModel.saveWatchProgress(req, pos, dur)
            },
            onClose = { viewModel.closePlayer() },
        )
    } else {
        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(containerColor = ObsidianSurface) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = selectedTabIndex == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTabIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CinemaGold,
                                    selectedTextColor = CinemaGold,
                                    indicatorColor = CinemaGoldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isWideScreen) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = selectedTabIndex == index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTabIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CinemaGold,
                                    selectedTextColor = CinemaGold,
                                    indicatorColor = CinemaGoldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTabIndex) {
                        0 -> DiscoverScreen(
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            internetMovies = internetMovies,
                            internetWebPages = internetWebPages,
                            isSearching = isSearching,
                            searchStatusText = searchStatusText,
                            searchError = searchError,
                            isExtractingUrl = isExtractingUrl,
                            extractedQuickStreams = extractedQuickStreams,
                            savedSources = savedSourcesList,
                            libraryItems = libraryItemsList,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSearchSubmit = viewModel::searchMoviesOnline,
                            onSelectCategory = viewModel::selectCategory,
                            onPlayMovie = { movie ->
                                val existingPos = libraryItemsList
                                    .firstOrNull { it.streamUrl == movie.streamUrl }
                                    ?.lastPositionMs ?: 0L
                                viewModel.openMovie(movie, existingPos)
                            },
                            onQuickPlayUrl = { url ->
                                viewModel.extractStreamsFromAnyUrl(url, autoPlaySingle = true)
                            },
                            onPlayDetectedStream = { stream ->
                                viewModel.openPlayerRequest(
                                    PlayerRequest(
                                        title = stream.title,
                                        streamUrl = stream.url,
                                        sourceName = "پخش مستقیم",
                                        mediaType = if (stream.formatBadge == "HLS") {
                                            MediaType.LIVE_CHANNEL
                                        } else {
                                            MediaType.WEB_VIDEO
                                        },
                                    )
                                )
                            },
                            onClearExtractedStreams = viewModel::clearExtractedQuickStreams,
                            onToggleFavorite = { movie, isFav ->
                                viewModel.toggleFavorite(
                                    title = movie.title,
                                    streamUrl = movie.streamUrl,
                                    posterUrl = movie.posterUrl,
                                    sourceName = movie.sourceName,
                                    mediaType = movie.mediaType,
                                    currentlyFavorite = isFav,
                                )
                            },
                            onOpenWebsiteInBrowser = { siteUrl ->
                                viewModel.setBrowserUrl(siteUrl)
                                selectedTabIndex = 2
                            },
                        )
                        1 -> ChannelsScreen(
                            sourcesState = sourcesState,
                            activePlaylistTitle = activePlaylistTitle,
                            activePlaylistChannels = activePlaylistChannels,
                            isLoadingPlaylist = isLoadingPlaylist,
                            onAddSource = { title, url, type, category, rawM3u ->
                                viewModel.addCustomSource(
                                    title = title,
                                    url = url,
                                    sourceType = type,
                                    category = category,
                                    rawM3uContent = rawM3u,
                                )
                            },
                            onImportM3uText = { title, rawText ->
                                viewModel.importM3uFromText(title, rawText)
                            },
                            onOpenPlaylist = viewModel::openPlaylistSource,
                            onClosePlaylist = viewModel::closePlaylistViewer,
                            onDeleteSource = viewModel::deleteSource,
                            onPlayRequest = viewModel::openPlayerRequest,
                            onOpenWebsiteInBrowser = { siteUrl ->
                                viewModel.setBrowserUrl(siteUrl)
                                selectedTabIndex = 2
                            },
                        )
                        2 -> WebBrowserScreen(
                            currentUrl = browserUrl,
                            detectedStreams = detectedWebStreams,
                            savedSources = savedSourcesList,
                            onUrlChange = viewModel::setBrowserUrl,
                            onStreamDetected = viewModel::reportDetectedWebStream,
                            onClearStreams = viewModel::clearDetectedWebStreams,
                            onPlayStream = viewModel::openPlayerRequest,
                            onBookmarkSite = { title, url ->
                                viewModel.addCustomSource(
                                    title = title,
                                    url = url,
                                    sourceType = SourceType.WEBSITE,
                                    category = "وب‌سایت فیلم",
                                )
                            },
                            onSaveStreamFavorite = { stream ->
                                viewModel.toggleFavorite(
                                    title = stream.title,
                                    streamUrl = stream.url,
                                    posterUrl = null,
                                    sourceName = "مرورگر هوشمند",
                                    mediaType = MediaType.WEB_VIDEO,
                                    currentlyFavorite = false,
                                )
                            },
                        )
                        3 -> LibraryScreen(
                            libraryState = libraryState,
                            onPlayItem = viewModel::openPlayerRequest,
                            onToggleFavorite = { item ->
                                viewModel.toggleFavorite(
                                    title = item.title,
                                    streamUrl = item.streamUrl,
                                    posterUrl = item.posterUrl,
                                    sourceName = item.sourceName,
                                    mediaType = item.resolvedMediaType(),
                                    currentlyFavorite = item.favorite,
                                )
                            },
                            onDeleteItem = viewModel::deleteLibraryItem,
                        )
                    }
                }
            }
        }
    }
}
