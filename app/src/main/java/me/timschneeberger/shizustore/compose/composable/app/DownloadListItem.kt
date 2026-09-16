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
import androidx.compose.runtime.remember
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

@Composable
internal fun DownloadListItem(
    state: DownloadRowState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Rebuilt only when the row actually changes, not on every list emission.
    val row = remember(state, context) { downloadRow(context, state) }

    AuroraListItem(
        modifier = modifier,
        headline = state.displayName,
        supporting = row.status,
        tertiary = row.detail?.let { AnnotatedString(it) },
        headlineStyle = MaterialTheme.typography.bodyMedium,
        onClick = onClick,
        leading = {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                iconUrl = state.iconUrl,
                inProgress = state.isActive,
                progress = progressPercent(state.progress)
            )
        },
        trailing = when (state.status) {
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
