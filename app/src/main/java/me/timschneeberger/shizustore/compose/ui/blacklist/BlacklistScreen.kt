/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.blacklist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import me.timschneeberger.shizustore.viewmodel.BlacklistViewModel

@Composable
fun BlacklistScreen(
    modifier: Modifier = Modifier,
    viewModel: BlacklistViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val hasBlacklistedPackages by viewModel.hasBlacklistedPackages.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))

    val contentPhase = when {
        apps == null || hasBlacklistedPackages == null -> ContentPhase.Loading
        apps.isNullOrEmpty() -> ContentPhase.Empty
        else -> ContentPhase.Loaded
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_blacklist),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "BlacklistScreenContent"
            ) { phase ->
                when (phase) {
                    ContentPhase.Loading -> AppRowSkeleton(contentPadding = listPadding)

                    ContentPhase.Empty -> Placeholder(
                        painter = painterResource(R.drawable.ic_block),
                        message = stringResource(
                            if (hasBlacklistedPackages == true) {
                                R.string.blacklist_unavailable
                            } else {
                                R.string.blacklist_empty
                            }
                        ),
                        detail = stringResource(
                            if (hasBlacklistedPackages == true) {
                                R.string.unavailable_detail
                            } else {
                                R.string.blacklist_empty_detail
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
                                items = apps.orEmpty(),
                                key = { it.slug }
                            ) { app ->
                                BlacklistItem(
                                    app = app,
                                    onClick = {
                                        onNavigateTo(Destination.AppDetails(app.packageName))
                                    },
                                    onRemove = { viewModel.unblacklist(app.packageName) },
                                    modifier = Modifier.animateItem()
                                )
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
private fun BlacklistItem(
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
                        painter = painterResource(R.drawable.ic_block),
                        contentDescription = stringResource(R.string.action_unblacklist)
                    )
                }
            }
        )
    }
}
