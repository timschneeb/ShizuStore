/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.ContainedLoadingIndicator
import me.timschneeberger.shizustore.compose.composable.Info
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.AppDetailsUiState
import me.timschneeberger.shizustore.viewmodel.AppDetailsViewModel

@Composable
fun PermissionsScreen(
    packageName: String,
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val packageManager = LocalContext.current.packageManager

    LaunchedEffect(packageName) { viewModel.load(packageName) }

    val declared = (uiState as? AppDetailsUiState.Loaded)?.details?.permissions

    val groups = remember(declared) {
        declared
            ?.map { resolvePermission(packageManager, it) }
            ?.sortedBy { it.label.lowercase() }
            ?.groupBy { it.isKnown }
            ?.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.details_permissions),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        when {
            groups == null -> ContainedLoadingIndicator(
                modifier = Modifier.padding(padding)
            )

            groups.isEmpty() -> Placeholder(
                modifier = Modifier.padding(padding),
                painter = painterResource(R.drawable.ic_shield),
                message = stringResource(R.string.details_permissions_none)
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_xsmall)
                )
            ) {
                groups.forEach { (isKnown, entries) ->
                    item(key = "header-$isKnown") {
                        SectionHeader(
                            title = stringResource(
                                if (isKnown) {
                                    R.string.details_permissions_known
                                } else {
                                    R.string.details_permissions_custom
                                }
                            )
                        )
                    }
                    items(items = entries, key = { it.description ?: it.label }) { entry ->
                        Info(title = entry.label, description = entry.description)
                    }
                }
            }
        }
    }
}
