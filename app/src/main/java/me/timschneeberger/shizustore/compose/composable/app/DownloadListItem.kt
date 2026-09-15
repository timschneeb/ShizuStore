/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's DownloadListItem.
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.theme.successColor
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.entity.Download

@Composable
fun DownloadListItem(download: Download, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val row = downloadRow(LocalContext.current, download)

    AuroraListItem(
        modifier = modifier,
        headline = download.displayName,
        supporting = row.status,
        tertiary = row.detail?.let { AnnotatedString(it) },
        headlineStyle = MaterialTheme.typography.bodyMedium,
        onClick = onClick,
        leading = {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                iconUrl = downloadIconUrl(download),
                inProgress = download.isActive,
                progress = progressPercent(download.progress)
            )
        },
        trailing = when (download.status) {
            DownloadStatus.COMPLETED -> {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = stringResource(R.string.download_status_completed),
                        tint = successColor
                    )
                }
            }

            DownloadStatus.INSTALLED -> {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_installed),
                        contentDescription = stringResource(R.string.download_status_installed),
                        tint = successColor
                    )
                }
            }

            DownloadStatus.CANCELLED, DownloadStatus.FAILED, DownloadStatus.UNAVAILABLE -> {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancel),
                        contentDescription = stringResource(R.string.download_status_failed),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> null
        }
    )
}
