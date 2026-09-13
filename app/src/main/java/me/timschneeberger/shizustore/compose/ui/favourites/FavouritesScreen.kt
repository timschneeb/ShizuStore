/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.favourites

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppRowSkeleton
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.RemovableListItem
import me.timschneeberger.shizustore.compose.composable.ScrollHint
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.composable.app.AppListItem
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
    val hasStarredPackages by viewModel.hasStarredPackages.collectAsStateWithLifecycle()

    val isListEmpty = apps.itemCount == 0
    val isInitialLoad = isListEmpty &&
        (apps.loadState.refresh is LoadState.Loading || hasStarredPackages == null)
    val isEmpty = isListEmpty &&
        apps.loadState.refresh is LoadState.NotLoading &&
        hasStarredPackages != null

    val listState = rememberLazyListState()
    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_favourites),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        val contentPhase = when {
            isInitialLoad -> ContentPhase.Loading
            isEmpty -> ContentPhase.Empty
            else -> ContentPhase.Loaded
        }

        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "FavouritesScreenContent"
            ) { phase ->
                when (phase) {
                    ContentPhase.Loading -> AppRowSkeleton(contentPadding = listPadding)

                    ContentPhase.Empty -> Placeholder(
                        painter = painterResource(R.drawable.ic_favorite_unchecked),
                        message = stringResource(
                            if (hasStarredPackages == true) {
                                R.string.favourites_unavailable
                            } else {
                                R.string.favourites_empty
                            }
                        ),
                        detail = stringResource(
                            if (hasStarredPackages == true) {
                                R.string.unavailable_detail
                            } else {
                                R.string.favourites_empty_detail
                            }
                        )
                    )

                    ContentPhase.Loaded -> Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = listPadding
                        ) {
                            items(
                                count = apps.itemCount,
                                key = apps.itemKey { it.slug }
                            ) { index ->
                                apps[index]?.let { app ->
                                    FavouriteItem(
                                        app = app,
                                        onClick = {
                                            onNavigateTo(Destination.AppDetails(app.packageName))
                                        },
                                        onRemove = { viewModel.unfavourite(app.packageName) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }

                        ScrollHint(
                            listState = listState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavouriteItem(
    app: ResolvedApp,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    RemovableListItem(onRemove = onRemove, modifier = modifier) { triggerRemove ->
        AppListItem(
            app = app,
            onClick = onClick,
            trailing = {
                IconButton(onClick = triggerRemove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_favorite_checked),
                        contentDescription = stringResource(R.string.action_unfavourite)
                    )
                }
            }
        )
    }
}
