/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.LabelChip
import me.timschneeberger.shizustore.compose.indexUrl
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.util.CommonUtil

@Composable
fun AppListItem(
    app: ResolvedApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    showStars: Boolean = false,
    supporting: String? = app.authorName?.takeIf { it.isNotBlank() } ?: app.summary
) {
    val starLabel = if (showStars) {
        app.stars?.let { stringResource(R.string.app_stars, CommonUtil.formatCount(it.toLong())) }
    } else {
        null
    }
    val versionText = stringResource(
        R.string.app_version_size,
        app.versionName,
        CommonUtil.addSiPrefix(app.size)
    )
    val tertiaryText = if (starLabel != null) "$starLabel · $versionText" else versionText

    AuroraListItem(
        modifier = modifier,
        headline = app.name.ifBlank { app.packageName },
        supporting = supporting?.takeIf { it.isNotBlank() },
        tertiary = tertiaryText,
        headlineStyle = MaterialTheme.typography.bodyMedium,
        onClick = onClick,
        trailing = trailing,
        badges = if (app.hasPaid || app.hasIap) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_xsmall)
                    )
                ) {
                    if (app.hasIap) {
                        LabelChip(
                            text = stringResource(R.string.app_badge_iap),
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (app.hasPaid) {
                        LabelChip(
                            text = stringResource(R.string.app_badge_paid),
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        } else {
            null
        },
        leading = leading ?: {
            AsyncImage(
                model = rememberAppIconModel(indexUrl(app.repoAddress, app.iconUrl)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                    .clip(appIconShape(app.iconAdaptive))
            )
        }
    )
}
