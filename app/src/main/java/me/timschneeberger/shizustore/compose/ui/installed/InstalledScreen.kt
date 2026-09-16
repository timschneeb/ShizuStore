/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.installed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppListScaffold
import me.timschneeberger.shizustore.compose.composable.app.AppListItem
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.InstalledViewModel

@Composable
fun InstalledScreen(
    modifier: Modifier = Modifier,
    viewModel: InstalledViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val apps = viewModel.installed.collectAsLazyPagingItems()

    val isInitialLoad = apps.loadState.refresh is LoadState.Loading && apps.itemCount == 0
    val isEmpty = apps.loadState.refresh is LoadState.NotLoading && apps.itemCount == 0

    // Saveable (not plain remember): the entry leaves composition while a
    // detail screen is on top, and only rememberSaveable survives the return.
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    AppListScaffold(
        title = stringResource(R.string.title_my_apps),
        contentPhase = when {
            isInitialLoad -> ContentPhase.Loading
            isEmpty -> ContentPhase.Empty
            else -> ContentPhase.Loaded
        },
        listState = listState,
        emptyPainter = painterResource(R.drawable.ic_installed),
        emptyMessage = stringResource(R.string.installed_empty),
        showScrollHint = false,
        transitionLabel = "InstalledScreenContent",
        onNavigateBack = { onNavigateTo(Destination.Back) },
        modifier = modifier
    ) {
        items(
            count = apps.itemCount,
            key = apps.itemKey { it.slug }
        ) { index ->
            apps[index]?.let { app ->
                AppListItem(
                    app = app,
                    onClick = {
                        onNavigateTo(Destination.AppDetails(app.packageName))
                    }
                )
            }
        }
    }
}
