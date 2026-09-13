/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's AppUpdateSheet.
 */

package me.timschneeberger.shizustore.compose.ui.updates.composable

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SheetActionItem
import me.timschneeberger.shizustore.compose.composable.app.AnimatedAppIcon
import me.timschneeberger.shizustore.compose.indexUrl
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.util.CommonUtil

@Composable
fun AppUpdateSheet(
    app: ResolvedApp,
    isBlacklisted: Boolean,
    whatsNew: String?,
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AppHeader(
                app = app,
                onAppDetails = {
                    onAppDetails()
                    onDismiss()
                }
            )

            if (!whatsNew.isNullOrBlank()) {
                ChangelogSection(html = whatsNew)
            }

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

@Composable
private fun AppHeader(app: ResolvedApp, onAppDetails: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        AnimatedAppIcon(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
            iconUrl = indexUrl(app.repoAddress, app.iconUrl).orEmpty()
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name.ifBlank { app.packageName },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.updates_version,
                    app.versionName,
                    app.versionCode
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (app.releasedAt > 0L) {
                Text(
                    text = CommonUtil.formatDate(app.releasedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FilledTonalButton(onClick = onAppDetails) {
            Text(stringResource(R.string.updates_app_details))
        }
    }
}

@Composable
private fun ChangelogSection(html: String) {
    Text(
        text = stringResource(R.string.details_changelog),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_xsmall)
        )
    )
    Text(
        text = AnnotatedString.fromHtml(html),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = dimensionResource(R.dimen.spacing_xsmall)
        )
    )
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
    )
}
