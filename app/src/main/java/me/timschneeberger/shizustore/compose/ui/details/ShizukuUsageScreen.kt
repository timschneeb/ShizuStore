/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.LoadingIndicatorBox
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.AppDetailsUiState
import me.timschneeberger.shizustore.viewmodel.AppDetailsViewModel

/**
 * Full AI usage report for one app. The markdown is generated from the app's
 * public source (server SPEC 5.4) and every claim carries a file citation.
 */
@Composable
fun ShizukuUsageScreen(
    packageName: String,
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fetching by viewModel.detailFetching.collectAsStateWithLifecycle()

    LaunchedEffect(packageName) { viewModel.load(packageName) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.details_shizuku_title),
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

            is AppDetailsUiState.Loaded -> {
                // The server composes the report; the AI disclaimer is appended
                // here so it stays localized with the app.
                val note = stringResource(R.string.details_shizuku_ai_note)
                val body = state.details.usageMarkdown?.takeIf { it.isNotBlank() }
                    ?.let { "$it\n\n> [!NOTE]\n> $note" }
                when {
                    // Keep the markdown hidden until the report is fetched, so a
                    // stale or empty document never flashes before the spinner.
                    body.isNullOrBlank() && fetching -> LoadingIndicatorBox(
                        modifier = Modifier.padding(padding)
                    )

                    body.isNullOrBlank() -> Placeholder(
                        modifier = Modifier.padding(padding),
                        painter = painterResource(R.drawable.ic_apps),
                        message = stringResource(R.string.details_shizuku_empty)
                    )

                    else -> Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(
                            dimensionResource(R.dimen.spacing_medium)
                        )
                    ) {
                        state.details.usageShort?.takeIf { it.isNotBlank() }?.let { short ->
                            UsageSummaryCard(text = short)
                        }
                        MarkdownDescription(
                            content = body,
                            repoBaseUrl = githubRawBase(
                                state.details.sourceUrl ?: state.details.url
                            ),
                            accentColor = MaterialTheme.colorScheme.primary,
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

/** One-line report summary as a titled container card above the markdown. */
@Composable
private fun UsageSummaryCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.spacing_large),
                end = dimensionResource(R.dimen.spacing_large),
                top = dimensionResource(R.dimen.spacing_medium)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_medium))) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shizuku_icon),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_default))
                )
                Text(
                    text = stringResource(R.string.details_shizuku_summary_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacing_small)))
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
