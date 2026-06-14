package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistItemDao {
    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    fun getAllWatchlistItems(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItem)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun deleteWatchlistItem(id: Int)
}

@Dao
interface HistoryItemDao {
    @Query("SELECT * FROM history_items ORDER BY watchedAt DESC")
    fun getAllHistoryItems(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history_items WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM history_items")
    suspend fun clearAllHistory()
}

@Dao
interface CustomPlaylistDao {
    @Query("SELECT * FROM custom_playlists")
    fun getAllCustomPlaylists(): Flow<List<CustomPlaylist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPlaylist(playlist: CustomPlaylist)

    @Query("DELETE FROM custom_playlists WHERE id = :id")
    suspend fun deleteCustomPlaylist(id: String)
}
