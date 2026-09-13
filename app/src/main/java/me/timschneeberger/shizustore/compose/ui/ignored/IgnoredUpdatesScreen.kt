/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.ignored

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
import me.timschneeberger.shizustore.viewmodel.IgnoredUpdate
import me.timschneeberger.shizustore.viewmodel.IgnoredUpdatesViewModel

@Composable
fun IgnoredUpdatesScreen(
    modifier: Modifier = Modifier,
    viewModel: IgnoredUpdatesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val ignored by viewModel.ignored.collectAsStateWithLifecycle()
    val hasIgnoredPackages by viewModel.hasIgnoredPackages.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))

    val contentPhase = when {
        ignored == null || hasIgnoredPackages == null -> ContentPhase.Loading
        ignored.isNullOrEmpty() -> ContentPhase.Empty
        else -> ContentPhase.Loaded
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_ignored_updates),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "IgnoredUpdatesScreenContent"
            ) { phase ->
                when (phase) {
                    ContentPhase.Loading -> AppRowSkeleton(contentPadding = listPadding)

                    ContentPhase.Empty -> Placeholder(
                        painter = painterResource(R.drawable.ic_updates),
                        message = stringResource(
                            if (hasIgnoredPackages == true) {
                                R.string.ignored_updates_unavailable
                            } else {
                                R.string.ignored_updates_empty
                            }
                        ),
                        detail = stringResource(
                            if (hasIgnoredPackages == true) {
                                R.string.unavailable_detail
                            } else {
                                R.string.ignored_updates_empty_detail
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
                                items = ignored.orEmpty(),
                                key = { it.app.slug }
                            ) { row ->
                                IgnoredUpdateItem(
                                    row = row,
                                    onClick = {
                                        onNavigateTo(
                                            Destination.AppDetails(row.app.packageName)
                                        )
                                    },
                                    onRemove = {
                                        viewModel.stopIgnoring(row.app.packageName)
                                    },
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
private fun IgnoredUpdateItem(
    row: IgnoredUpdate,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val caption = row.ignoredVersionCode
        ?.let { stringResource(R.string.ignored_version, it) }
        ?: stringResource(R.string.ignored_all)

    RemovableListItem(onRemove = onRemove, modifier = modifier) { triggerRemove ->
        AppListItem(
            app = row.app,
            onClick = onClick,
            supporting = caption,
            trailing = {
                IconButton(onClick = triggerRemove) {
                    Icon(
                        painter = painterResource(R.drawable.ic_updates),
                        contentDescription = stringResource(
                            if (row.ignoredVersionCode == null) {
                                R.string.action_stop_ignoring_all
                            } else {
                                R.string.action_stop_ignoring_version
                            }
                        )
                    )
                }
            }
        )
    }
}
