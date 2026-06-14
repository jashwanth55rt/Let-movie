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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
                    icon = { Icon(Icons.Default.Film, "Movies") },
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
            icon = { Icon(Icons.Default.Film, "Movies") },
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

                    // Infinite Scroll triggers:
                    if (index == items.size - 1 && !viewModel.isDashboardLoading) {
                        LaunchedEffect(key1 = true) {
                            viewModel.loadDashboardData()
                        }
                    }
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
