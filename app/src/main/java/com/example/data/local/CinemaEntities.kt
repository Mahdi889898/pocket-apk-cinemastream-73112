package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.LibraryItem
import com.example.data.model.MediaSource

@Entity(tableName = "media_sources")
data class MediaSourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val sourceType: String,
    val category: String,
    val logoUrl: String?,
    val channelCount: Long,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) {
    fun toDomain(): MediaSource = MediaSource(
        id = id,
        title = title,
        url = url,
        sourceType = sourceType,
        category = category,
        logoUrl = logoUrl,
        channelCount = channelCount,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )
}

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String?,
    val sourceName: String,
    val mediaType: String,
    val favorite: Boolean,
    val lastPositionMs: Long,
    val durationMs: Long,
    val lastWatchedAtMs: Long?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) {
    fun toDomain(): LibraryItem = LibraryItem(
        id = id,
        title = title,
        streamUrl = streamUrl,
        posterUrl = posterUrl,
        sourceName = sourceName,
        mediaType = mediaType,
        favorite = favorite,
        lastPositionMs = lastPositionMs,
        durationMs = durationMs,
        lastWatchedAtMs = lastWatchedAtMs,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )
}
