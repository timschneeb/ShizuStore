/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import android.text.format.DateUtils
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.indexUrl
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.CommonUtil

@Composable
fun AppUpdateItem(
    app: ResolvedApp,
    modifier: Modifier = Modifier,
    download: Download? = null,
    onClick: () -> Unit = {},
    onUpdate: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val inProgress = download != null && !download.isFinished
    val installing = download?.status == DownloadStatus.INSTALLING

    val releasedAt = remember(app.releasedAt) {
        if (app.releasedAt <= 0L) {
            null
        } else {
            DateUtils.getRelativeTimeSpanString(
                app.releasedAt,
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS
            ).toString()
        }
    }

    val versionChange = remember(app.installedVersionName, app.versionName) {
        app.installedVersionName
            ?.takeIf { it.isNotBlank() && it != app.versionName }
            ?.let { "$it  →  ${app.versionName}" }
            ?: app.versionName
    }

    val sizeLabel = CommonUtil.sizeLabel(app.size)
    val tertiaryText = if (sizeLabel != null) "$sizeLabel  •  $versionChange" else versionChange

    AuroraListItem(
        modifier = modifier,
        headline = app.name.ifBlank { app.packageName },
        supporting = releasedAt,
        tertiary = AnnotatedString(tertiaryText),
        headlineStyle = MaterialTheme.typography.bodyMedium,
        minHeight = R.dimen.list_item_height_three_line,
        onClick = onClick,
        leading = {
            AnimatedAppIcon(
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium)),
                iconUrl = indexUrl(app.repoAddress, app.iconUrl).orEmpty(),
                inProgress = inProgress,
                progress = if (download?.status == DownloadStatus.DOWNLOADING) {
                    download.progress.toFloat()
                } else {
                    0F
                }
            )
        },
        trailing = {
            when {
                installing -> OutlinedButton(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.action_installing))
                }

                inProgress -> OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }

                else -> Button(onClick = onUpdate) {
                    Text(stringResource(R.string.action_update))
                }
            }
        }
    )
}
