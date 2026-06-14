package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MovieRepository(
        database.watchlistItemDao(),
        database.historyItemDao(),
        database.customPlaylistDao()
    )

    private val sharedPrefs = application.getSharedPreferences("eliteplex_prefs", Context.MODE_PRIVATE)

    // Sourced Flows
    val watchlistItems: StateFlow<List<WatchlistItem>> = repository.allItemsFlow()
    val historyItems: StateFlow<List<HistoryItem>> = repository.allHistoryItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val customPlaylists: StateFlow<List<CustomPlaylist>> = repository.allCustomPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Local DB Flow Helpers
    private fun MovieRepository.allItemsFlow(): StateFlow<List<WatchlistItem>> {
        return allWatchlistItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // App Preferences / Settings
    var themeMode by mutableStateOf(sharedPrefs.getString("theme_mode", "dark") ?: "dark")
        private set

    var accentColor by mutableStateOf(sharedPrefs.getString("accent_color", "#D4AF37") ?: "#D4AF37")
        private set

    var layoutDensity by mutableStateOf(sharedPrefs.getString("layout_density", "default") ?: "default")
        private set

    var userAvatar by mutableStateOf(sharedPrefs.getString("user_avatar", "") ?: "")
        private set

    var recentSearchesList by mutableStateOf(getSavedRecentSearches())
        private set

    // Active UI states
    var selectedTab by mutableStateOf("home")
    var selectedMediaId by mutableStateOf<Int?>(null)
    var selectedMediaType by mutableStateOf<String?>(null)
    var selectedDetailResponse by mutableStateOf<TmdbDetailResponse?>(null)
    var isDetailLoading by mutableStateOf(false)
    var trailerKey by mutableStateOf<String?>(null)

    // Active Player states
    var activePlayerMediaId by mutableStateOf<Int?>(null)
    var activePlayerTitle by mutableStateOf("")
    var activePlayerMediaType by mutableStateOf("")
    var activePlayerSeason by mutableStateOf(1)
    var activePlayerEpisode by mutableStateOf(1)
    var activePlayerPoster by mutableStateOf<String?>(null)
    var activeStreamUrl by mutableStateOf<String?>(null)

    // Active Dashboard lists
    var trendingMovies by mutableStateOf<List<TmdbItem>>(MockData.movies)
        private set
    var popularTvSeries by mutableStateOf<List<TmdbItem>>(MockData.tvSeries)
        private set
    var nowPlayingMovies by mutableStateOf<List<TmdbItem>>(MockData.similarMovies)
        private set
    var indianCinema by mutableStateOf<List<TmdbItem>>(MockData.indianCinema)
        private set
    var animeList by mutableStateOf<List<TmdbItem>>(MockData.anime)
        private set
    var actionBlockbusters by mutableStateOf<List<TmdbItem>>(MockData.actionBlockbusters)
        private set
    var isDashboardLoading by mutableStateOf(false)
        private set

    // Recommendation/Similar lists
    var recommendationList by mutableStateOf<List<TmdbItem>>(emptyList())
        private set
    var isRecommendationLoading by mutableStateOf(false)
        private set

    // Search states
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<TmdbItem>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var showSearchOverlay by mutableStateOf(false)
    var isVoiceSearching by mutableStateOf(false)

    init {
        loadDashboardData()
        loadRecommendations()
    }

    // Load full dashboard lists with graceful fallbacks
    fun loadDashboardData() {
        viewModelScope.launch {
            isDashboardLoading = true
            try {
                // Fetch in parallel using async to maximize speeds and prevent network lag
                val trendingJob = async { repository.getTrendingMovies() }
                val tvJob = async { repository.getPopularTvSeries() }
                val nowPlayingJob = async { repository.getNowPlayingMovies() }
                val indianJob = async { repository.getIndianCinema() }
                val animeJob = async { repository.getAnime() }
                val actionJob = async { repository.getActionBlockbusters() }

                trendingMovies = trendingJob.await()
                popularTvSeries = tvJob.await()
                nowPlayingMovies = nowPlayingJob.await()
                indianCinema = indianJob.await()
                animeList = animeJob.await()
                actionBlockbusters = actionJob.await()
            } catch (e: Exception) {
                // Keep pre-populated fallback MockData
            } finally {
                isDashboardLoading = false
            }
        }
    }

    // Load recommendations based on first item in History, or load fallback general list
    fun loadRecommendations() {
        viewModelScope.launch {
            isRecommendationLoading = true
            try {
                repository.allHistoryItems.collect { history ->
                    val firstHistory = history.firstOrNull()
                    if (firstHistory != null) {
                        val details = repository.getDetails(firstHistory.mediaType, firstHistory.mediaId)
                        if (details != null) {
                            // Sourcing matched recommendations
                            recommendationList = repository.getTrendingMovies().filter { it.id != firstHistory.mediaId }
                        }
                    } else {
                        recommendationList = MockData.similarMovies
                    }
                    isRecommendationLoading = false
                }
            } catch (e: Exception) {
                recommendationList = MockData.similarMovies
                isRecommendationLoading = false
            }
        }
    }

    // Detail modal fetching
    fun openMediaDetails(id: Int, mediaType: String) {
        selectedMediaId = id
        selectedMediaType = mediaType
        selectedDetailResponse = null
        trailerKey = null
        isDetailLoading = true
        viewModelScope.launch {
            val response = repository.getDetails(mediaType, id)
            selectedDetailResponse = response
            trailerKey = repository.getOrCreateTrailerKey(mediaType, id)
            isDetailLoading = false
        }
    }

    private suspend fun MovieRepository.getOrCreateTrailerKey(mediaType: String, id: Int): String? {
        val fetchedKey = getTrailerKey(mediaType, id)
        if (fetchedKey != null) return fetchedKey
        // Match high-fidelity hardcoded youtube keys from reference model
        return when (id) {
            9901 -> "co7K9S_fEHQ"
            9902 -> "4Gx3MgZHYO4"
            9903 -> "zSWdZAIB5nY"
            9904 -> "YoHD9XEInc0"
            9905 -> "vI9I67sAnEw"
            9911 -> "_InqQJRqGW4"
            9912 -> "b9EkMc79ZSU"
            9913 -> "6P96Kz06K5A"
            9921 -> "Fp9pNPdNwjI"
            9922 -> "8g18jFHCL7A"
            9931 -> "NgBoMJy386M"
            9932 -> "vqu4z34wENw"
            9941 -> "vyW84HIn2_8"
            9942 -> "p2Z7VOn_N20"
            9951 -> "32RAqMyP7wA"
            9952 -> "f9pIK8U_TTo"
            else -> null
        }
    }

    fun closeMediaDetails() {
        selectedMediaId = null
        selectedMediaType = null
        selectedDetailResponse = null
        trailerKey = null
    }

    // Toggle items inside local Room Watchlist database
    fun toggleWatchlist(item: TmdbItem) {
        viewModelScope.launch {
            val isFav = watchlistItems.value.any { it.id == item.id }
            if (isFav) {
                repository.removeFromWatchlist(item.id)
            } else {
                repository.addToWatchlist(
                    WatchlistItem(
                        id = item.id,
                        title = item.title ?: item.name ?: "Untitled",
                        posterPath = item.posterPath,
                        mediaType = item.mediaType ?: "movie",
                        addedAt = System.currentTimeMillis(),
                        voteAverage = item.voteAverage ?: 8.0
                    )
                )
            }
        }
    }

    fun removeIdFromWatchlist(id: Int) {
        viewModelScope.launch {
            repository.removeFromWatchlist(id)
        }
    }

    fun isWatchlisted(id: Int): Boolean {
        return watchlistItems.value.any { it.id == id }
    }

    // History log mutations
    fun playMovie(id: Int, title: String, posterPath: String?) {
        activePlayerMediaId = id
        activePlayerTitle = title
        activePlayerMediaType = "movie"
        activePlayerPoster = posterPath
        activeStreamUrl = when (id) {
            9901 -> "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            9902 -> "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.m3u8"
            9903 -> "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop_all.m3u8"
            else -> "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.m3u8"
        }

        viewModelScope.launch {
            repository.addToHistory(
                HistoryItem(
                    mediaId = id,
                    title = title,
                    posterPath = posterPath,
                    mediaType = "movie",
                    season = null,
                    episode = null,
                    watchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun playEpisode(id: Int, title: String, season: Int, episode: Int, posterPath: String?) {
        activePlayerMediaId = id
        activePlayerTitle = title
        activePlayerMediaType = "tv"
        activePlayerSeason = season
        activePlayerEpisode = episode
        activePlayerPoster = posterPath
        activeStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.m3u8"

        viewModelScope.launch {
            repository.addToHistory(
                HistoryItem(
                    mediaId = id,
                    title = title,
                    posterPath = posterPath,
                    mediaType = "tv",
                    season = season,
                    episode = episode,
                    watchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Custom Playlists mutations
    fun addCustomPlaylist(name: String, url: String, logo: String, description: String) {
        viewModelScope.launch {
            repository.addCustomPlaylist(
                CustomPlaylist(
                    id = "custom-${System.currentTimeMillis()}",
                    name = name,
                    logo = logo,
                    description = description,
                    url = url,
                    badge = "CUSTOM",
                    channelCount = "LIVE"
                )
            )
        }
    }

    fun deleteCustomPlaylist(id: String) {
        viewModelScope.launch {
            repository.deleteCustomPlaylist(id)
        }
    }

    // Search mutations
    fun performSearch(query: String) {
        searchQuery = query
        if (query.trim().length >= 2) {
            saveRecentSearch(query)
            isSearching = true
            viewModelScope.launch {
                searchResults = repository.searchMedia(query)
                isSearching = false
            }
        } else {
            searchResults = emptyList()
        }
    }

    private fun saveRecentSearch(term: String) {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return
        val current = recentSearchesList.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(10)
        recentSearchesList = limited
        sharedPrefs.edit().putString("recent_searches", limited.joinToString(",")).apply()
    }

    fun clearRecentSearches() {
        recentSearchesList = emptyList()
        sharedPrefs.edit().remove("recent_searches").apply()
    }

    private fun getSavedRecentSearches(): List<String> {
        val saved = sharedPrefs.getString("recent_searches", "") ?: ""
        return if (saved.isEmpty()) emptyList() else saved.split(",")
    }

    // Theme & Accent setters
    fun toggleThemeMode() {
        val newTheme = if (themeMode == "dark") "light" else "dark"
        themeMode = newTheme
        sharedPrefs.edit().putString("theme_mode", newTheme).apply()
    }

    fun updateAccentColor(color: String) {
        accentColor = color
        sharedPrefs.edit().putString("accent_color", color).apply()
    }

    fun updateLayoutDensity(density: String) {
        layoutDensity = density
        sharedPrefs.edit().putString("layout_density", density).apply()
    }

    fun updateAvatar(avatarUrl: String) {
        userAvatar = avatarUrl
        sharedPrefs.edit().putString("user_avatar", avatarUrl).apply()
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.clearHistory()
            recentSearchesList = emptyList()
            sharedPrefs.edit().clear().apply()
            themeMode = "dark"
            accentColor = "#D4AF37"
            layoutDensity = "default"
            userAvatar = ""
            // Clear Room DB watchlist too
            watchlistItems.value.forEach {
                repository.removeFromWatchlist(it.id)
            }
        }
    }
}
