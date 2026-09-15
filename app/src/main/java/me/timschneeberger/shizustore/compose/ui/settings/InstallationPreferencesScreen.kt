/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.compose.composable.ShimmerHost
import me.timschneeberger.shizustore.compose.composable.ShimmerInstallerRow
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.model.InstallerCatalogue
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.SettingsViewModel

@Composable
fun InstallationPreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val availableInstallers by viewModel.availableInstallers.collectAsStateWithLifecycle()
    val selectedInstaller by viewModel.selectedInstaller.collectAsStateWithLifecycle()
    val loading = availableInstallers.isEmpty() || selectedInstaller == null

    val loadingDescription = stringResource(R.string.loading)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_installer_title),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .then(
                    if (loading) {
                        Modifier.semantics { stateDescription = loadingDescription }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (loading) {
                ShimmerHost {
                    repeat(GUARANTEED_INSTALLERS) { ShimmerInstallerRow() }
                }
            } else {
                availableInstallers.forEach { installer ->
                    val info = InstallerCatalogue.infoFor(installer)
                    AuroraListItem(
                        headline = stringResource(info.title),
                        supporting = stringResource(info.subtitle),
                        tertiary = AnnotatedString(stringResource(info.description)),
                        tertiaryMaxLines = Int.MAX_VALUE,
                        leading = {
                            RadioButton(selected = installer == selectedInstaller, onClick = null)
                        },
                        onClick = { viewModel.selectInstaller(installer) },
                        selection = ItemSelection.Radio(installer == selectedInstaller)
                    )
                }
            }
        }
    }
}

private const val GUARANTEED_INSTALLERS = 2
