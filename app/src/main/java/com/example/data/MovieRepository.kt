package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MovieRepository(
    private val watchlistItemDao: WatchlistItemDao,
    private val historyItemDao: HistoryItemDao,
    private val customPlaylistDao: CustomPlaylistDao,
    private val tmdbService: TmdbService = TmdbService.create()
) {
    // API Key default from TMDB public key or provided
    private val apiKey = "15d2ea6d0dc1d476efbca3eba2b9bbfb"

    // Local DB Flows
    val allWatchlistItems: Flow<List<WatchlistItem>> = watchlistItemDao.getAllWatchlistItems()
    val allHistoryItems: Flow<List<HistoryItem>> = historyItemDao.getAllHistoryItems()
    val allCustomPlaylists: Flow<List<CustomPlaylist>> = customPlaylistDao.getAllCustomPlaylists()

    // Watchlist mutations
    suspend fun addToWatchlist(item: WatchlistItem) = withContext(Dispatchers.IO) {
        watchlistItemDao.insertWatchlistItem(item)
    }

    suspend fun removeFromWatchlist(id: Int) = withContext(Dispatchers.IO) {
        watchlistItemDao.deleteWatchlistItem(id)
    }

    // History mutations
    suspend fun addToHistory(item: HistoryItem) = withContext(Dispatchers.IO) {
        historyItemDao.insertHistoryItem(item)
    }

    suspend fun deleteFromHistory(id: Int) = withContext(Dispatchers.IO) {
        historyItemDao.deleteHistoryItem(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyItemDao.clearAllHistory()
    }

    // Custom Playlists mutations
    suspend fun addCustomPlaylist(playlist: CustomPlaylist) = withContext(Dispatchers.IO) {
        customPlaylistDao.insertCustomPlaylist(playlist)
    }

    suspend fun deleteCustomPlaylist(id: String) = withContext(Dispatchers.IO) {
        customPlaylistDao.deleteCustomPlaylist(id)
    }

    // Network / Mock Sourcing
    suspend fun getTrendingMovies(): List<TmdbItem> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbService.getTrendingMovies(apiKey)
            response.results ?: MockData.movies
        } catch (e: Exception) {
            MockData.movies
        }
    }

    suspend fun getPopularTvSeries(): List<TmdbItem> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbService.getPopularTvSeries(apiKey)
            response.results ?: MockData.tvSeries
        } catch (e: Exception) {
            MockData.tvSeries
        }
    }

    suspend fun getNowPlayingMovies(): List<TmdbItem> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbService.getNowPlayingMovies(apiKey)
            response.results ?: MockData.similarMovies
        } catch (e: Exception) {
            MockData.similarMovies
        }
    }

    suspend fun getIndianCinema(): List<TmdbItem> = withContext(Dispatchers.IO) {
        try {
            val response = tmdbService.discoverMovies(apiKey, "IN")
            response.results ?: MockData.indianCinema
        } catch (e: Exception) {
            MockData.indianCinema
        }
    }

    suspend fun getAnime(): List<TmdbItem> = withContext(Dispatchers.IO) {
        try {
            // Genre 16 is Animation, Original Language is ja (Japanese) for Anime
            val response = tmdbService.discoverTvSeries(apiKey, "16", "ja")
            response.results ?: MockData.anime
        } catch (e: Exception) {
            MockData.anime
        }
    }

    suspend fun getActionBlockbusters(): List<TmdbItem> = withContext(Dispatchers.IO) {
        try {
            // Genre 28 is Action
            val response = tmdbService.discoverMovies(apiKey, genres = "28")
            response.results ?: MockData.actionBlockbusters
        } catch (e: Exception) {
            MockData.actionBlockbusters
        }
    }

    suspend fun getDetails(mediaType: String, id: Int): TmdbDetailResponse? = withContext(Dispatchers.IO) {
        try {
            tmdbService.getDetails(mediaType, id, apiKey)
        } catch (e: Exception) {
            MockData.getDetailForId(id)
        }
    }

    suspend fun getTrailerKey(mediaType: String, id: Int): String? = withContext(Dispatchers.IO) {
        try {
            val response = tmdbService.getVideos(mediaType, id, apiKey)
            val ytVideo = response.results?.find { it.site == "YouTube" && (it.type == "Trailer" || it.type == "Teaser") }
                ?: response.results?.find { it.site == "YouTube" }
            ytVideo?.key
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchMedia(query: String): List<TmdbItem> = withContext(Dispatchers.IO) {
        try {
            val moviesRes = tmdbService.searchMovies(apiKey, query).results ?: emptyList()
            val tvRes = tmdbService.searchTvSeries(apiKey, query).results ?: emptyList()
            val mergedList = mutableListOf<TmdbItem>()
            mergedList.addAll(moviesRes.map { it.copy(mediaType = "movie") })
            mergedList.addAll(tvRes.map { it.copy(mediaType = "tv") })
            mergedList.sortedByDescending { it.voteAverage ?: 0.0 }
        } catch (e: Exception) {
            // Fuzzy search MockData
            MockData.allMockItems.filter {
                (it.title ?: "").contains(query, ignoreCase = true) ||
                (it.name ?: "").contains(query, ignoreCase = true) ||
                (it.overview ?: "").contains(query, ignoreCase = true)
            }
        }
    }
}
