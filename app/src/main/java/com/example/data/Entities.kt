package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist_items")
data class WatchlistItem(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val mediaType: String, // "movie" or "tv"
    val addedAt: Long,
    val voteAverage: Double
)

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mediaId: Int,
    val title: String,
    val posterPath: String?,
    val mediaType: String, // "movie" or "tv"
    val season: Int?,
    val episode: Int?,
    val watchedAt: Long
)

@Entity(tableName = "custom_playlists")
data class CustomPlaylist(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String,
    val description: String,
    val url: String,
    val badge: String, // "CUSTOM"
    val channelCount: String
)
