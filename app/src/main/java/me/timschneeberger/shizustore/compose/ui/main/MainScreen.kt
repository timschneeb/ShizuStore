/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store.
 */

package me.timschneeberger.shizustore.compose.ui.main

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.compose.permission.rememberNotificationPermissionRequest
import me.timschneeberger.shizustore.compose.theme.motionEffectsSpec
import me.timschneeberger.shizustore.compose.theme.motionSpatialSpec
import me.timschneeberger.shizustore.compose.ui.applist.AppListScreen
import me.timschneeberger.shizustore.compose.ui.applist.AppSearchField
import me.timschneeberger.shizustore.compose.ui.apps.AppsScreen
import me.timschneeberger.shizustore.compose.ui.updates.UpdatesScreen
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.viewmodel.AppListViewModel
import me.timschneeberger.shizustore.viewmodel.UpdatesViewModel

internal enum class MainTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    APPS(R.string.title_apps, R.drawable.ic_apps),
    SEARCH(R.string.title_search, R.drawable.ic_search),
    UPDATES(R.string.title_updates, R.drawable.ic_updates)
}

@Composable
fun MainScreen(
    initialTab: Int = 0,
    updatesViewModel: UpdatesViewModel = hiltViewModel(),
    searchViewModel: AppListViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val updateCount by updatesViewModel.updateCount.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable {
        mutableStateOf(MainTab.entries[initialTab.coerceIn(0, MainTab.entries.size - 1)])
    }

    // Hoisted here: AnimatedContent's transition lambda is not composable. The
    // slide is spatial, so the Appearance toggle makes tab changes expressive;
    // the title crossfade stays on the matching effects curve.
    val tabSlideSpec = motionSpatialSpec<IntOffset>()
    val tabFadeSpec = motionEffectsSpec<Float>()

    val requestNotifications = rememberNotificationPermissionRequest()
    LaunchedEffect(Unit) { requestNotifications() }

    val configuration = LocalConfiguration.current
    val hasRemoteInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS ||
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val tabFocusRequesters = remember { List(MainTab.entries.size) { FocusRequester() } }
    LaunchedEffect(Unit) {
        // Touch-only phones keep their clean launch; remote and keyboard users
        // get a visible starting point for the first arrow press.
        if (hasRemoteInput) {
            tabFocusRequesters[currentTab.ordinal].requestFocus()
        }
    }

    var showMoreSheet by remember { mutableStateOf(false) }
    // Bumped only by the top-bar search action so the Search tab focuses its field there,
    // not when the user arrives via the bottom navigation.
    var searchFocusRequest by remember { mutableStateOf(0) }

    val openSearch: () -> Unit = {
        currentTab = MainTab.SEARCH
        searchFocusRequest++
    }
    val openDownloads: () -> Unit = { onNavigateTo(Destination.Downloads) }
    val openMore: () -> Unit = { showMoreSheet = true }

    val searchFieldState = rememberTextFieldState("")
    val searchFocusRequester = remember { FocusRequester() }
    val atSearchHome by searchViewModel.atSearchHome.collectAsStateWithLifecycle()

    // One top bar serves all three tabs; on Search it swaps its title for the
    // field. Keeping the bar in place means the scaffold padding never changes
    // between tabs, so the content slide below stays purely horizontal.
    Scaffold(
        topBar = {
            TopAppBar(
                titleContent = {
                    // The box centers both states on the same midline, so the
                    // field can only fade (and rise slightly on enter) instead
                    // of being dragged down with the slot as its height changes.
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(tabFadeSpec) togetherWith fadeOut(tabFadeSpec)
                            },
                            contentAlignment = Alignment.CenterStart,
                            label = "MainTopBarTitle"
                        ) { tab ->
                            if (tab != MainTab.SEARCH) {
                                Text(text = stringResource(tab.labelRes))
                            }
                        }
                        AnimatedVisibility(
                            visible = currentTab == MainTab.SEARCH,
                            enter = fadeIn(tabFadeSpec) +
                                slideInVertically(tabSlideSpec) { it / 2 },
                            exit = fadeOut(tabFadeSpec)
                        ) {
                            AppSearchField(
                                textFieldState = searchFieldState,
                                onQueryChange = searchViewModel::setQuery,
                                onSearchCleared = { searchViewModel.setQuery("") },
                                focusRequester = searchFocusRequester
                            )
                        }
                    }
                },
                navigationContent = {
                    if (currentTab == MainTab.SEARCH && !atSearchHome) {
                        IconButton(
                            onClick = {
                                searchFieldState.setTextAndPlaceCursorAtEnd("")
                                searchViewModel.clearAll()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                },
                showNavigationIcon = false,
                actions = {
                    AnimatedVisibility(
                        visible = currentTab != MainTab.SEARCH,
                        enter = fadeIn(tabFadeSpec),
                        exit = fadeOut(tabFadeSpec)
                    ) {
                        Row {
                            IconButton(onClick = openSearch) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = stringResource(R.string.action_search)
                                )
                            }
                            IconButton(onClick = openDownloads) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_download_manager),
                                    contentDescription = stringResource(R.string.title_downloads)
                                )
                            }
                            IconButton(onClick = openMore) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings_outlined),
                                    contentDescription = stringResource(R.string.action_more)
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        modifier = Modifier.focusRequester(tabFocusRequesters[index]),
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            val icon = painterResource(tab.iconRes)
                            if (tab == MainTab.UPDATES && updateCount > 0) {
                                BadgedBox(badge = { Badge { Text("$updateCount") } }) {
                                    Icon(painter = icon, contentDescription = null)
                                }
                            } else {
                                Icon(painter = icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentTab,
                // Direct slide between any two tabs; the pager used to glide
                // through the search page when jumping between apps and updates.
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(tabSlideSpec) { it } + fadeIn(tabFadeSpec)) togetherWith
                            (slideOutHorizontally(tabSlideSpec) { -it } + fadeOut(tabFadeSpec))
                    } else {
                        (slideInHorizontally(tabSlideSpec) { -it } + fadeIn(tabFadeSpec)) togetherWith
                            (slideOutHorizontally(tabSlideSpec) { it } + fadeOut(tabFadeSpec))
                    }
                },
                label = "MainTabContent",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    MainTab.APPS -> AppsScreen(onNavigateTo = onNavigateTo)
                    MainTab.SEARCH -> AppListScreen(
                        args = AppListArgs(),
                        searchHome = true,
                        searchFocusRequest = searchFocusRequest,
                        showTopBar = false,
                        textFieldState = searchFieldState,
                        focusRequester = searchFocusRequester,
                        onNavigateTo = onNavigateTo,
                        viewModel = searchViewModel
                    )
                    MainTab.UPDATES -> UpdatesScreen(
                        viewModel = updatesViewModel,
                        onNavigateTo = onNavigateTo
                    )
                }
            }
        }
    }

    if (showMoreSheet) {
        MoreSheet(
            onNavigateTo = onNavigateTo,
            onDismiss = { showMoreSheet = false }
        )
    }
}
