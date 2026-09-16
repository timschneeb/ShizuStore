/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.blacklist

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppListScaffold
import me.timschneeberger.shizustore.compose.composable.RemovableAppItem
import me.timschneeberger.shizustore.compose.navigation.Destination
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

    AppListScaffold(
        title = stringResource(R.string.title_blacklist),
        contentPhase = when {
            apps == null || hasBlacklistedPackages == null -> ContentPhase.Loading
            apps.isNullOrEmpty() -> ContentPhase.Empty
            else -> ContentPhase.Loaded
        },
        listState = listState,
        emptyPainter = painterResource(R.drawable.ic_block),
        emptyMessage = stringResource(
            if (hasBlacklistedPackages == true) {
                R.string.blacklist_unavailable
            } else {
                R.string.blacklist_empty
            }
        ),
        emptyDetail = stringResource(R.string.blacklist_empty_detail),
        transitionLabel = "BlacklistScreenContent",
        onNavigateBack = { onNavigateTo(Destination.Back) },
        modifier = modifier
    ) {
        items(
            items = apps.orEmpty(),
            key = { it.slug }
        ) { app ->
            RemovableAppItem(
                app = app,
                onClick = {
                    onNavigateTo(Destination.AppDetails(app.packageName))
                },
                onRemove = { viewModel.unblacklist(app.packageName) },
                icon = painterResource(R.drawable.ic_block),
                contentDescription = stringResource(R.string.action_unblacklist),
                modifier = Modifier.animateItem()
            )
        }
    }
}
