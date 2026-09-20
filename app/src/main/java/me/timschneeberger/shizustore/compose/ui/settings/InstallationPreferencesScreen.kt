/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.ShimmerHost
import me.timschneeberger.shizustore.compose.composable.ShimmerInstallerRow
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.model.InstallerCatalogue
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.viewmodel.SettingsViewModel

@Composable
fun InstallationPreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val availableInstallers by viewModel.availableInstallers.collectAsStateWithLifecycle()
    val selectedInstaller by viewModel.selectedInstaller.collectAsStateWithLifecycle()
    val customSourceEnabled by viewModel.customInstallerSourceEnabled.collectAsStateWithLifecycle()
    val customSourcePackage by viewModel.customInstallerSourcePackage.collectAsStateWithLifecycle()
    val loading = availableInstallers.isEmpty() || selectedInstaller == null
    val shizukuSelected = selectedInstaller == Installer.SHIZUKU

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
                .imePadding()
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

                SectionHeader(
                    title = stringResource(R.string.settings_installer_custom_source_section),
                    titleColor = MaterialTheme.colorScheme.primary
                )

                AuroraListItem(
                    headline = stringResource(R.string.settings_installer_custom_source_title),
                    supporting = stringResource(R.string.settings_installer_custom_source_subtitle),
                    supportingMaxLines = 3,
                    enabled = shizukuSelected,
                    trailing = {
                        Switch(
                            checked = customSourceEnabled,
                            enabled = shizukuSelected,
                            onCheckedChange = null
                        )
                    },
                    onClick = { viewModel.setCustomInstallerSourceEnabled(!customSourceEnabled) },
                    selection = ItemSelection.Switch(customSourceEnabled)
                )

                if (customSourceEnabled) {
                    InstallerSourcePackageField(
                        currentPackage = customSourcePackage,
                        enabled = shizukuSelected,
                        onSave = viewModel::setCustomInstallerSourcePackage
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallerSourcePackageField(
    currentPackage: String,
    enabled: Boolean,
    onSave: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var packageName by remember(currentPackage) { mutableStateOf(currentPackage) }
    var invalid by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // The IME only shrinks the viewport; without an explicit request the field
    // stays hidden behind the keyboard.
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) bringIntoViewRequester.bringIntoView()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(horizontal = dimensionResource(R.dimen.spacing_large))
    ) {
        OutlinedTextField(
            value = packageName,
            onValueChange = {
                packageName = it
                invalid = false
            },
            label = { Text(stringResource(R.string.settings_installer_custom_source_hint)) },
            singleLine = true,
            enabled = enabled,
            isError = invalid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth()
        )
        if (invalid) {
            Text(
                text = stringResource(R.string.settings_installer_custom_source_invalid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_small))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    if (onSave(packageName)) {
                        invalid = false
                        Toast.makeText(
                            context,
                            R.string.settings_installer_custom_source_saved,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        invalid = true
                    }
                },
                enabled = enabled && packageName.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

private const val GUARANTEED_INSTALLERS = 2
