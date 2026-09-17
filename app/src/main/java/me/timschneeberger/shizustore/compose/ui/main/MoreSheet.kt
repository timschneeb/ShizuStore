/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.DonationDialog
import me.timschneeberger.shizustore.compose.composable.SheetActionItem
import me.timschneeberger.shizustore.compose.navigation.Destination

@Composable
fun MoreSheet(onNavigateTo: (Destination) -> Unit, onDismiss: () -> Unit) {
    fun navigateAndDismiss(destination: Destination) {
        onNavigateTo(destination)
        onDismiss()
    }

    var showDonationDialog by remember { mutableStateOf(false) }

    if (showDonationDialog) {
        DonationDialog(onDismiss = { showDonationDialog = false })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        SheetActionItem(
            label = stringResource(R.string.title_my_apps),
            onClick = { navigateAndDismiss(Destination.Installed) },
            icon = R.drawable.ic_widgets_outlined
        )

        HorizontalDivider()

        SheetActionItem(
            label = stringResource(R.string.title_favourites),
            onClick = { navigateAndDismiss(Destination.Favourites) },
            icon = R.drawable.ic_favorite_border_outlined
        )
        SheetActionItem(
            label = stringResource(R.string.title_blacklist),
            onClick = { navigateAndDismiss(Destination.Blacklist) },
            icon = R.drawable.ic_block_outlined
        )
        SheetActionItem(
            label = stringResource(R.string.title_ignored_updates),
            onClick = { navigateAndDismiss(Destination.IgnoredUpdates) },
            icon = R.drawable.ic_update_disabled_outlined
        )

        HorizontalDivider()

        SheetActionItem(
            label = stringResource(R.string.title_settings),
            onClick = { navigateAndDismiss(Destination.Settings) },
            icon = R.drawable.ic_settings_outlined
        )
        SheetActionItem(
            label = stringResource(R.string.donation_card_title),
            onClick = { showDonationDialog = true },
            icon = R.drawable.ic_favorite_checked
        )
        SheetActionItem(
            label = stringResource(R.string.title_about),
            onClick = { navigateAndDismiss(Destination.About) },
            icon = R.drawable.ic_info_outlined
        )

        Spacer(
            Modifier
                .testTag("more_sheet_nav_bar_spacer")
                .navigationBarsPadding()
        )
    }
}
