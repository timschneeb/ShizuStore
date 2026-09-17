/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.ExpressivePullToRefreshBox
import me.timschneeberger.shizustore.compose.composable.LoadingIndicatorBox
import me.timschneeberger.shizustore.compose.composable.OfflineBanner
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.compose.ui.details.composable.AppExclusionMenu
import me.timschneeberger.shizustore.compose.ui.details.composable.BillingNotice
import me.timschneeberger.shizustore.compose.ui.details.composable.ClosedSourceNotice
import me.timschneeberger.shizustore.compose.ui.details.composable.CompatibilityNotice
import me.timschneeberger.shizustore.compose.ui.details.composable.DetailsCarousel
import me.timschneeberger.shizustore.compose.ui.details.composable.DetailsHeader
import me.timschneeberger.shizustore.compose.ui.details.composable.DetailsTags
import me.timschneeberger.shizustore.compose.ui.details.composable.InstallAction
import me.timschneeberger.shizustore.compose.ui.details.composable.InstallActions
import me.timschneeberger.shizustore.compose.ui.details.composable.LinkList
import me.timschneeberger.shizustore.compose.ui.details.composable.ScreenshotGallery
import me.timschneeberger.shizustore.compose.ui.details.composable.SourceList
import me.timschneeberger.shizustore.compose.ui.details.composable.StoreNotice
import me.timschneeberger.shizustore.compose.ui.details.composable.installButtonState
import me.timschneeberger.shizustore.compose.ui.details.composable.installRefusalText
import me.timschneeberger.shizustore.compose.ui.details.composable.linkButtonState
import me.timschneeberger.shizustore.compose.ui.details.composable.requiresUnknownSourcesSettings
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.helper.SourceLauncher
import me.timschneeberger.shizustore.data.installer.AppInstaller
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.extensions.appInfo
import me.timschneeberger.shizustore.extensions.isOAndAbove
import me.timschneeberger.shizustore.extensions.shareApp
import me.timschneeberger.shizustore.extensions.uninstallPackage
import me.timschneeberger.shizustore.util.ShortcutUtil
import me.timschneeberger.shizustore.viewmodel.AppDetailsUiState
import me.timschneeberger.shizustore.viewmodel.AppDetailsViewModel

@Composable
fun AppDetailsScreen(
    packageName: String,
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detailError by viewModel.detailError.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val isFavourite by viewModel.isFavourite.collectAsStateWithLifecycle()
    val isBlacklisted by viewModel.isBlacklisted.collectAsStateWithLifecycle()
    val ignoredUpdate by viewModel.ignoredUpdate.collectAsStateWithLifecycle()
    val moreFromAuthor by viewModel.moreFromAuthor.collectAsStateWithLifecycle()
    val moreFromCategory by viewModel.moreFromCategory.collectAsStateWithLifecycle()
    val categorySlug by viewModel.categorySlug.collectAsStateWithLifecycle()
    val loadedState = uiState as? AppDetailsUiState.Loaded
    // Installed-app actions must target the flavor the user installed, not the
    // catalog's canonical package.
    val actionablePackage = loadedState?.actionablePackage ?: packageName
    // Keyed on the installed flag instead of the download status: the launch
    // intent only changes when installation settles, not on progress ticks.
    val canAddToHome =
        rememberCanOpen(actionablePackage, loadedState?.resolved?.isInstalled == true)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(packageName) { viewModel.load(packageName) }

    LaunchedEffect(viewModel) {
        viewModel.refusals.collect {
            snackbarHostState.showSnackbar(installRefusalText(context, it))
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = null,
                onNavigateBack = { onNavigateTo(Destination.Back) },
                actions = {
                    IconButton(
                        onClick = {
                            val details = (uiState as? AppDetailsUiState.Loaded)?.details
                            context.shareApp(
                                details?.name ?: packageName,
                                packageName,
                                url = details?.storeUrl ?: details?.url ?: details?.sourceUrl
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = stringResource(R.string.action_share)
                        )
                    }

                    IconButton(onClick = viewModel::toggleFavourite) {
                        Icon(
                            painter = painterResource(
                                if (isFavourite) {
                                    R.drawable.ic_favorite_checked
                                } else {
                                    R.drawable.ic_favorite_unchecked
                                }
                            ),
                            contentDescription = stringResource(
                                if (isFavourite) {
                                    R.string.action_unfavourite
                                } else {
                                    R.string.action_favourite
                                }
                            )
                        )
                    }

                    val resolved = loadedState?.resolved
                    AppExclusionMenu(
                        isBlacklisted = isBlacklisted,
                        isIgnored = ignoredUpdate != null,
                        ignoresEveryVersion = ignoredUpdate?.versionCode == null,
                        updateVersionName = resolved?.takeIf { it.hasUpdate }?.versionName,
                        canIgnoreUpdates = resolved?.isInstalled == true,
                        onToggleBlacklist = viewModel::toggleBlacklist,
                        onIgnoreAllUpdates = viewModel::ignoreAllUpdates,
                        onIgnoreThisVersion = viewModel::ignoreThisVersion,
                        onStopIgnoring = viewModel::stopIgnoringUpdates,
                        onAppInfo = { context.appInfo(actionablePackage) },
                        onAddToHome = canAddToHome.takeIf { it }?.let {
                            { ShortcutUtil.requestPinShortcut(context, actionablePackage) }
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (detailError != null) {
                OfflineBanner(onRetry = viewModel::retry)
            }

            ExpressivePullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::retry,
                modifier = Modifier.weight(1f).fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = uiState,
                    contentKey = { state ->
                        when (state) {
                            is AppDetailsUiState.Loading -> ContentPhase.Loading
                            is AppDetailsUiState.NotFound -> ContentPhase.Empty
                            is AppDetailsUiState.Error -> ContentPhase.Empty
                            is AppDetailsUiState.Loaded -> ContentPhase.Loaded
                        }
                    },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AppDetailsScreenContent"
                ) { state ->
                    when (state) {
                        is AppDetailsUiState.Loading -> LoadingIndicatorBox()

                        is AppDetailsUiState.NotFound -> Placeholder(
                            painter = painterResource(R.drawable.ic_apps),
                            message = stringResource(R.string.details_not_found)
                        )

                        is AppDetailsUiState.Error -> Placeholder(
                            painter = painterResource(R.drawable.ic_apps),
                            message = stringResource(R.string.details_load_failed),
                            actionLabel = stringResource(R.string.action_retry),
                            onAction = viewModel::retry
                        )

                        is AppDetailsUiState.Loaded -> Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        ) {
                            val canOpen = rememberCanOpen(
                                state.actionablePackage,
                                state.resolved?.isInstalled == true
                            )

                            InstallSection(
                                details = state.details,
                                resolved = state.resolved,
                                canOpen = canOpen,
                                downloadFlow = viewModel.download,
                                onAction = { action ->
                                    when (action) {
                                        InstallAction.INSTALL -> coroutineScope.launch {
                                            installOrRequestPermission(context, viewModel)
                                        }

                                        InstallAction.CANCEL -> viewModel.cancel()
                                        InstallAction.OPEN -> launchApp(
                                            context,
                                            state.actionablePackage
                                        )

                                        InstallAction.UNINSTALL ->
                                            context.uninstallPackage(state.actionablePackage)

                                        InstallAction.OPEN_STORE,
                                        InstallAction.OPEN_LINK -> SourceLauncher.launch(
                                            context = context,
                                            availability = state.details.availability,
                                            storeUrl = state.details.storeUrl,
                                            url = state.details.url,
                                            sourceUrl = state.details.sourceUrl
                                        )
                                    }
                                }
                            )

                            CompatibilityNotice(minSdk = state.details.minSdk)

                            DetailsTags(
                                details = state.details,
                                onCategoryClick = categorySlug?.takeIf {
                                    it.isNotBlank()
                                }?.let { slug ->
                                    { onNavigateTo(Destination.AppList(categorySlug = slug)) }
                                }
                            )

                            BillingNotice(
                                hasPaid = state.details.hasPaid,
                                hasIap = state.details.hasIap
                            )

                            ClosedSourceNotice(listing = state.details.listing)

                            SectionHeader(
                                title = stringResource(R.string.details_more_about),
                                subtitle = state.details.description.takeIf { it.isNotBlank() },
                                icon = R.drawable.ic_info_outlined,
                                onClick = { onNavigateTo(Destination.MoreAbout(packageName)) }
                            )

                            if (!state.details.changelog.isNullOrBlank()) {
                                SectionHeader(
                                    title = stringResource(R.string.details_changelog),
                                    subtitle = state.details.versionName
                                        .takeIf { it.isNotBlank() }
                                        ?.let {
                                            stringResource(R.string.details_changelog_subtitle, it)
                                        },
                                    icon = R.drawable.ic_updates,
                                    onClick = {
                                        onNavigateTo(Destination.Changelog(packageName))
                                    }
                                )
                            }

                            if (state.details.screenshots.isNotEmpty()) {
                                ScreenshotGallery(screenshots = state.details.screenshots)
                            }

                            LinkList(details = state.details)

                            if (state.sources.isNotEmpty()) {
                                SectionHeader(
                                    title = stringResource(R.string.details_permissions),
                                    subtitle = if (state.details.permissions.isEmpty()) {
                                        stringResource(R.string.details_permissions_none)
                                    } else {
                                        pluralStringResource(
                                            R.plurals.details_permissions_count,
                                            state.details.permissions.size,
                                            state.details.permissions.size
                                        )
                                    },
                                    icon = R.drawable.ic_shield,
                                    onClick = {
                                        onNavigateTo(Destination.Permissions(packageName))
                                    }
                                )
                            }

                            SourceList(
                                sources = state.sources,
                                onSelect = { viewModel.installFrom(it.app) }
                            )

                            DetailsCarousel(
                                title = stringResource(R.string.details_more_from_author),
                                apps = moreFromAuthor,
                                onAppClick = {
                                    onNavigateTo(Destination.AppDetails(it.packageName))
                                }
                            )

                            DetailsCarousel(
                                title = stringResource(R.string.details_more_from_category),
                                apps = moreFromCategory,
                                modifier = Modifier.padding(bottom = 8.dp),
                                onHeaderClick = categorySlug?.takeIf {
                                    it.isNotBlank()
                                }?.let { slug ->
                                    { onNavigateTo(Destination.AppList(categorySlug = slug)) }
                                },
                                onAppClick = {
                                    onNavigateTo(Destination.AppDetails(it.packageName))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberCanOpen(packageName: String, installed: Boolean): Boolean {
    val context = LocalContext.current
    return remember(packageName, installed) {
        runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }.getOrDefault(false)
    }
}

/**
 * Header and install controls read the download state, so they are isolated from
 * the rest of the page: a progress tick must not recompose screenshots or sources.
 */
@Composable
private fun InstallSection(
    details: AppDetails,
    resolved: ResolvedApp?,
    canOpen: Boolean,
    downloadFlow: StateFlow<Download?>,
    onAction: (InstallAction) -> Unit
) {
    val context = LocalContext.current
    val download by downloadFlow.collectAsStateWithLifecycle()

    val actions = resolved?.let { app ->
        installButtonState(
            context = context,
            app = app,
            download = download,
            canOpen = canOpen
        )
    } ?: linkButtonState(
        context = context,
        availability = details.availability,
        installed = details.installedVersionCode != null,
        canOpen = canOpen
    )

    DetailsHeader(
        details = details,
        inProgress = actions?.bar?.isActive == true,
        progress = actions?.bar?.percent ?: 0F,
        status = actions?.caption,
        statusIsError = actions?.captionIsError == true,
        statusKey = download?.status
    )

    // Play-only apps show the store card above the action row so the install
    // button pair stays the primary action.
    if (details.availability == Availability.PLAY_REDIRECT) {
        StoreNotice(
            onOpen = {
                details.storeUrl?.let { storeUrl -> SourceLauncher.open(context, storeUrl) }
            }
        )
    }

    if (actions != null) {
        InstallActions(state = actions, onAction = onAction)
    }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    runCatching { context.startActivity(intent) }
        .onFailure { Log.w("AppDetailsScreen", "Could not launch $packageName", it) }
}

private suspend fun installOrRequestPermission(context: Context, viewModel: AppDetailsViewModel) {
    val installer = AppInstaller.getCurrentInstaller(context)

    if (requiresUnknownSourcesSettings(installer, canRequestPackageInstalls(context))) {
        openUnknownAppSourcesSettings(context)
    } else {
        viewModel.install()
    }
}

private fun canRequestPackageInstalls(context: Context): Boolean =
    !isOAndAbove || context.packageManager.canRequestPackageInstalls()

@SuppressLint("InlinedApi")
private fun openUnknownAppSourcesSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.fromParts("package", context.packageName, null)
    )
    runCatching { context.startActivity(intent) }
        .onFailure { Log.w("AppDetailsScreen", "Could not open unknown sources settings", it) }
}
