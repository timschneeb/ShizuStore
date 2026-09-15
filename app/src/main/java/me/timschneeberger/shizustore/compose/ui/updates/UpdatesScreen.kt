/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.updates

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppRowSkeleton
import me.timschneeberger.shizustore.compose.composable.ExpressivePullToRefreshBox
import me.timschneeberger.shizustore.compose.composable.OfflineBanner
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.app.AppUpdateItem
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.compose.ui.updates.composable.AppUpdateSheet
import me.timschneeberger.shizustore.extensions.appInfo
import me.timschneeberger.shizustore.extensions.uninstallPackage
import me.timschneeberger.shizustore.viewmodel.UpdatesViewModel

@Composable
fun UpdatesScreen(
    modifier: Modifier = Modifier,
    viewModel: UpdatesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val apps = viewModel.updates.collectAsLazyPagingItems()
    val downloads by viewModel.downloadsByPackage.collectAsStateWithLifecycle()
    val updateCount by viewModel.updateCount.collectAsStateWithLifecycle()
    val anyActive by viewModel.anyDownloadActive.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val syncFailure by viewModel.syncFailure.collectAsStateWithLifecycle()
    val blacklisted by viewModel.blacklisted.collectAsStateWithLifecycle()
    val sheetApp by viewModel.sheetApp.collectAsStateWithLifecycle()
    val sheetWhatsNew by viewModel.sheetWhatsNew.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isInitialLoad = apps.loadState.refresh is LoadState.Loading && apps.itemCount == 0
    val isEmpty = apps.loadState.refresh is LoadState.NotLoading && apps.itemCount == 0

    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.fab_clearance))

    // Saveable (not plain remember): the entry leaves composition while a
    // detail screen is on top, and only rememberSaveable survives the return.
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val contentPhase = when {
        isInitialLoad -> ContentPhase.Loading
        isEmpty -> ContentPhase.Empty
        else -> ContentPhase.Loaded
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (syncFailure != null) {
                OfflineBanner(onRetry = viewModel::retrySync)
            }

            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "UpdatesScreenContent",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { phase ->
                when (phase) {
                    ContentPhase.Loading -> Column(modifier = Modifier.fillMaxSize()) {
                        UpdatesHeader(
                            count = updateCount,
                            anyActive = anyActive,
                            enabled = false,
                            onUpdateAll = viewModel::updateAll,
                            onCancelAll = viewModel::cancelAll
                        )

                        AppRowSkeleton(
                            modifier = Modifier.weight(1f),
                            contentPadding = listPadding,
                            showTrailing = true
                        )
                    }

                    ContentPhase.Empty -> Placeholder(
                        painter = painterResource(R.drawable.ic_updates),
                        message = stringResource(R.string.updates_empty)
                    )

                    ContentPhase.Loaded -> Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.alpha(if (refreshing) 0f else 1f)
                        ) {
                            UpdatesHeader(
                                count = updateCount,
                                anyActive = anyActive,
                                enabled = !refreshing,
                                onUpdateAll = viewModel::updateAll,
                                onCancelAll = viewModel::cancelAll
                            )
                        }

                        ExpressivePullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = viewModel::retrySync,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            placeholder = {
                                AppRowSkeleton(
                                    contentPadding = listPadding,
                                    showTrailing = true
                                )
                            }
                        ) {
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
                                        AppUpdateItem(
                                            app = app,
                                            download = downloads[app.installedPackage ?: app.packageName],
                                            onClick = { viewModel.openSheet(app) },
                                            onUpdate = { viewModel.update(app) },
                                            onCancel = {
                                                viewModel.cancel(app.installedPackage ?: app.packageName)
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

        sheetApp?.let { app ->
            AppUpdateSheet(
                app = app,
                isBlacklisted = app.packageName in blacklisted,
                whatsNew = sheetWhatsNew,
                onAppDetails = { onNavigateTo(Destination.AppDetails(app.packageName)) },
                onIgnoreAllUpdates = { viewModel.ignoreAllUpdates(app) },
                onIgnoreThisVersion = { viewModel.ignoreThisVersion(app) },
                onToggleBlacklist = { viewModel.toggleBlacklist(app) },
                onUninstall = { context.uninstallPackage(app.installedPackage ?: app.packageName) },
                onAppInfo = { context.appInfo(app.installedPackage ?: app.packageName) },
                onDismiss = viewModel::dismissSheet
            )
        }
    }
}

@Composable
private fun UpdatesHeader(
    count: Int,
    anyActive: Boolean,
    enabled: Boolean,
    onUpdateAll: () -> Unit,
    onCancelAll: () -> Unit
) {
    SectionHeader(
        title = pluralStringResource(R.plurals.updates_available_count, count, count),
        trailing = {
            TextButton(
                onClick = if (anyActive) onCancelAll else onUpdateAll,
                enabled = enabled
            ) {
                Text(
                    stringResource(
                        if (anyActive) R.string.action_cancel_all else R.string.action_update_all
                    )
                )
            }
        }
    )
}
