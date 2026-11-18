package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CinemaDao {
    @Query("SELECT * FROM media_sources ORDER BY createdAtMs DESC")
    fun observeSources(): Flow<List<MediaSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(entity: MediaSourceEntity)

    @Query("DELETE FROM media_sources WHERE id = :id")
    suspend fun deleteSourceById(id: String)

    @Query("SELECT * FROM library_items ORDER BY COALESCE(lastWatchedAtMs, updatedAtMs, createdAtMs) DESC")
    fun observeLibrary(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE id = :id LIMIT 1")
    suspend fun getLibraryItemById(id: String): LibraryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibraryItem(entity: LibraryItemEntity)

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun deleteLibraryItemById(id: String)
}
