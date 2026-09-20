/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.util.ServerConfig
import me.timschneeberger.shizustore.viewmodel.ServerStatus
import me.timschneeberger.shizustore.viewmodel.ServerViewModel

@Composable
fun ServerPreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: ServerViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val useCustom by viewModel.useCustom.collectAsStateWithLifecycle()
    val customUrl by viewModel.customUrl.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_server_reset)) },
            text = { Text(stringResource(R.string.settings_server_reset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLocalDatabase()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_server_title),
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
            SectionHeader(
                title = stringResource(R.string.settings_server_title),
                titleColor = MaterialTheme.colorScheme.primary
            )

            AuroraListItem(
                headline = stringResource(R.string.settings_server_production),
                supporting = ServerConfig.productionBaseUrl,
                leading = {
                    RadioButton(selected = !useCustom, onClick = null)
                },
                onClick = { viewModel.setUseCustom(false) },
                selection = ItemSelection.Radio(!useCustom)
            )

            AuroraListItem(
                headline = stringResource(R.string.settings_server_custom),
                supporting = customUrl.takeIf { it.isNotBlank() },
                leading = {
                    RadioButton(selected = useCustom, onClick = null)
                },
                onClick = { viewModel.setUseCustom(true) },
                selection = ItemSelection.Radio(useCustom)
            )

            if (useCustom) {
                CustomServerField(
                    currentUrl = customUrl,
                    onSave = viewModel::saveCustomUrl
                )
            }

            AuroraListItem(
                headline = stringResource(R.string.settings_server_active),
                supporting = baseUrl.ifBlank {
                    stringResource(R.string.settings_server_not_configured)
                },
                tertiary = AnnotatedString(stringResource(statusTextRes(status))),
                supportingMaxLines = 2
            )

            SectionHeader(
                title = stringResource(R.string.settings_server_maintenance),
                titleColor = MaterialTheme.colorScheme.primary
            )

            AuroraListItem(
                headline = stringResource(R.string.settings_server_reset),
                supporting = stringResource(R.string.settings_server_reset_summary),
                supportingMaxLines = 3,
                onClick = { showResetDialog = true }
            )
        }
    }
}

private fun statusTextRes(status: ServerStatus): Int = when (status) {
    ServerStatus.UNKNOWN -> R.string.settings_server_status_unknown
    ServerStatus.CHECKING -> R.string.settings_server_status_checking
    ServerStatus.REACHABLE -> R.string.settings_server_status_reachable
    ServerStatus.UNREACHABLE -> R.string.settings_server_status_unreachable
}

@Composable
private fun CustomServerField(
    currentUrl: String,
    onSave: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }
    var invalid by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_large))
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                invalid = false
            },
            label = { Text(stringResource(R.string.settings_server_hint)) },
            singleLine = true,
            isError = invalid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        if (invalid) {
            Text(
                text = stringResource(R.string.settings_server_invalid),
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
                onClick = { if (onSave(url)) invalid = false else invalid = true },
                enabled = url.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
