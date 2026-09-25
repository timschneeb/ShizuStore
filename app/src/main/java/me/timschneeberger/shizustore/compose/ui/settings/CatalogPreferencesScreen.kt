/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.CatalogPreferencesViewModel

@Composable
fun CatalogPreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: CatalogPreferencesViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val showClosedSource by viewModel.showClosedSource.collectAsStateWithLifecycle()
    val showTrackerInfo by viewModel.showTrackerInfo.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_catalog_title),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AuroraListItem(
                headline = stringResource(R.string.settings_show_closed_source_title),
                supporting = stringResource(R.string.settings_show_closed_source_subtitle),
                supportingMaxLines = 3,
                trailing = {
                    Switch(
                        checked = showClosedSource,
                        onCheckedChange = null
                    )
                },
                onClick = { viewModel.setShowClosedSource(!showClosedSource) },
                selection = ItemSelection.Switch(showClosedSource)
            )

            AuroraListItem(
                headline = stringResource(R.string.settings_show_tracker_info_title),
                supporting = stringResource(R.string.settings_show_tracker_info_subtitle),
                supportingMaxLines = 3,
                trailing = {
                    Switch(
                        checked = showTrackerInfo,
                        onCheckedChange = null
                    )
                },
                onClick = { viewModel.setShowTrackerInfo(!showTrackerInfo) },
                selection = ItemSelection.Switch(showTrackerInfo)
            )
        }
    }
}
