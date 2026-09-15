/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
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
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.ServerStatus
import me.timschneeberger.shizustore.viewmodel.ServerViewModel

@Composable
fun ServerPreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: ServerViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var showServerDialog by remember { mutableStateOf(false) }

    if (showServerDialog) {
        ServerDialog(
            currentUrl = baseUrl,
            onSave = viewModel::save,
            onClear = {
                viewModel.clear()
                showServerDialog = false
            },
            onDismiss = { showServerDialog = false }
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
            AuroraListItem(
                headline = stringResource(R.string.settings_server_title),
                supporting = baseUrl.ifBlank {
                    stringResource(R.string.settings_server_not_configured)
                },
                tertiary = AnnotatedString(stringResource(statusTextRes(status))),
                supportingMaxLines = 2,
                onClick = { showServerDialog = true }
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
private fun ServerDialog(
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
        title = { Text(stringResource(R.string.settings_server_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_server_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.spacing_small))
                )
                if (invalid) {
                    Text(
                        text = stringResource(R.string.settings_server_invalid),
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
                        Text(stringResource(R.string.settings_server_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
