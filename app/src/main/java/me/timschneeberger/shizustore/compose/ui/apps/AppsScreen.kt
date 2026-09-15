/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.apps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppCarousel
import me.timschneeberger.shizustore.compose.composable.AppCarouselSkeleton
import me.timschneeberger.shizustore.compose.composable.ExpressivePullToRefreshBox
import me.timschneeberger.shizustore.compose.composable.OfflineBanner
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.ShizukuPromptCard
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.data.model.AppSort
import me.timschneeberger.shizustore.viewmodel.AppGroup
import me.timschneeberger.shizustore.viewmodel.AppGroupKind
import me.timschneeberger.shizustore.viewmodel.AppsViewModel

@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val syncFailure by viewModel.syncFailure.collectAsStateWithLifecycle()
    val shizukuDismissed by viewModel.shizukuCardDismissed.collectAsStateWithLifecycle()

    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.fab_clearance))

    Column(modifier = modifier.fillMaxSize()) {
        if (syncFailure != null) {
            OfflineBanner(onRetry = viewModel::retrySync)
        }

        if (!shizukuDismissed) {
            ShizukuPromptCard(onDismiss = viewModel::dismissShizukuCard)
        }

        val known = groups
        val contentPhase = when {
            known == null -> ContentPhase.Loading
            known.isEmpty() -> ContentPhase.Empty
            else -> ContentPhase.Loaded
        }

        ExpressivePullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::retrySync,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            placeholder = { AppCarouselSkeleton(contentPadding = listPadding) }
        ) {
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "AppsScreenContent"
            ) { phase ->
                when (phase) {
                    ContentPhase.Loading -> AppCarouselSkeleton(contentPadding = listPadding)

                    ContentPhase.Empty -> Placeholder(
                        painter = painterResource(R.drawable.ic_apps),
                        message = if (syncFailure != null) {
                            stringResource(R.string.apps_sync_failed)
                        } else {
                            stringResource(R.string.apps_empty)
                        },
                        detail = if (syncFailure != null) {
                            null
                        } else {
                            stringResource(
                                if (syncing) {
                                    R.string.apps_empty_syncing
                                } else {
                                    R.string.apps_empty_detail
                                }
                            )
                        },
                        inProgress = syncing && syncFailure == null,
                        actionLabel = if (syncFailure != null) {
                            stringResource(R.string.action_retry)
                        } else {
                            null
                        },
                        onAction = if (syncFailure != null) viewModel::retrySync else null
                    )

                    ContentPhase.Loaded -> AppCarousel(
                        groups = known.orEmpty(),
                        contentPadding = listPadding,
                        onGroupClick = { group ->
                            group.moreDestination()?.let(onNavigateTo)
                        },
                        onAppClick = { onNavigateTo(Destination.AppDetails(it.packageName)) }
                    )
                }
            }
        }
    }
}

private fun AppGroup.moreDestination(): Destination? = when (kind) {
    AppGroupKind.RECOMMENDED -> Destination.AppList(recommended = true)
    AppGroupKind.RECENTLY_ADDED -> Destination.AppList(sort = AppSort.RECENTLY_ADDED)
    AppGroupKind.RECENTLY_UPDATED -> Destination.AppList(sort = AppSort.RECENTLY_UPDATED)
    AppGroupKind.MOST_STARRED -> Destination.AppList(sort = AppSort.STARS)
    AppGroupKind.RANDOM_PICKS -> null
    AppGroupKind.CATEGORY -> Destination.AppList(categorySlug = category)
}
