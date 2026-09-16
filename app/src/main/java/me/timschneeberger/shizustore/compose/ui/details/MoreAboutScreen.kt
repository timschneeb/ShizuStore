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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import me.timschneeberger.shizustore.viewmodel.AppDetailsUiState
import me.timschneeberger.shizustore.viewmodel.AppDetailsViewModel

@Composable
fun MoreAboutScreen(
    packageName: String,
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(packageName) { viewModel.load(packageName) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.details_more_about),
                onNavigateBack = { onNavigateTo(Destination.Back) }
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

            is AppDetailsUiState.Loaded -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                )
            ) {
                val body = state.details.fullDescription
                    ?.takeIf { it.isNotBlank() }
                    ?: state.details.description

                if (body.isNotBlank()) {
                    MarkdownDescription(
                        content = body,
                        repoBaseUrl = githubRawBase(
                            state.details.sourceUrl ?: state.details.url
                        ),
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
