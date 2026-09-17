/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.util.CommonUtil
import me.timschneeberger.shizustore.viewmodel.ProxyViewModel

@Composable
fun NetworkPreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: ProxyViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val proxy by viewModel.proxy.collectAsStateWithLifecycle()
    val installReportingEnabled by viewModel.installReportingEnabled.collectAsStateWithLifecycle()
    val showClosedSource by viewModel.showClosedSource.collectAsStateWithLifecycle()
    var showProxyDialog by remember { mutableStateOf(false) }

    if (showProxyDialog) {
        ProxyDialog(
            currentUrl = proxy?.let { CommonUtil.proxyUrl(it) } ?: "",
            onSave = viewModel::save,
            onClear = {
                viewModel.clear()
                showProxyDialog = false
            },
            onDismiss = { showProxyDialog = false }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_network_title),
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
                headline = stringResource(R.string.settings_proxy_title),
                supporting = proxy
                    ?.let { CommonUtil.proxyDisplayText(it) }
                    ?: stringResource(R.string.settings_proxy_summary_none),
                supportingMaxLines = 2,
                onClick = { showProxyDialog = true }
            )

            AuroraListItem(
                headline = stringResource(R.string.settings_install_reporting_title),
                supporting = stringResource(R.string.settings_install_reporting_subtitle),
                supportingMaxLines = 3,
                trailing = {
                    Switch(
                        checked = installReportingEnabled,
                        onCheckedChange = null
                    )
                },
                onClick = { viewModel.setInstallReportingEnabled(!installReportingEnabled) },
                selection = ItemSelection.Switch(installReportingEnabled)
            )

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
        }
    }
}

@Composable
private fun ProxyDialog(
    currentUrl: String,
    onSave: (String) -> Boolean,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(currentUrl) }
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_proxy_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_proxy_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        invalid = false
                    },
                    label = { Text(stringResource(R.string.settings_proxy_hint)) },
                    singleLine = true,
                    isError = invalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.spacing_small))
                )
                if (invalid) {
                    Text(
                        text = stringResource(R.string.settings_proxy_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_small))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (onSave(url)) onDismiss() else invalid = true },
                enabled = url.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (currentUrl.isNotBlank()) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.settings_proxy_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
