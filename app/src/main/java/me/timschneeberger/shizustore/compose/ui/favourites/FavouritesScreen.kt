/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.favourites

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppListScaffold
import me.timschneeberger.shizustore.compose.composable.RemovableAppItem
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.viewmodel.FavouritesViewModel

@Composable
fun FavouritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavouritesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val apps = viewModel.favourites.collectAsLazyPagingItems()

    // Saveable (not plain remember): the entry leaves composition while a
    // detail screen is on top, and only rememberSaveable survives the return.
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    FavouritesContent(
        apps = apps,
        viewModel = viewModel,
        listState = listState,
        onNavigateTo = onNavigateTo,
        modifier = modifier
    )
}

/**
 * Owns the paging state reads so page loads only invalidate the list content,
 * not the navigation wrapper.
 */
@Composable
private fun FavouritesContent(
    apps: LazyPagingItems<ResolvedApp>,
    viewModel: FavouritesViewModel,
    listState: LazyListState,
    onNavigateTo: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasStarredPackages by viewModel.hasStarredPackages.collectAsStateWithLifecycle()

    val isListEmpty = apps.itemCount == 0
    val isInitialLoad = isListEmpty &&
        (apps.loadState.refresh is LoadState.Loading || hasStarredPackages == null)
    val isEmpty = isListEmpty &&
        apps.loadState.refresh is LoadState.NotLoading &&
        hasStarredPackages != null

    AppListScaffold(
        title = stringResource(R.string.title_favourites),
        contentPhase = when {
            isInitialLoad -> ContentPhase.Loading
            isEmpty -> ContentPhase.Empty
            else -> ContentPhase.Loaded
        },
        listState = listState,
        emptyPainter = painterResource(R.drawable.ic_favorite_unchecked),
        emptyMessage = stringResource(
            if (hasStarredPackages == true) {
                R.string.favourites_unavailable
            } else {
                R.string.favourites_empty
            }
        ),
        emptyDetail = stringResource(R.string.favourites_empty_detail),
        transitionLabel = "FavouritesScreenContent",
        onNavigateBack = { onNavigateTo(Destination.Back) },
        modifier = modifier
    ) {
        items(
            count = apps.itemCount,
            key = apps.itemKey { it.slug }
        ) { index ->
            apps[index]?.let { app ->
                RemovableAppItem(
                    app = app,
                    onClick = {
                        onNavigateTo(Destination.AppDetails(app.packageName))
                    },
                    onRemove = { viewModel.unfavourite(app.packageName) },
                    icon = painterResource(R.drawable.ic_favorite_checked),
                    contentDescription = stringResource(R.string.action_unfavourite),
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}
