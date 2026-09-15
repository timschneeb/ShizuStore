/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store.
 */

package me.timschneeberger.shizustore.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.compose.permission.rememberNotificationPermissionRequest
import me.timschneeberger.shizustore.compose.ui.applist.AppListScreen
import me.timschneeberger.shizustore.compose.ui.apps.AppsScreen
import me.timschneeberger.shizustore.compose.ui.updates.UpdatesScreen
import me.timschneeberger.shizustore.data.model.AppListArgs
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
    onNavigateTo: (Destination) -> Unit = {}
) {
    val updateCount by updatesViewModel.updateCount.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, MainTab.entries.size - 1)
    ) { MainTab.entries.size }

    val requestNotifications = rememberNotificationPermissionRequest()
    LaunchedEffect(Unit) { requestNotifications() }

    var showMoreSheet by remember { mutableStateOf(false) }
    // Bumped only by the top-bar search action so the Search tab focuses its field there,
    // not when the user arrives via the bottom navigation.
    var searchFocusRequest by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            // The Search tab provides its own search field in the bar.
            if (MainTab.entries[pagerState.currentPage] != MainTab.SEARCH) {
                TopAppBar(
                    title = stringResource(MainTab.entries[pagerState.currentPage].labelRes),
                    showNavigationIcon = false,
                    actions = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(MainTab.SEARCH.ordinal)
                                    searchFocusRequest++
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.action_search)
                            )
                        }
                        IconButton(onClick = { onNavigateTo(Destination.Downloads) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download_manager),
                                contentDescription = stringResource(R.string.title_downloads)
                            )
                        }
                        IconButton(onClick = { showMoreSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_outlined),
                                contentDescription = stringResource(R.string.action_more)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
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
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (MainTab.entries[page]) {
                    MainTab.APPS -> AppsScreen(onNavigateTo = onNavigateTo)
                    MainTab.SEARCH -> AppListScreen(
                        args = AppListArgs(),
                        searchHome = true,
                        searchFocusRequest = searchFocusRequest,
                        onNavigateTo = onNavigateTo
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
