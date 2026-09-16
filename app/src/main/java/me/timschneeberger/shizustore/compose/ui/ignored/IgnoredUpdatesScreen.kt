/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.ignored

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

    AppListScaffold(
        title = stringResource(R.string.title_ignored_updates),
        contentPhase = when {
            ignored == null || hasIgnoredPackages == null -> ContentPhase.Loading
            ignored.isNullOrEmpty() -> ContentPhase.Empty
            else -> ContentPhase.Loaded
        },
        listState = listState,
        emptyPainter = painterResource(R.drawable.ic_updates),
        emptyMessage = stringResource(
            if (hasIgnoredPackages == true) {
                R.string.ignored_updates_unavailable
            } else {
                R.string.ignored_updates_empty
            }
        ),
        emptyDetail = stringResource(R.string.ignored_updates_empty_detail),
        transitionLabel = "IgnoredUpdatesScreenContent",
        onNavigateBack = { onNavigateTo(Destination.Back) },
        modifier = modifier
    ) {
        items(
            items = ignored.orEmpty(),
            key = { it.app.slug }
        ) { row ->
            val caption = row.ignoredVersionCode
                ?.let { stringResource(R.string.ignored_version, it) }
                ?: stringResource(R.string.ignored_all)

            RemovableAppItem(
                app = row.app,
                onClick = {
                    onNavigateTo(Destination.AppDetails(row.app.packageName))
                },
                onRemove = { viewModel.stopIgnoring(row.app.packageName) },
                icon = painterResource(R.drawable.ic_updates),
                contentDescription = stringResource(
                    if (row.ignoredVersionCode == null) {
                        R.string.action_stop_ignoring_all
                    } else {
                        R.string.action_stop_ignoring_version
                    }
                ),
                supporting = caption,
                modifier = Modifier.animateItem()
            )
        }
    }
}
