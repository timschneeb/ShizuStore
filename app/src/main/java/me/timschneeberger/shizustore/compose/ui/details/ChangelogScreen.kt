/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.LoadingIndicatorBox
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.data.api.SourceKind
import me.timschneeberger.shizustore.data.helper.SourceLauncher
import me.timschneeberger.shizustore.viewmodel.AppDetailsUiState
import me.timschneeberger.shizustore.viewmodel.AppDetailsViewModel

/**
 * F-Droid and IzzyOnDroid publish release notes as sanitized HTML, unlike the
 * markdown release bodies of the code forges.
 */
internal fun changelogRendersAsHtml(sourceKind: SourceKind?): Boolean =
    sourceKind == SourceKind.FDROID || sourceKind == SourceKind.IZZY

@Composable
fun ChangelogScreen(
    packageName: String,
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(packageName) { viewModel.load(packageName) }

    val context = LocalContext.current
    val changelogUrl = (uiState as? AppDetailsUiState.Loaded)?.details?.changelogBrowserUrl

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.details_changelog),
                onNavigateBack = { onNavigateTo(Destination.Back) },
                actions = {
                    if (changelogUrl != null) {
                        IconButton(onClick = { SourceLauncher.open(context, changelogUrl) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_open_in_new),
                                contentDescription = stringResource(
                                    R.string.action_open_in_browser
                                )
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AppDetailsUiState.Loading -> LoadingIndicatorBox(
                modifier = Modifier.padding(padding)
            )

            is AppDetailsUiState.NotFound -> Placeholder(
                modifier = Modifier.padding(padding),
                painter = painterResource(R.drawable.ic_apps),
                message = stringResource(R.string.details_not_found)
            )

            is AppDetailsUiState.Error -> Placeholder(
                modifier = Modifier.padding(padding),
                painter = painterResource(R.drawable.ic_apps),
                message = stringResource(R.string.details_load_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = viewModel::retry
            )

            is AppDetailsUiState.Loaded -> {
                val body = state.details.changelog?.takeIf { it.isNotBlank() }
                if (body.isNullOrBlank()) {
                    Placeholder(
                        modifier = Modifier.padding(padding),
                        painter = painterResource(R.drawable.ic_apps),
                        message = stringResource(R.string.details_changelog_empty)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.spacing_medium)
                        )
                    ) {
                        MarkdownDescription(
                            content = body,
                            repoBaseUrl = githubRawBase(
                                state.details.sourceUrl ?: state.details.url
                            ),
                            assumeHtml = changelogRendersAsHtml(state.details.sourceKind),
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.spacing_large),
                                vertical = dimensionResource(R.dimen.spacing_small)
                            )
                        )
                    }
                }
            }
        }
    }
}
