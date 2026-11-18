package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CinemaRepository
import com.example.data.model.CatalogMovie
import com.example.data.model.DetectedWebStream
import com.example.data.model.LibraryItem
import com.example.data.model.M3uChannel
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.PlayerRequest
import com.example.data.model.SourceType
import com.example.data.model.UiState
import com.example.data.model.WebSearchPageHit
import com.example.data.remote.CuratedCatalog
import com.example.data.remote.M3uParser
import com.example.data.remote.MediaDiscoveryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CinemaViewModel(
    private val repository: CinemaRepository,
    private val discoveryService: MediaDiscoveryService,
) : ViewModel() {

    val sourcesState: StateFlow<UiState<List<MediaSource>>> =
        repository.observeSources()
            .map<List<MediaSource>, UiState<List<MediaSource>>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(error.message ?: "خطا در خواندن لیست منابع"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading,
            )

    val libraryState: StateFlow<UiState<List<LibraryItem>>> =
        repository.observeLibrary()
            .map<List<LibraryItem>, UiState<List<LibraryItem>>> { UiState.Success(it) }
            .catch { error ->
                emit(UiState.Error(error.message ?: "خطا در خواندن کتابخانه"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading,
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("همه")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _internetMovies = MutableStateFlow<List<CatalogMovie>>(emptyList())
    val internetMovies = _internetMovies.asStateFlow()

    private val _internetWebPages = MutableStateFlow<List<WebSearchPageHit>>(emptyList())
    val internetWebPages = _internetWebPages.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchStatusText = MutableStateFlow<String?>(null)
    val searchStatusText = _searchStatusText.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _activePlaylistTitle = MutableStateFlow<String?>(null)
    val activePlaylistTitle = _activePlaylistTitle.asStateFlow()

    private val _activePlaylistChannels = MutableStateFlow<List<M3uChannel>>(emptyList())
    val activePlaylistChannels = _activePlaylistChannels.asStateFlow()

    private val _isLoadingPlaylist = MutableStateFlow(false)
    val isLoadingPlaylist = _isLoadingPlaylist.asStateFlow()

    private val _browserUrl = MutableStateFlow(CuratedCatalog.starterWebsites.first().url)
    val browserUrl = _browserUrl.asStateFlow()

    private val _detectedWebStreams = MutableStateFlow<List<DetectedWebStream>>(emptyList())
    val detectedWebStreams = _detectedWebStreams.asStateFlow()

    private val _isExtractingUrl = MutableStateFlow(false)
    val isExtractingUrl = _isExtractingUrl.asStateFlow()

    private val _extractedQuickStreams = MutableStateFlow<List<DetectedWebStream>>(emptyList())
    val extractedQuickStreams = _extractedQuickStreams.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    private val _activePlayerRequest = MutableStateFlow<PlayerRequest?>(null)
    val activePlayerRequest = _activePlayerRequest.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _internetMovies.value = emptyList()
            _internetWebPages.value = emptyList()
            _searchError.value = null
            _searchStatusText.value = null
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun searchMoviesOnline() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            _internetMovies.value = emptyList()
            _internetWebPages.value = emptyList()
            _searchError.value = null
            return
        }
        val currentSources = (sourcesState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchStatusText.value = "در حال جستجو در سراسر اینترنت و استخراج لینک پخش..."
            val result = discoveryService.searchInternetForMovie(
                query = query,
                savedSources = currentSources,
                onProgress = { msg -> _searchStatusText.value = msg },
            )
            _isSearching.value = false
            _searchStatusText.value = null
            result.onSuccess { bundle ->
                _internetMovies.value = bundle.playableMovies
                _internetWebPages.value = bundle.webPages
                if (bundle.playableMovies.isEmpty() && bundle.webPages.isEmpty()) {
                    _searchError.value = "نتیجه مستقیمی پیدا نشد. می‌توانید جستجوی مستقیم در مرورگر هوشمند را بزنید."
                }
            }.onFailure { err ->
                _searchError.value = "جستجوی آنلاین با خطا مواجه شد: ${err.localizedMessage ?: "عدم دسترسی"}"
            }
        }
    }

    fun openMovie(movie: CatalogMovie, resumePosMs: Long = 0L) {
        viewModelScope.launch {
            var targetUrl = movie.streamUrl
            if (movie.id.startsWith("ia_")) {
                val identifier = movie.id.removePrefix("ia_")
                discoveryService.resolveArchiveDirectMp4(identifier)?.let { resolved ->
                    targetUrl = resolved
                }
            }
            _activePlayerRequest.value = PlayerRequest(
                title = movie.title,
                streamUrl = targetUrl,
                posterUrl = movie.posterUrl,
                sourceName = movie.sourceName,
                mediaType = movie.mediaType,
                startPositionMs = resumePosMs,
            )
        }
    }

    fun openPlayerRequest(request: PlayerRequest) {
        _activePlayerRequest.value = request
    }

    fun closePlayer() {
        _activePlayerRequest.value = null
    }

    fun extractStreamsFromAnyUrl(rawUrl: String, autoPlaySingle: Boolean = true) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            _snackbarMessage.value = "لطفاً یک لینک ویدیو یا آدرس وب‌سایت وارد کنید."
            return
        }
        viewModelScope.launch {
            _isExtractingUrl.value = true
            _extractedQuickStreams.value = emptyList()
            val normalized = MediaDiscoveryService.normalizeUrl(trimmed)
            val result = discoveryService.extractStreamsFromWebpage(normalized)
            _isExtractingUrl.value = false
            result.onSuccess { streams ->
                when {
                    streams.isEmpty() -> {
                        _browserUrl.value = normalized
                        _snackbarMessage.value = "لینک مستقیم پیدا نشد؛ برای استخراج خودکار از مرورگر هوشمند استفاده کنید."
                    }
                    streams.size == 1 && autoPlaySingle -> {
                        val only = streams.first()
                        _activePlayerRequest.value = PlayerRequest(
                            title = only.title,
                            streamUrl = only.url,
                            sourceName = normalized.substringAfter("://").substringBefore('/'),
                            mediaType = if (only.formatBadge == "HLS") MediaType.LIVE_CHANNEL else MediaType.WEB_VIDEO,
                        )
                    }
                    else -> {
                        _extractedQuickStreams.value = streams
                    }
                }
            }.onFailure { err ->
                _snackbarMessage.value = "خطا در بررسی لینک: ${err.localizedMessage ?: "نامشخص"}"
            }
        }
    }

    fun clearExtractedQuickStreams() {
        _extractedQuickStreams.value = emptyList()
    }

    fun addCustomSource(
        title: String,
        url: String,
        sourceType: SourceType,
        category: String,
        logoUrl: String? = null,
        rawM3uContent: String? = null,
    ) {
        viewModelScope.launch {
            val normalizedUrl = if (url.isNotBlank()) MediaDiscoveryService.normalizeUrl(url) else "https://local.m3u"
            val parsedCount = if (!rawM3uContent.isNullOrBlank()) {
                M3uParser.parse(rawM3uContent, category).size.toLong()
            } else {
                0L
            }
            val result = repository.addSource(
                title = title,
                url = normalizedUrl,
                sourceType = sourceType,
                category = category,
                logoUrl = logoUrl,
                channelCount = parsedCount,
            )
            result.onSuccess {
                _snackbarMessage.value = "«$title» با موفقیت ذخیره شد."
                if (!rawM3uContent.isNullOrBlank()) {
                    _activePlaylistTitle.value = title
                    _activePlaylistChannels.value = M3uParser.parse(rawM3uContent, category)
                }
            }.onFailure { err ->
                _snackbarMessage.value = "ذخیره ناموفق بود: ${err.localizedMessage ?: "خطا"}"
            }
        }
    }

    fun importM3uFromText(title: String, rawM3uText: String, category: String = "پلی‌لیست شخصی") {
        val channels = M3uParser.parse(rawM3uText, category)
        if (channels.isEmpty()) {
            _snackbarMessage.value = "فایل یا متن M3U معتبر نیست یا کانالی ندارد."
            return
        }
        _activePlaylistTitle.value = title
        _activePlaylistChannels.value = channels
        _snackbarMessage.value = "${channels.size} کانال از لیست استخراج شد."
    }

    fun openPlaylistSource(source: MediaSource) {
        viewModelScope.launch {
            _isLoadingPlaylist.value = true
            _activePlaylistTitle.value = source.title
            _activePlaylistChannels.value = emptyList()
            val result = discoveryService.fetchM3uPlaylist(source.url, source.category)
            _isLoadingPlaylist.value = false
            result.onSuccess { channels ->
                _activePlaylistChannels.value = channels
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "خطا در بارگذاری لیست کانال‌ها"
            }
        }
    }

    fun closePlaylistViewer() {
        _activePlaylistTitle.value = null
        _activePlaylistChannels.value = emptyList()
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            repository.deleteSource(sourceId).onFailure {
                _snackbarMessage.value = "حذف منبع انجام نشد."
            }
        }
    }

    fun toggleFavorite(
        title: String,
        streamUrl: String,
        posterUrl: String?,
        sourceName: String,
        mediaType: MediaType,
        currentlyFavorite: Boolean,
    ) {
        viewModelScope.launch {
            repository.toggleFavorite(
                title = title,
                streamUrl = streamUrl,
                posterUrl = posterUrl,
                sourceName = sourceName,
                mediaType = mediaType,
                currentlyFavorite = currentlyFavorite,
            ).onSuccess { nowFav ->
                _snackbarMessage.value = if (nowFav) {
                    "به علاقه‌مندی‌ها اضافه شد."
                } else {
                    "از علاقه‌مندی‌ها حذف شد."
                }
            }.onFailure {
                _snackbarMessage.value = "خطا در بروزرسانی علاقه‌مندی‌ها."
            }
        }
    }

    fun saveWatchProgress(
        request: PlayerRequest,
        positionMs: Long,
        durationMs: Long,
    ) {
        viewModelScope.launch {
            repository.saveWatchProgress(
                title = request.title,
                streamUrl = request.streamUrl,
                posterUrl = request.posterUrl,
                sourceName = request.sourceName,
                mediaType = request.mediaType,
                positionMs = positionMs,
                durationMs = durationMs,
            )
        }
    }

    fun deleteLibraryItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteLibraryItem(itemId).onFailure {
                _snackbarMessage.value = "حذف مورد انجام نشد."
            }
        }
    }

    fun setBrowserUrl(url: String) {
        _browserUrl.value = MediaDiscoveryService.normalizeUrl(url)
        _detectedWebStreams.value = emptyList()
    }

    fun reportDetectedWebStream(stream: DetectedWebStream) {
        val current = _detectedWebStreams.value
        if (current.none { it.url == stream.url }) {
            _detectedWebStreams.value = (listOf(stream) + current).take(30)
        }
    }

    fun clearDetectedWebStreams() {
        _detectedWebStreams.value = emptyList()
    }
}
