package com.example.data.model

enum class SourceType(val persianLabel: String) {
    CHANNEL("کانال زنده"),
    PLAYLIST_M3U("پلی‌لیست M3U"),
    WEBSITE("وب‌سایت فیلم"),
    DIRECT_MOVIE("لینک مستقیم فیلم");

    companion object {
        fun fromRaw(raw: String?): SourceType =
            entries.firstOrNull { it.name == raw } ?: CHANNEL
    }
}

enum class MediaType(val persianLabel: String) {
    MOVIE("فیلم"),
    LIVE_CHANNEL("کانال زنده"),
    WEB_VIDEO("ویدیو وب");

    companion object {
        fun fromRaw(raw: String?): MediaType =
            entries.firstOrNull { it.name == raw } ?: MOVIE
    }
}

data class MediaSource(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val sourceType: String = SourceType.CHANNEL.name,
    val category: String = "عمومی",
    val logoUrl: String? = null,
    val channelCount: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    fun resolvedType(): SourceType = SourceType.fromRaw(sourceType)
}

data class LibraryItem(
    val id: String = "",
    val title: String = "",
    val streamUrl: String = "",
    val posterUrl: String? = null,
    val sourceName: String = "",
    val mediaType: String = MediaType.MOVIE.name,
    val favorite: Boolean = false,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAtMs: Long? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    fun resolvedMediaType(): MediaType = MediaType.fromRaw(mediaType)
}

data class CatalogMovie(
    val id: String,
    val title: String,
    val subtitle: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val category: String = "سینمایی",
    val sourceName: String = "سینما استریم",
    val mediaType: MediaType = MediaType.MOVIE,
    val durationLabel: String = "",
    val year: String = "",
    val formatBadge: String = "MP4",
    val isLive: Boolean = false,
)

data class WebSearchPageHit(
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String,
)

data class InternetSearchBundle(
    val playableMovies: List<CatalogMovie> = emptyList(),
    val webPages: List<WebSearchPageHit> = emptyList(),
)

data class M3uChannel(
    val title: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "عمومی",
)

data class DetectedWebStream(
    val url: String,
    val formatBadge: String,
    val title: String,
    val detectedAtMs: Long = System.currentTimeMillis(),
)

data class PlayerRequest(
    val title: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val sourceName: String = "پخش مستقیم",
    val mediaType: MediaType = MediaType.MOVIE,
    val startPositionMs: Long = 0L,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
