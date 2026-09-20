/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.compose.stringRes
import me.timschneeberger.shizustore.data.work.UpdateWorker
import me.timschneeberger.shizustore.viewmodel.SettingsViewModel

@Composable
fun UpdatePreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val updateCheckIntervalHours by viewModel.updateCheckIntervalHours
        .collectAsStateWithLifecycle()
    val syncOnWifiOnly by viewModel.syncOnWifiOnly.collectAsStateWithLifecycle()
    val unattendedUpdatesEnabled by viewModel.unattendedUpdatesEnabled.collectAsStateWithLifecycle()
    val unattendedPausedReason by viewModel.unattendedPausedReason.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshUnattendedState()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_group_updates_title),
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
                title = stringResource(R.string.settings_updates_title),
                subtitle = stringResource(R.string.settings_updates_subtitle),
                titleColor = MaterialTheme.colorScheme.primary
            )

            UpdateIntervalOption.entries.forEach { option ->
                AuroraListItem(
                    headline = stringResource(option.labelRes),
                    leading = {
                        RadioButton(
                            selected = updateCheckIntervalHours == option.hours,
                            onClick = null
                        )
                    },
                    onClick = { viewModel.setUpdateCheckInterval(option.hours) },
                    selection = ItemSelection.Radio(updateCheckIntervalHours == option.hours)
                )
            }

            AuroraListItem(
                headline = stringResource(R.string.settings_wifi_only_title),
                supporting = stringResource(R.string.settings_wifi_only_subtitle),
                supportingMaxLines = 2,
                enabled = updateCheckIntervalHours != UpdateWorker.INTERVAL_NEVER,
                trailing = {
                    Switch(
                        checked = syncOnWifiOnly,
                        enabled = updateCheckIntervalHours != UpdateWorker.INTERVAL_NEVER,
                        onCheckedChange = null
                    )
                },
                onClick = { viewModel.setSyncOnWifiOnly(!syncOnWifiOnly) },
                selection = ItemSelection.Switch(syncOnWifiOnly)
            )

            SectionHeader(
                title = stringResource(R.string.settings_unattended_section_title),
                titleColor = MaterialTheme.colorScheme.primary
            )

            AuroraListItem(
                headline = stringResource(R.string.settings_unattended_title),
                supporting = stringResource(R.string.settings_unattended_subtitle),
                supportingMaxLines = 2,
                tertiary = unattendedPausedReason?.let { stringResource(it.stringRes()) }
                    ?.let { AnnotatedString(it) },
                trailing = {
                    Switch(
                        checked = unattendedUpdatesEnabled,
                        onCheckedChange = null
                    )
                },
                onClick = { viewModel.setUnattendedUpdatesEnabled(!unattendedUpdatesEnabled) },
                selection = ItemSelection.Switch(unattendedUpdatesEnabled)
            )
        }
    }
}

private enum class UpdateIntervalOption(val hours: Int, @StringRes val labelRes: Int) {
    NEVER(UpdateWorker.INTERVAL_NEVER, R.string.settings_update_interval_never),
    SIX_HOURS(UpdateWorker.DEFAULT_INTERVAL_HOURS, R.string.settings_update_interval_six_hours),
    TWELVE_HOURS(12, R.string.settings_update_interval_twelve_hours),
    DAILY(24, R.string.settings_update_interval_daily),
    WEEKLY(24 * 7, R.string.settings_update_interval_weekly)
}
