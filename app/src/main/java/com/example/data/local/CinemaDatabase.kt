package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaSourceEntity::class, LibraryItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CinemaDatabase : RoomDatabase() {
    abstract fun cinemaDao(): CinemaDao

    companion object {
        @Volatile
        private var INSTANCE: CinemaDatabase? = null

        fun getInstance(context: Context): CinemaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CinemaDatabase::class.java,
                    "cinema_stream.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
