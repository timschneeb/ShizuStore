/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's AppUpdateSheet.
 */

package me.timschneeberger.shizustore.compose.ui.updates.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SheetActionItem
import me.timschneeberger.shizustore.compose.composable.SheetAppHeader
import me.timschneeberger.shizustore.compose.composable.SheetDivider
import me.timschneeberger.shizustore.compose.indexUrl
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.util.CommonUtil

@Composable
fun AppUpdateSheet(
    app: ResolvedApp,
    isBlacklisted: Boolean,
    onAppDetails: () -> Unit,
    onIgnoreAllUpdates: () -> Unit,
    onIgnoreThisVersion: () -> Unit,
    onToggleBlacklist: () -> Unit,
    onUninstall: () -> Unit,
    onAppInfo: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SheetAppHeader(
                title = app.name.ifBlank { app.packageName },
                packageName = app.packageName,
                iconUrl = indexUrl(app.repoAddress, app.iconUrl).orEmpty(),
                lines = buildList {
                    add(stringResource(R.string.updates_version, app.versionName, app.versionCode))
                    if (app.releasedAt > 0L) add(CommonUtil.formatDate(app.releasedAt))
                },
                onAppDetails = {
                    onAppDetails()
                    onDismiss()
                }
            )

            SheetDivider()

            SheetActionItem(
                label = stringResource(R.string.action_ignore_all),
                onClick = {
                    onIgnoreAllUpdates()
                    onDismiss()
                }
            )
            SheetActionItem(
                label = stringResource(R.string.action_ignore_version, app.versionName),
                onClick = {
                    onIgnoreThisVersion()
                    onDismiss()
                }
            )

            SheetDivider()

            SheetActionItem(
                label = if (isBlacklisted) {
                    stringResource(R.string.action_unblacklist)
                } else {
                    stringResource(R.string.action_blacklist)
                },
                onClick = {
                    onToggleBlacklist()
                    onDismiss()
                }
            )
            SheetActionItem(
                label = stringResource(R.string.action_app_info),
                onClick = {
                    onAppInfo()
                    onDismiss()
                }
            )
            SheetActionItem(
                label = stringResource(R.string.action_uninstall),
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    onUninstall()
                    onDismiss()
                }
            )

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
