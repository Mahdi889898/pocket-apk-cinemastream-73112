package com.example.data.local

import com.example.data.model.LibraryItem
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import com.example.data.model.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

class CinemaRepository(private val dao: CinemaDao) {

    fun observeSources(): Flow<List<MediaSource>> =
        dao.observeSources().map { list -> list.map { it.toDomain() } }

    fun observeLibrary(): Flow<List<LibraryItem>> =
        dao.observeLibrary().map { list -> list.map { it.toDomain() } }

    suspend fun addSource(
        title: String,
        url: String,
        sourceType: SourceType,
        category: String,
        logoUrl: String? = null,
        channelCount: Long = 0L,
    ): Result<String> = runCatching {
        val cleanTitle = title.trim().take(120)
        val cleanUrl = url.trim().take(1200)
        require(cleanTitle.isNotEmpty()) { "عنوان منبع نباید خالی باشد." }
        require(cleanUrl.length >= 4) { "آدرس منبع معتبر نیست." }
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString().replace("-", "")
        dao.insertSource(
            MediaSourceEntity(
                id = id,
                title = cleanTitle,
                url = cleanUrl,
                sourceType = sourceType.name,
                category = category.trim().ifBlank { "عمومی" }.take(60),
                logoUrl = logoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(1200),
                channelCount = channelCount.coerceIn(0L, 50000L),
                createdAtMs = now,
                updatedAtMs = now,
            )
        )
        id
    }

    suspend fun deleteSource(sourceId: String): Result<Unit> = runCatching {
        dao.deleteSourceById(sourceId)
    }

    suspend fun toggleFavorite(
        title: String,
        streamUrl: String,
        posterUrl: String?,
        sourceName: String,
        mediaType: MediaType,
        currentlyFavorite: Boolean,
    ): Result<Boolean> = runCatching {
        val itemId = stableDocIdForUrl(streamUrl)
        val existing = dao.getLibraryItemById(itemId)
        val now = System.currentTimeMillis()
        val nextFavorite = !currentlyFavorite

        val entity = if (existing != null) {
            existing.copy(
                title = title.trim().ifBlank { existing.title }.take(160),
                posterUrl = posterUrl?.trim()?.takeIf { it.isNotBlank() } ?: existing.posterUrl,
                sourceName = sourceName.trim().ifBlank { existing.sourceName }.take(100),
                mediaType = mediaType.name,
                favorite = nextFavorite,
                updatedAtMs = now,
            )
        } else {
            LibraryItemEntity(
                id = itemId,
                title = title.trim().ifBlank { "ویدیو" }.take(160),
                streamUrl = streamUrl.trim().take(1200),
                posterUrl = posterUrl?.trim()?.takeIf { it.isNotBlank() }?.take(1200),
                sourceName = sourceName.trim().ifBlank { "سینما استریم" }.take(100),
                mediaType = mediaType.name,
                favorite = nextFavorite,
                lastPositionMs = 0L,
                durationMs = 0L,
                lastWatchedAtMs = null,
                createdAtMs = now,
                updatedAtMs = now,
            )
        }
        dao.upsertLibraryItem(entity)
        nextFavorite
    }

    suspend fun saveWatchProgress(
        title: String,
        streamUrl: String,
        posterUrl: String?,
        sourceName: String,
        mediaType: MediaType,
        positionMs: Long,
        durationMs: Long,
    ): Result<Unit> = runCatching {
        val cleanUrl = streamUrl.trim().take(1200)
        if (cleanUrl.length < 4) return@runCatching
        val itemId = stableDocIdForUrl(cleanUrl)
        val existing = dao.getLibraryItemById(itemId)
        val now = System.currentTimeMillis()

        val entity = if (existing != null) {
            existing.copy(
                title = title.trim().ifBlank { existing.title }.take(160),
                posterUrl = posterUrl?.trim()?.takeIf { it.isNotBlank() } ?: existing.posterUrl,
                sourceName = sourceName.trim().ifBlank { existing.sourceName }.take(100),
                mediaType = mediaType.name,
                lastPositionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(0L),
                lastWatchedAtMs = now,
                updatedAtMs = now,
            )
        } else {
            LibraryItemEntity(
                id = itemId,
                title = title.trim().ifBlank { "ویدیو" }.take(160),
                streamUrl = cleanUrl,
                posterUrl = posterUrl?.trim()?.takeIf { it.isNotBlank() }?.take(1200),
                sourceName = sourceName.trim().ifBlank { "سینما استریم" }.take(100),
                mediaType = mediaType.name,
                favorite = false,
                lastPositionMs = positionMs.coerceAtLeast(0L),
                durationMs = durationMs.coerceAtLeast(0L),
                lastWatchedAtMs = now,
                createdAtMs = now,
                updatedAtMs = now,
            )
        }
        dao.upsertLibraryItem(entity)
    }

    suspend fun deleteLibraryItem(itemId: String): Result<Unit> = runCatching {
        dao.deleteLibraryItemById(itemId)
    }

    companion object {
        fun stableDocIdForUrl(url: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(url.trim().toByteArray(Charsets.UTF_8))
            return "m_" + digest.take(20).joinToString("") { "%02x".format(it) }
        }
    }
}
