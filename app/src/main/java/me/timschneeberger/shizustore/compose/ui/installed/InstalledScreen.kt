/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.installed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppRowSkeleton
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.TopAppBar
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

    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))

    val contentPhase = when {
        isInitialLoad -> ContentPhase.Loading
        isEmpty -> ContentPhase.Empty
        else -> ContentPhase.Loaded
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_my_apps),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "InstalledScreenContent"
            ) { phase ->
                when (phase) {
                    ContentPhase.Loading -> AppRowSkeleton(contentPadding = listPadding)

                    ContentPhase.Empty -> Placeholder(
                        painter = painterResource(R.drawable.ic_installed),
                        message = stringResource(R.string.installed_empty)
                    )

                    ContentPhase.Loaded -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listPadding
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
            }
        }
    }
}
