package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MovieApp(viewModel: MovieViewModel) {
    val context = LocalContext.current
    val accentColor = Color(android.graphics.Color.parseColor(viewModel.accentColor))
    val isDark = viewModel.themeMode == "dark"

    val backgroundColors = if (isDark) {
        listOf(Color(0xFF050505), Color(0xFF0A0A0A))
    } else {
        listOf(Color(0xFFFAFAFA), Color(0xFFF5F5F5))
    }

    val textPrimaryColor = if (isDark) Color(0xFFFAFAFA) else Color(0xFF111111)
    val textSecondaryColor = if (isDark) Color(0xFFA3A3A3) else Color(0xFF525252)
    val cardBgColor = if (isDark) Color(0xFF171717) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF262626) else Color(0xFFE5E5E5)

    // Handle Deep Link / In-app player playback view
    if (viewModel.activePlayerMediaId != null) {
        LivePlayerScreen(viewModel = viewModel, accentColor = accentColor, isDark = isDark)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(backgroundColors))
            .drawBehind {
                // Ambient cinematic blur effect
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x0DF43F5E), Color.Transparent),
                            radius = size.minDimension * 0.8f
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.2f),
                        radius = size.minDimension * 0.8f
                    )
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Sidebar for Medium and Expanded Screens (Tablets / Foldables)
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isTablet = configuration.screenWidthDp >= 600

            if (isTablet) {
                NavigationSidebar(
                    selectedTab = viewModel.selectedTab,
                    onTabSelected = { viewModel.selectedTab = it },
                    accentColor = accentColor,
                    isDark = isDark,
                    userAvatar = viewModel.userAvatar,
                    onAvatarClick = { viewModel.selectedTab = "profile" }
                )
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = Color.Transparent,
                topBar = {
                    if (viewModel.selectedTab != "profile" && viewModel.selectedTab != "settings") {
                        TopHeader(
                            viewModel = viewModel,
                            accentColor = accentColor,
                            textPrimaryColor = textPrimaryColor,
                            isDark = isDark
                        )
                    }
                },
                bottomBar = {
                    if (!isTablet) {
                        BottomNavBar(
                            selectedTab = viewModel.selectedTab,
                            onTabSelected = { viewModel.selectedTab = it },
                            accentColor = accentColor,
                            isDark = isDark
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = viewModel.selectedTab,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) with fadeOut(animationSpec = spring())
                        },
                        label = "tabChange"
                    ) { tab ->
                        when (tab) {
                            "home" -> HomeScreen(viewModel, accentColor, isDark)
                            "movies" -> CatalogScreen(viewModel, "movie", accentColor, isDark)
                            "series" -> CatalogScreen(viewModel, "tv", accentColor, isDark)
                            "live-tv" -> LiveTvScreen(viewModel, accentColor, isDark)
                            "profile" -> ProfileScreen(viewModel, accentColor, isDark)
                            "settings" -> SettingsScreen(viewModel, accentColor, isDark)
                        }
                    }

                    // Search overlay view
                    if (viewModel.showSearchOverlay) {
                        SearchOverlay(viewModel, accentColor, isDark)
                    }
                }
            }
        }

        // Details Modal Sheet
        AnimatedVisibility(
            visible = viewModel.selectedMediaId != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            DetailBottomSheet(viewModel = viewModel, accentColor = accentColor, isDark = isDark)
        }
    }
}

@Composable
fun TopHeader(
    viewModel: MovieViewModel,
    accentColor: Color,
    textPrimaryColor: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                viewModel.searchQuery = spokenText
                viewModel.showSearchOverlay = true
                viewModel.performSearch(spokenText)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = "https://sP878rcC8zdQ.cc/52NFCsj5/c11uRqpJN60G.png",
                contentDescription = "Logo",
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            )
            Text(
                text = "ElitePlex",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.testTag("app_title")
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.showSearchOverlay = true
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = accentColor
                )
            }

            IconButton(
                onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a movie or TV series title...")
                        }
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        viewModel.showSearchOverlay = true
                        viewModel.isVoiceSearching = true
                    }
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    tint = if (viewModel.isVoiceSearching) Color.Red else accentColor
                )
            }

            IconButton(
                onClick = {
                    viewModel.selectedTab = "profile"
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Color.Red, accentColor, Color.Blue)))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.userAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = viewModel.userAvatar,
                            contentDescription = "User Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "VIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationSidebar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    accentColor: Color,
    isDark: Boolean,
    userAvatar: String,
    onAvatarClick: () -> Unit
) {
    val sidebarBg = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.LightGray.copy(alpha = 0.6f)

    NavigationRail(
        containerColor = sidebarBg,
        modifier = Modifier
            .fillMaxHeight()
            .border(Dp.Unspecified, borderColor)
            .shadow(24.dp, shape = RectangleShape),
        header = {
            Row(
                modifier = Modifier
                    .clickable { onTabSelected("home") }
                    .padding(vertical = 24.dp, horizontal = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = "https://sP878rcC8zdQ.cc/52NFCsj5/c11uRqpJN60G.png",
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Text(
                    text = "ELITE",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic
                )
            }
        },
        contentColor = accentColor
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                NavigationRailItem(
                    selected = selectedTab == "home",
                    onClick = { onTabSelected("home") },
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    )
                )

                NavigationRailItem(
                    selected = selectedTab == "movies",
                    onClick = { onTabSelected("movies") },
                    icon = { Icon(Icons.Default.Movie, "Movies") },
                    label = { Text("Movies", fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    )
                )

                NavigationRailItem(
                    selected = selectedTab == "series",
                    onClick = { onTabSelected("series") },
                    icon = { Icon(Icons.Default.Tv, "Series") },
                    label = { Text("Series", fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    )
                )

                NavigationRailItem(
                    selected = selectedTab == "live-tv",
                    onClick = { onTabSelected("live-tv") },
                    icon = { Icon(Icons.Default.LiveTv, "Live TV") },
                    label = { Text("Live TV", fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    )
                )

                NavigationRailItem(
                    selected = selectedTab == "settings",
                    onClick = { onTabSelected("settings") },
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) },
                    alwaysShowLabel = false,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        indicatorColor = accentColor.copy(alpha = 0.15f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clickable { onAvatarClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = userAvatar,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("VIP", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    accentColor: Color,
    isDark: Boolean
) {
    val barBg = if (isDark) Color(0xFF0A0A0A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.8f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.4f)

    NavigationBar(
        containerColor = barBg,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, borderColor, RoundedCornerShape(32.dp))
            .shadow(8.dp, shape = RoundedCornerShape(32.dp)),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == "home",
            onClick = { onTabSelected("home") },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accentColor,
                selectedTextColor = accentColor,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accentColor.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == "movies",
            onClick = { onTabSelected("movies") },
            icon = { Icon(Icons.Default.Movie, "Movies") },
            label = { Text("Movies") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accentColor,
                selectedTextColor = accentColor,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accentColor.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == "series",
            onClick = { onTabSelected("series") },
            icon = { Icon(Icons.Default.Tv, "Series") },
            label = { Text("Series") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accentColor,
                selectedTextColor = accentColor,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accentColor.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == "live-tv",
            onClick = { onTabSelected("live-tv") },
            icon = { Icon(Icons.Default.LiveTv, "Live TV") },
            label = { Text("Live TV") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accentColor,
                selectedTextColor = accentColor,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accentColor.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(Icons.Default.Settings, "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accentColor,
                selectedTextColor = accentColor,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accentColor.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun HomeScreen(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val context = LocalContext.current
    val watchlist by viewModel.watchlistItems.collectAsStateWithLifecycle()
    val history by viewModel.historyItems.collectAsStateWithLifecycle()
    val recommendations = viewModel.recommendationList

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 3D Cinematic Spotlight Carousel
        item {
            SpotlightCarousel(viewModel, accentColor, isDark)
        }

        // Continue Watching Row (from Local History Room DB)
        if (history.isNotEmpty()) {
            item {
                RowSectionHeader(title = "Continue Watching", onClearClick = { viewModel.clearHistory() })
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(history) { log ->
                        ContinueWatchingCard(log, onClick = {
                            if (log.mediaType == "movie") {
                                viewModel.playMovie(log.mediaId, log.title, log.posterPath)
                            } else {
                                viewModel.playEpisode(
                                    log.mediaId,
                                    log.title,
                                    log.season ?: 1,
                                    log.episode ?: 1,
                                    log.posterPath
                                )
                            }
                        }, accentColor = accentColor, isDark = isDark)
                    }
                }
            }
        }

        // Recommended / Similar Row based on user profile and history
        if (recommendations.isNotEmpty()) {
            item {
                RowSectionHeader(title = "Recommended For You")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(recommendations) { item ->
                        MediaCard(item = item, onClick = {
                            viewModel.openMediaDetails(item.id, item.mediaType ?: "movie")
                        }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                            viewModel.toggleWatchlist(item)
                        }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                    }
                }
            }
        }

        // Trending Movies
        if (viewModel.trendingMovies.isNotEmpty()) {
            item {
                RowSectionHeader(title = "Trending Movies", onSeeAllIndex = { viewModel.selectedTab = "movies" })
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.trendingMovies.take(10)) { item ->
                        MediaCard(item = item, onClick = {
                            viewModel.openMediaDetails(item.id, "movie")
                        }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                            viewModel.toggleWatchlist(item)
                        }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                    }
                }
            }
        }

        // Popular TV Series
        if (viewModel.popularTvSeries.isNotEmpty()) {
            item {
                RowSectionHeader(title = "Popular TV Series", onSeeAllIndex = { viewModel.selectedTab = "series" })
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.popularTvSeries.take(10)) { item ->
                        MediaCard(item = item, onClick = {
                            viewModel.openMediaDetails(item.id, "tv")
                        }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                            viewModel.toggleWatchlist(item)
                        }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                    }
                }
            }
        }

        // Indian Cinema
        if (viewModel.indianCinema.isNotEmpty()) {
            item {
                RowSectionHeader(title = "Indian Cinema")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.indianCinema.take(10)) { item ->
                        MediaCard(item = item, onClick = {
                            viewModel.openMediaDetails(item.id, "movie")
                        }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                            viewModel.toggleWatchlist(item)
                        }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                    }
                }
            }
        }

        // Anime Collection
        if (viewModel.animeList.isNotEmpty()) {
            item {
                RowSectionHeader(title = "Anime Collection")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.animeList.take(10)) { item ->
                        MediaCard(item = item, onClick = {
                            viewModel.openMediaDetails(item.id, "tv")
                        }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                            viewModel.toggleWatchlist(item)
                        }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                    }
                }
            }
        }

        // Action Blockbusters
        if (viewModel.actionBlockbusters.isNotEmpty()) {
            item {
                RowSectionHeader(title = "Action Blockbusters")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.actionBlockbusters.take(10)) { item ->
                        MediaCard(item = item, onClick = {
                            viewModel.openMediaDetails(item.id, "movie")
                        }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                            viewModel.toggleWatchlist(item)
                        }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotlightCarousel(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val items = viewModel.trendingMovies.take(6)
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(key1 = true) {
        while (true) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spotlight_carousel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            contentPadding = PaddingValues(horizontal = 48.dp)
        ) { page ->
            val item = items[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

            // Apply 3D perspective scroll transformations (scale, translation, rotation)
            val graphicsModifier = Modifier.graphicsLayer {
                val scale = 1f - (abs(pageOffset) * 0.15f)
                scaleX = scale
                scaleY = scale

                translationX = pageOffset * 50f
                alpha = 1f - (abs(pageOffset) * 0.35f)

                rotationY = pageOffset * -20f
                cameraDistance = 12f
            }

            Box(
                modifier = graphicsModifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .clickable {
                        viewModel.openMediaDetails(item.id, item.mediaType ?: "movie")
                    }
                    .shadow(16.dp, shape = RoundedCornerShape(24.dp))
            ) {
                AsyncImage(
                    model = if (item.posterPath != null) "https://image.tmdb.org/t/p/w500${item.posterPath}" else "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&q=80&w=500",
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(android.R.drawable.stat_notify_error)
                )

                // Bottom transparent information overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Text(
                        text = item.title ?: item.name ?: "",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", item.voteAverage ?: 0.0),
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pager indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 16.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) accentColor else Color.Gray.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun RowSectionHeader(
    title: String,
    onClearClick: (() -> Unit)? = null,
    onSeeAllIndex: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 16.dp)
                    .background(Color(android.graphics.Color.parseColor("#DF3F5E")), CircleShape)
            )
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )
        }

        if (onClearClick != null) {
            Text(
                text = "Clear All",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Red,
                modifier = Modifier.clickable { onClearClick() }
            )
        } else if (onSeeAllIndex != null) {
            Text(
                text = "See All",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                modifier = Modifier.clickable { onSeeAllIndex() }
            )
        }
    }
}

@Composable
fun MediaCard(
    item: TmdbItem,
    onClick: () -> Unit,
    isWatchlisted: Boolean,
    onToggleWatchlist: () -> Unit,
    isDark: Boolean,
    sizeCompact: Boolean = false
) {
    val cardWidth = if (sizeCompact) 140.dp else 200.dp
    val cardHeight = if (sizeCompact) 210.dp else 300.dp
    val posterUrl = if (item.posterPath != null) {
        if (item.posterPath.startsWith("http")) item.posterPath else "https://image.tmdb.org/t/p/w320${item.posterPath}"
    } else {
        "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&q=80&w=320"
    }

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clickable { onClick() }
            .testTag("media_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = item.title ?: item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top tags & rating
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", item.voteAverage ?: 8.0),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Watchlist Heart Toggle
                    IconButton(
                        onClick = { onToggleWatchlist() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isWatchlisted) Color(0xFFFF2357) else Color.Black.copy(alpha = 0.75f))
                    ) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Watchlist",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Bottom Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = item.title ?: item.name ?: "Untitled",
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val year = (item.releaseDate ?: item.firstAirDate ?: "2026").split("-")[0]
                    Text(
                        text = year,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    log: HistoryItem,
    onClick: () -> Unit,
    accentColor: Color,
    isDark: Boolean
) {
    val cardWidth = 200.dp
    val cardHeight = 135.dp
    val posterUrl = if (log.posterPath != null) {
        if (log.posterPath.startsWith("http")) log.posterPath else "https://image.tmdb.org/t/p/w320${log.posterPath}"
    } else {
        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&q=80&w=320"
    }

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = log.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Blur Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            )

            // Play Icon over the center on hover/focus
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Resume",
                tint = accentColor,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(6.dp)
            )

            // Title & Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = log.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (log.mediaType == "tv") {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "TV Series",
                            tint = accentColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "S${log.season} • E${log.episode}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Movie",
                            tint = Color(0xFFDF3F5E),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "Resume Film",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFFDF3F5E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogScreen(viewModel: MovieViewModel, mediaType: String, accentColor: Color, isDark: Boolean) {
    val items = if (mediaType == "movie") viewModel.trendingMovies else viewModel.popularTvSeries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (mediaType == "movie") "Cinematic Movies" else "Popular TV Series",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = accentColor
                )
                Text(
                    text = "Streaming dynamically synced catalogs",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (items.isEmpty() && viewModel.isDashboardLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            val columns = if (viewModel.layoutDensity == "compact") 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("catalog_grid"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    MediaCard(item = item, onClick = {
                        viewModel.openMediaDetails(item.id, mediaType)
                    }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                        viewModel.toggleWatchlist(item)
                    }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                }
            }
        }
    }
}

@Composable
fun SearchOverlay(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val backgroundColors = if (isDark) {
        listOf(Color(0xFF0B0C0E), Color(0xFF050505))
    } else {
        listOf(Color(0xFFFAFAFA), Color(0xFFE5E5E5))
    }
    val textPrimary = if (isDark) Color.White else Color.Black
    val containerBg = if (isDark) Color(0xFF101216) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(backgroundColors))
            .clickable(enabled = false) {}
            .testTag("search_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header with search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = {
                        viewModel.showSearchOverlay = false
                        viewModel.searchQuery = ""
                        viewModel.isVoiceSearching = false
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }

                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = {
                        viewModel.performSearch(it)
                    },
                    placeholder = {
                        Text(
                            text = "Search movies and TV shows...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input"),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                    },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = accentColor,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.performSearch(viewModel.searchQuery)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                IconButton(
                    onClick = {
                        viewModel.isVoiceSearching = true
                    },
                    modifier = Modifier.background(
                        if (viewModel.isVoiceSearching) Color.Red.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f),
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Assistant",
                        tint = if (viewModel.isVoiceSearching) Color.Red else accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Voice Search Info Notice
            if (viewModel.isVoiceSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Red.copy(alpha = 0.1f))
                        .border(1.dp, Color.Red.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Speech Assistant",
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please state the movie or series title directly (e.g. \"KGF\" or \"Pushpa\") to trigger live search directory lists.",
                            fontSize = 11.sp,
                            color = textPrimary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Content
            if (viewModel.searchQuery.trim().isEmpty()) {
                // Recent Searches
                if (viewModel.recentSearchesList.isNotEmpty()) {
                    Text(
                        text = "Recent Searches",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(viewModel.recentSearchesList) { term ->
                            SuggestionChip(term = term, onClick = {
                                viewModel.searchQuery = term
                                viewModel.performSearch(term)
                            }, isDark = isDark)
                        }
                    }
                }

                // Explore genres
                Text(
                    text = "Explore Genres",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(qO) { genre ->
                        GenreExploreCard(genre = genre, onClick = {
                            viewModel.searchQuery = genre.query
                            viewModel.performSearch(genre.query)
                        }, isDark = isDark)
                    }
                }
            } else {
                // Results List
                if (viewModel.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else if (viewModel.searchResults.isNotEmpty()) {
                    Text(
                        text = "Search Results (${viewModel.searchResults.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (viewModel.layoutDensity == "compact") 3 else 2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(viewModel.searchResults) { item ->
                            MediaCard(item = item, onClick = {
                                viewModel.showSearchOverlay = false
                                viewModel.openMediaDetails(item.id, item.mediaType ?: "movie")
                            }, isWatchlisted = viewModel.isWatchlisted(item.id), onToggleWatchlist = {
                                viewModel.toggleWatchlist(item)
                            }, isDark = isDark, sizeCompact = viewModel.layoutDensity == "compact")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "No Results",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No results found for \"${viewModel.searchQuery}\"",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Try adjusting spelling or searching other active genres.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(term: String, onClick: () -> Unit, isDark: Boolean) {
    val bg = if (isDark) Color(0xFF18181A) else Color(0xFFE5E5E5)
    val text = if (isDark) Color(0xFFE5E5E5) else Color(0xFF171717)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = Color.Gray, modifier = Modifier.size(16.dp))
        Text(text = term, fontSize = 13.sp, color = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GenreExploreCard(genre: GenreInfo, onClick: () -> Unit, isDark: Boolean) {
    val bgInfo = if (isDark) Color(0xFF121214) else Color.White
    val borderCol = if (isDark) Color.White.copy(alpha = 0.05f) else Color.LightGray.copy(alpha = 0.4f)
    val textCol = if (isDark) Color(0xFFE5E5E5) else Color(0xFF171717)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgInfo)
            .clickable { onClick() }
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = genre.img,
            contentDescription = genre.name,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Text(text = genre.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textCol)
    }
}

data class GenreInfo(val id: Int, val name: String, val query: String, val img: String)

val qO = listOf(
    GenreInfo(28, "Action", "Action", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(16, "Animated", "Animation", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(35, "Comedy", "Comedy", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(99, "Documentary", "Documentary", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(18, "Drama", "Drama", "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(10751, "Family", "Family", "https://images.unsplash.com/photo-1481841580057-e2b9927a05c6?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(14, "Fantasy", "Fantasy", "https://images.unsplash.com/photo-1519074069444-1ba4e6664104?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(27, "Horror", "Horror", "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(9648, "Mystery", "Mystery", "https://images.unsplash.com/photo-1505664194779-8bebcb95c370?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(10749, "Romance", "Romance", "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(878, "Sci-Fi", "Science Fiction", "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=120&auto=format&fit=crop&q=60"),
    GenreInfo(53, "Thriller", "Thriller", "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=120&auto=format&fit=crop&q=60")
)

@Composable
fun DetailBottomSheet(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val details = viewModel.selectedDetailResponse
    val isTv = viewModel.selectedMediaType == "tv"
    val textPrimary = if (isDark) Color.White else Color.Black
    val containerBg = if (isDark) Color(0xFF101216) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { viewModel.closeMediaDetails() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(containerBg)
                .clickable(enabled = false) {}
                .padding(bottom = 60.dp)
        ) {
            if (viewModel.isDetailLoading || details == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            val detailImgUrl = if (details.backdropPath != null) {
                                if (details.backdropPath.startsWith("http")) details.backdropPath else "https://image.tmdb.org/t/p/w780${details.backdropPath}"
                            } else if (details.posterPath != null) {
                                if (details.posterPath.startsWith("http")) details.posterPath else "https://image.tmdb.org/t/p/w500${details.posterPath}"
                            } else {
                                "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&q=80&w=780"
                            }
                            AsyncImage(
                                model = detailImgUrl,
                                contentDescription = details.title ?: details.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                containerBg.copy(alpha = 0.5f),
                                                containerBg
                                            )
                                        )
                                    )
                            )
                            IconButton(
                                onClick = { viewModel.closeMediaDetails() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = details.title ?: details.name ?: "Untitled",
                                fontFamily = FontFamily.Serif,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "★ ${details.voteAverage?.let { String.format("%.1f", it) } ?: "8.0"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = accentColor
                                )
                                Text(
                                    text = details.releaseDate?.take(4) ?: details.firstAirDate?.take(4) ?: "2026",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = if (isTv) "${details.numberOfSeasons ?: 1} Seasons" else "${details.runtime ?: 120} min",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isTv) {
                                            viewModel.playEpisode(details.id, details.name ?: "TV Series", 1, 1, details.posterPath)
                                        } else {
                                            viewModel.playMovie(details.id, details.title ?: "Movie", details.posterPath)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("detail_play_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Play Now")
                                }

                                val isWatch = viewModel.isWatchlisted(details.id)
                                OutlinedButton(
                                    onClick = {
                                        val tmdbItem = TmdbItem(
                                            id = details.id,
                                            title = details.title ?: details.name ?: "Untitled",
                                            name = details.name,
                                            posterPath = details.posterPath,
                                            backdropPath = details.backdropPath,
                                            voteAverage = details.voteAverage,
                                            overview = details.overview,
                                            mediaType = if (isTv) "tv" else "movie"
                                        )
                                        viewModel.toggleWatchlist(tmdbItem)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                                ) {
                                    Icon(
                                        imageVector = if (isWatch) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = "Watchlist"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isWatch) "In Watchlist" else "Watchlist")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Overview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = details.overview ?: "No synopsis available.",
                                fontSize = 14.sp,
                                color = textPrimary.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                        }
                    }

                    val cast = details.credits?.cast
                    if (cast != null && cast.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Top Cast",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(cast) { member ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(80.dp)
                                        ) {
                                            val castImgUrl = if (member.profilePath != null) {
                                                if (member.profilePath.startsWith("http")) member.profilePath else "https://image.tmdb.org/t/p/w185${member.profilePath}"
                                            } else {
                                                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=60"
                                            }
                                            AsyncImage(
                                                model = castImgUrl,
                                                contentDescription = member.name,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = member.name ?: "",
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = textPrimary
                                            )
                                            Text(
                                                text = member.character ?: "",
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isTv) {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Seasons & Episodes",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                for (ep in 1..8) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                viewModel.playEpisode(details.id, details.name ?: "TV Series", 1, ep, details.posterPath)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1B1D22) else Color(0xFFF0F0F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayCircle,
                                                contentDescription = "Play",
                                                tint = accentColor
                                            )
                                            Column {
                                                Text(
                                                    text = "Episode $ep: Title of Episode $ep",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary
                                                )
                                                Text(
                                                    text = "Season 1 • Ep $ep • 45 min",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LivePlayerScreen(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val context = LocalContext.current
    val streamUrl = viewModel.activeStreamUrl ?: "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.m3u8"
    val title = viewModel.activePlayerTitle

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = {
                        viewModel.activePlayerMediaId = null
                        viewModel.activeStreamUrl = null
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Serif
                    )
                    if (viewModel.activePlayerMediaType == "tv") {
                        Text(
                            text = "Season ${viewModel.activePlayerSeason} • Episode ${viewModel.activePlayerEpisode}",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Live Stream Video Feed",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveTvScreen(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val context = LocalContext.current
    val customPlaylists by viewModel.customPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedPlaylistId by remember { mutableStateOf("jio-tv") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddPlaylistDialog by remember { mutableStateOf(false) }

    val jioTV = MockData.defaultIpTVPlaylists[0]
    val jioTVPlus = MockData.defaultIpTVPlaylists[1]
    val zee5 = MockData.defaultIpTVPlaylists[2]

    val playlists = listOf(
        Pair("jio-tv", jioTV["name"] ?: "Jio TV"),
        Pair("jio-tv-plus", jioTVPlus["name"] ?: "Jio TV+"),
        Pair("zee5-live", zee5["name"] ?: "ZEE5 Live")
    ) + customPlaylists.map { Pair(it.id, it.name) }

    val activeChannels = when (selectedPlaylistId) {
        "jio-tv" -> MockData.jioChannels
        "jio-tv-plus" -> MockData.liveChannels
        "zee5-live" -> MockData.liveChannels.filter { it["group"] != "Sports" }
        else -> MockData.liveChannels
    }

    val categories = listOf("All") + activeChannels.map { it["group"] ?: "Entertainment" }.distinct()
    val filteredChannels = if (selectedCategory == "All") {
        activeChannels
    } else {
        activeChannels.filter { it["group"] == selectedCategory }
    }

    val textPrimary = if (isDark) Color.White else Color.Black
    val surfaceColor = if (isDark) Color(0xFF101216) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live IPTV Stream TV",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = accentColor
                )
                Text(
                    text = "Streaming dynamically synced premium playlists",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(
                onClick = { showAddPlaylistDialog = true },
                modifier = Modifier.background(accentColor.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add custom list", tint = accentColor)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(playlists) { (id, name) ->
                val isSelected = selectedPlaylistId == id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedPlaylistId = id
                        selectedCategory = "All"
                    },
                    label = { Text(name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.Black,
                        containerColor = if (isDark) Color(0xFF1B1D22) else Color(0xFFEBEBEB)
                    )
                )
            }
        }

        if (categories.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    AssistChip(
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                            labelColor = if (isSelected) accentColor else textPrimary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        if (filteredChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No channels available.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (viewModel.layoutDensity == "compact") 3 else 2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(filteredChannels) { index, channel ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.activePlayerMediaId = 1000 + index
                                viewModel.activePlayerTitle = channel["name"] ?: "Channel"
                                viewModel.activePlayerMediaType = "live"
                                viewModel.activePlayerPoster = channel["logo"]
                                viewModel.activeStreamUrl = channel["url"]
                            }
                            .testTag("channel_card_${index}"),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1B1D22) else Color(0xFFF7F7F7))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = channel["logo"],
                                    contentDescription = channel["name"],
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = channel["name"] ?: "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = channel["group"] ?: "",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showAddPlaylistDialog) {
            var inputName by remember { mutableStateOf("") }
            var inputUrl by remember { mutableStateOf("") }
            var inputLogo by remember { mutableStateOf("") }
            var inputDesc by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddPlaylistDialog = false },
                title = { Text("Add IPTV Custom Playlist", fontFamily = FontFamily.Serif) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            label = { Text("M3U Playlist URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputLogo,
                            onValueChange = { inputLogo = it },
                            label = { Text("Logo Image URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputDesc,
                            onValueChange = { inputDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        onClick = {
                            if (inputName.isNotBlank() && inputUrl.isNotBlank()) {
                                viewModel.addCustomPlaylist(
                                    name = inputName,
                                    url = inputUrl,
                                    logo = if (inputLogo.isBlank()) "https://img.icons8.com/color/120/video.png" else inputLogo,
                                    description = inputDesc
                                )
                                showAddPlaylistDialog = false
                            } else {
                                Toast.makeText(context, "Please fill in Name and URL", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Add", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPlaylistDialog = false }) {
                        Text("Cancel", color = textPrimary)
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileScreen(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val watchlist by viewModel.watchlistItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by viewModel.historyItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val textPrimary = if (isDark) Color.White else Color.Black
    val containerBg = if (isDark) Color(0xFF101216) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.02f))))
                .border(1.dp, accentColor.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color.Black
                    )
                }
                Column {
                    Text(
                        text = "ElitePlex Member",
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Tier: Premium Streaming VIP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = containerBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = watchlist.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    Text(text = "Watchlist size", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = containerBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = history.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    Text(text = "Played streams", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Text(
            text = "Your Watchlist Favorites",
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerBg)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No items added to watchlist yet.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(watchlist) { item ->
                    Card(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { viewModel.openMediaDetails(item.id, item.mediaType) },
                        colors = CardDefaults.cardColors(containerColor = containerBg)
                    ) {
                        Column {
                            val watchListImgUrl = if (item.posterPath != null) {
                                if (item.posterPath.startsWith("http")) item.posterPath else "https://image.tmdb.org/t/p/w320${item.posterPath}"
                            } else {
                                "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?auto=format&fit=crop&q=80&w=320"
                            }
                            AsyncImage(
                                model = watchListImgUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Played Streams",
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            if (history.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear All", fontSize = 12.sp, color = accentColor)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerBg)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No playing history logs recorded.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                history.take(10).forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openMediaDetails(item.mediaId, item.mediaType) },
                        colors = CardDefaults.cardColors(containerColor = containerBg)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = item.posterPath ?: "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=100&auto=format&fit=crop&q=60",
                                contentDescription = item.title,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.mediaType == "tv") "S${item.season} • E${item.episode}" else "Movie Stream",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play",
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MovieViewModel, accentColor: Color, isDark: Boolean) {
    val textPrimary = if (isDark) Color.White else Color.Black
    val containerBg = if (isDark) Color(0xFF101216) else Color.White
    val context = LocalContext.current

    val colorsList = listOf(
        Pair("#D4AF37", "Royal Gold"),
        Pair("#DF3F5E", "Crimson Red"),
        Pair("#00FFCC", "Cyber Teal"),
        Pair("#1E90FF", "Ocean Blue"),
        Pair("#4CAF50", "Emerald Green")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App settings header
        Text(
            text = "ElitePlex Custom Preferences",
            fontFamily = FontFamily.Serif,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = accentColor,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Accent choice Row Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = containerBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Aesthetic Accent Accentuation",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Pick a display highlights scheme hue.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorsList.forEach { (hex, name) ->
                        val itemColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = viewModel.accentColor == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(itemColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateAccentColor(hex)
                                }
                        )
                    }
                }
            }
        }

        // Theme and density toggles
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = containerBg)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Dark Visual Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(text = "Reduces strain in ambient environments", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleThemeMode() },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }

                Divider(color = Color.Gray.copy(alpha = 0.15f))

                // Density preferences
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "High Density Layout Grid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(text = "Loads more assets in dense compactness columns", fontSize = 11.sp, color = Color.Gray)
                    }
                    val isCompact = viewModel.layoutDensity == "compact"
                    Switch(
                        checked = isCompact,
                        onCheckedChange = { isChecked ->
                            viewModel.updateLayoutDensity(if (isChecked) "compact" else "default")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }
            }
        }

        // Advanced mutations
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp),
            colors = CardDefaults.cardColors(containerColor = containerBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Database Operations",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Perform critical storage wipes on the application preferences, IPTV lists and watchlist bookmarks.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick = {
                        viewModel.wipeAllData()
                        Toast.makeText(context, "All data storage wiped", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDF3F5E), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Wipe")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Factory Sync Reset")
                }
            }
        }
    }
}
