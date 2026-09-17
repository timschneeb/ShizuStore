/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's compose/ui/preferences/SettingsScreen.
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.BuildConfig
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.DonationCard
import me.timschneeberger.shizustore.compose.composable.DonationDialog
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onNavigateTo: (Destination) -> Unit = {}) {
    var showDonationDialog by remember { mutableStateOf(false) }

    if (showDonationDialog) {
        DonationDialog(onDismiss = { showDonationDialog = false })
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_settings),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        val resources = LocalResources.current
        val groups = remember(resources) {
            SettingsGroup.entries
                // Server override is a dev tool, so keep it out of release builds.
                .filter { it != SettingsGroup.SERVER || BuildConfig.DEBUG }
                .sortedBy { resources.getString(it.titleRes).lowercase() }
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(groups) { group ->
                AuroraListItem(
                    headline = stringResource(group.titleRes),
                    supporting = stringResource(group.summaryRes),
                    supportingMaxLines = Int.MAX_VALUE,
                    onClick = { onNavigateTo(group.destination) },
                    leading = {
                        Icon(
                            painter = painterResource(group.iconRes),
                            contentDescription = null
                        )
                    }
                )
            }

            item {
                DonationCard(onClick = { showDonationDialog = true })
            }
        }
    }
}

private enum class SettingsGroup(
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    @DrawableRes val iconRes: Int,
    val destination: Destination
) {
    APPEARANCE(
        titleRes = R.string.settings_appearance_title,
        summaryRes = R.string.settings_appearance_summary,
        iconRes = R.drawable.ic_palette,
        destination = Destination.AppearancePreferences
    ),
    UPDATES(
        titleRes = R.string.settings_group_updates_title,
        summaryRes = R.string.settings_group_updates_summary,
        iconRes = R.drawable.ic_updates,
        destination = Destination.UpdatePreferences
    ),
    INSTALLATION(
        titleRes = R.string.settings_installer_title,
        summaryRes = R.string.settings_installer_summary,
        iconRes = R.drawable.ic_installed,
        destination = Destination.InstallationPreferences
    ),
    NETWORK(
        titleRes = R.string.settings_network_title,
        summaryRes = R.string.settings_network_summary,
        iconRes = R.drawable.ic_group_network,
        destination = Destination.NetworkPreferences
    ),
    SERVER(
        titleRes = R.string.settings_server_title,
        summaryRes = R.string.settings_server_summary,
        iconRes = R.drawable.ic_dns_outlined,
        destination = Destination.ServerPreferences
    ),
    PERMISSIONS(
        titleRes = R.string.settings_permissions_title,
        summaryRes = R.string.settings_permissions_summary,
        iconRes = R.drawable.ic_shield,
        destination = Destination.PermissionPreferences
    )
}
