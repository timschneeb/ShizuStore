/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's DownloadsScreen.
 */

package me.timschneeberger.shizustore.compose.ui.downloads

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.AppRowSkeleton
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.ScrollHint
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.composable.app.DownloadListItem
import me.timschneeberger.shizustore.compose.composable.app.toRowState
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.compose.ui.details.composable.installRefusalText
import me.timschneeberger.shizustore.compose.ui.downloads.composable.DownloadActionsSheet
import me.timschneeberger.shizustore.compose.ui.downloads.composable.DownloadsMenu
import me.timschneeberger.shizustore.compose.ui.downloads.composable.DownloadsMenuItem
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.viewmodel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val context = LocalContext.current
    val downloads = viewModel.downloads.collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }
    val exportDone = stringResource(R.string.download_export_done)
    val exportFailed = stringResource(R.string.download_export_failed)

    var actionsTarget by remember { mutableStateOf<Download?>(null) }
    var pendingExport by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(APK_MIME_TYPE),
        onResult = { target ->
            val packageName = pendingExport
            pendingExport = null
            if (target != null && packageName != null) viewModel.export(packageName, target)
        }
    )

    LaunchedEffect(viewModel) {
        viewModel.refusals.collect {
            snackbarHostState.showSnackbar(installRefusalText(context, it))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.exports.collect { exported ->
            snackbarHostState.showSnackbar(if (exported) exportDone else exportFailed)
        }
    }

    val listState = rememberLazyListState()
    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_downloads),
                onNavigateBack = { onNavigateTo(Destination.Back) },
                actions = {
                    if (downloads.itemCount > 0) {
                        DownloadsMenu { item ->
                            when (item) {
                                DownloadsMenuItem.CANCEL_ALL -> viewModel.cancelAll()
                                DownloadsMenuItem.CLEAR_FINISHED -> viewModel.clearFinished()
                                DownloadsMenuItem.CLEAR_ALL -> viewModel.clearAll()
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        DownloadsContent(
            downloads = downloads,
            listState = listState,
            listPadding = listPadding,
            onItemClick = { actionsTarget = it },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        )
    }

    actionsTarget?.let { target ->
        DownloadActionsSheet(
            download = target,
            canInstall = viewModel.canInstall(target),
            onShowDetails = { onNavigateTo(Destination.AppDetails(target.packageName)) },
            onCancel = { viewModel.cancel(target.packageName) },
            onInstall = { viewModel.install(target.packageName) },
            onExport = {
                pendingExport = target.packageName
                exportLauncher.launch("${target.packageName}_${target.versionCode}.apk")
            },
            onClear = { viewModel.clear(target.packageName) },
            onDismiss = { actionsTarget = null }
        )
    }
}

/**
 * Owns the paging state reads so page loads only invalidate this content, not
 * the whole screen (top bar, snackbar host, sheet).
 */
@Composable
private fun DownloadsContent(
    downloads: LazyPagingItems<Download>,
    listState: LazyListState,
    listPadding: PaddingValues,
    onItemClick: (Download) -> Unit,
    modifier: Modifier = Modifier
) {
    val isInitialLoad = downloads.loadState.refresh is LoadState.Loading &&
        downloads.itemCount == 0
    val isEmpty = downloads.loadState.refresh is LoadState.NotLoading &&
        downloads.itemCount == 0

    val contentPhase = when {
        isInitialLoad -> ContentPhase.Loading
        isEmpty -> ContentPhase.Empty
        else -> ContentPhase.Loaded
    }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = contentPhase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "DownloadsScreenContent"
        ) { phase ->
            when (phase) {
                ContentPhase.Loading -> AppRowSkeleton(contentPadding = listPadding)

                ContentPhase.Empty -> Placeholder(
                    painter = painterResource(R.drawable.ic_download_manager),
                    message = stringResource(R.string.downloads_empty),
                    detail = stringResource(R.string.downloads_empty_detail)
                )

                ContentPhase.Loaded -> Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = listPadding
                    ) {
                        items(
                            count = downloads.itemCount,
                            key = downloads.itemKey { it.packageName }
                        ) { index ->
                            downloads[index]?.let { download ->
                                DownloadListItem(
                                    state = download.toRowState(),
                                    onClick = { onItemClick(download) },
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

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
