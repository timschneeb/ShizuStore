/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's DownloadActionsSheet.
 */

package me.timschneeberger.shizustore.compose.ui.downloads.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SheetActionItem
import me.timschneeberger.shizustore.compose.composable.app.AnimatedAppIcon
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
            DownloadHeader(
                download = download,
                onShowDetails = {
                    onShowDetails()
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
            )

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

@Composable
private fun DownloadHeader(download: Download, onShowDetails: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
    ) {
        AnimatedAppIcon(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
            iconUrl = downloadIconUrl(download),
            inProgress = download.isActive,
            progress = progressPercent(download.progress)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = download.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.download_version_size,
                    download.versionCode,
                    CommonUtil.addSiPrefix(download.size)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusCaption(context, download.status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FilledTonalButton(onClick = onShowDetails) {
            Text(stringResource(R.string.updates_app_details))
        }
    }
}
