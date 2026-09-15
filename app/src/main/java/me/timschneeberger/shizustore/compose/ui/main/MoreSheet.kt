/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SheetActionItem
import me.timschneeberger.shizustore.compose.navigation.Destination

@Composable
fun MoreSheet(onNavigateTo: (Destination) -> Unit, onDismiss: () -> Unit) {
    fun navigateAndDismiss(destination: Destination) {
        onNavigateTo(destination)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        MoreSheetItem(R.drawable.ic_widgets_outlined, R.string.title_my_apps) {
            navigateAndDismiss(Destination.Installed)
        }

        HorizontalDivider()

        MoreSheetItem(R.drawable.ic_favorite_border_outlined, R.string.title_favourites) {
            navigateAndDismiss(Destination.Favourites)
        }
        MoreSheetItem(R.drawable.ic_block_outlined, R.string.title_blacklist) {
            navigateAndDismiss(Destination.Blacklist)
        }
        MoreSheetItem(R.drawable.ic_update_disabled_outlined, R.string.title_ignored_updates) {
            navigateAndDismiss(Destination.IgnoredUpdates)
        }

        HorizontalDivider()

        MoreSheetItem(R.drawable.ic_settings_outlined, R.string.title_settings) {
            navigateAndDismiss(Destination.Settings)
        }
        MoreSheetItem(R.drawable.ic_info_outlined, R.string.title_about) {
            navigateAndDismiss(Destination.About)
        }

        Spacer(
            Modifier
                .testTag("more_sheet_nav_bar_spacer")
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun MoreSheetItem(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    SheetActionItem(label = stringResource(label), icon = icon, onClick = onClick)
}
