package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistItem::class, HistoryItem::class, CustomPlaylist::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistItemDao(): WatchlistItemDao
    abstract fun historyItemDao(): HistoryItemDao
    abstract fun customPlaylistDao(): CustomPlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eliteplex_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
