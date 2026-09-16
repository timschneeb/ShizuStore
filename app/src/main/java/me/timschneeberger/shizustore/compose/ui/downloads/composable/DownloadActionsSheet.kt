/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's DownloadActionsSheet.
 */

package me.timschneeberger.shizustore.compose.ui.downloads.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SheetActionItem
import me.timschneeberger.shizustore.compose.composable.SheetAppHeader
import me.timschneeberger.shizustore.compose.composable.SheetDivider
import me.timschneeberger.shizustore.compose.composable.app.downloadIconUrl
import me.timschneeberger.shizustore.compose.composable.app.progressPercent
import me.timschneeberger.shizustore.compose.composable.app.statusCaption
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.CommonUtil

@Composable
fun DownloadActionsSheet(
    download: Download,
    canInstall: Boolean,
    onShowDetails: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
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
            val context = LocalContext.current

            SheetAppHeader(
                title = download.displayName,
                packageName = download.packageName,
                iconUrl = downloadIconUrl(download),
                lines = listOf(
                    stringResource(
                        R.string.download_version_size,
                        download.versionCode,
                        CommonUtil.addSiPrefix(download.size)
                    ),
                    statusCaption(context, download.status)
                ),
                onAppDetails = {
                    onShowDetails()
                    onDismiss()
                },
                inProgress = download.isActive,
                progress = progressPercent(download.progress)
            )

            SheetDivider()

            if (download.isActive) {
                SheetActionItem(
                    label = stringResource(R.string.action_cancel),
                    onClick = {
                        onCancel()
                        onDismiss()
                    }
                )
            } else {
                SheetActionItem(
                    label = stringResource(R.string.action_clear),
                    onClick = {
                        onClear()
                        onDismiss()
                    }
                )
            }

            if (canInstall) {
                SheetActionItem(
                    label = stringResource(R.string.action_install),
                    onClick = {
                        onInstall()
                        onDismiss()
                    }
                )
                SheetActionItem(
                    label = stringResource(R.string.action_export),
                    onClick = {
                        onExport()
                        onDismiss()
                    }
                )
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
