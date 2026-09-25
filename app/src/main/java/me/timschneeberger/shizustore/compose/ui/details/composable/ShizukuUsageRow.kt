// SPDX-FileCopyrightText: 2026 Tim Schneeberger <spam@timschneeberger.me>
// SPDX-License-Identifier: GPL-3.0-or-later

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.data.model.AppDetails

/**
 * Shizuku usage as a plain section row, matching the rows below it. Tapping
 * opens the evidence behind the classification. Deliberately not a card: the
 * classification is secondary to the app description.
 */
@Composable
fun ShizukuUsageRow(details: AppDetails, modifier: Modifier = Modifier) {
    if (details.managers.isEmpty() && details.usageSummary.isNullOrBlank()) {
        return
    }

    var showDialog by rememberSaveable { mutableStateOf(false) }
    val subtitle = details.usageSummary?.takeIf { it.isNotBlank() }
        ?: details.managers.joinToString(", ") { managerName(it) }

    SectionHeader(
        modifier = modifier,
        title = stringResource(R.string.details_shizuku_title),
        subtitle = subtitle,
        icon = R.drawable.ic_shizuku_icon,
        onClick = if (details.signals.isNotEmpty()) {
            { showDialog = true }
        } else {
            null
        }
    )

    if (showDialog) {
        ShizukuUsageDialog(details, onDismiss = { showDialog = false })
    }
}

@Composable
private fun ShizukuUsageDialog(details: AppDetails, onDismiss: () -> Unit) {
    val managerText = details.managers.joinToString(", ") { managerName(it) }
    val apiFormText = details.apiForm?.let { apiFormLabel(it) }
    val capabilityText = details.capabilities.map { capabilityLabel(it) }.joinToString(", ")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_shizuku_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                ),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (managerText.isNotEmpty()) {
                    Text(
                        stringResource(R.string.details_shizuku_managers, managerText),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                apiFormText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                if (capabilityText.isNotEmpty()) {
                    Text(
                        stringResource(R.string.details_shizuku_capabilities, capabilityText),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (details.usageOptional) {
                    Text(
                        stringResource(R.string.details_shizuku_optional),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                details.signals.forEach { signal ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.spacing_medium),
                                vertical = dimensionResource(R.dimen.spacing_small)
                            )
                        ) {
                            Text(signal.value, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${signal.kind} · ${signal.confidence}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.details_shizuku_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

private fun managerName(id: String): String = when (id) {
    "shizuku" -> "Shizuku"
    "dhizuku" -> "Dhizuku"
    "sui" -> "Sui"
    else -> id
}

@Composable
private fun apiFormLabel(form: String): String = when (form) {
    "user_service" -> stringResource(R.string.details_shizuku_api_user_service)
    "new_process" -> stringResource(R.string.details_shizuku_api_new_process)
    "permission" -> stringResource(R.string.details_shizuku_api_permission)
    else -> form
}

@Composable
private fun capabilityLabel(capability: String): String = when (capability) {
    "install" -> stringResource(R.string.details_shizuku_capability_install)
    "uninstall" -> stringResource(R.string.details_shizuku_capability_uninstall)
    "freeze" -> stringResource(R.string.details_shizuku_capability_freeze)
    "appops" -> stringResource(R.string.details_shizuku_capability_appops)
    "system_settings" -> stringResource(R.string.details_shizuku_capability_system_settings)
    "process" -> stringResource(R.string.details_shizuku_capability_process)
    "diagnostics" -> stringResource(R.string.details_shizuku_capability_diagnostics)
    "reboot" -> stringResource(R.string.details_shizuku_capability_reboot)
    "wireless_adb" -> stringResource(R.string.details_shizuku_capability_wireless_adb)
    "compile" -> stringResource(R.string.details_shizuku_capability_compile)
    else -> capability
}
