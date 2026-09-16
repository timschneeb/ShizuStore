/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.util.CommonUtil

@Composable
fun AppTile(app: ResolvedApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val badgeIcon = when {
        app.hasUpdate -> Icons.Rounded.Update
        app.isInstalled -> Icons.Rounded.CheckCircle
        else -> null
    }

    Column(
        modifier = modifier
            .width(dimensionResource(R.dimen.icon_size_cluster))
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.spacing_xsmall)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.spacing_xsmall),
            Alignment.CenterVertically
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            AsyncImage(
                model = rememberAppIconModel(app.iconUrl),
                contentDescription = null,
                contentScale = Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(appIconShape(app.iconAdaptive))
            )
            if (badgeIcon != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(dimensionResource(R.dimen.spacing_xsmall))
                ) {
                    Box(
                        modifier = Modifier.size(dimensionResource(R.dimen.app_badge_size)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = stringResource(
                                if (app.hasUpdate) {
                                    R.string.app_indicator_update
                                } else {
                                    R.string.app_indicator_installed
                                }
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(dimensionResource(R.dimen.app_badge_icon))
                        )
                    }
                }
            }
        }
        Text(
            text = app.name.ifBlank { app.packageName },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        val sizeLabel = CommonUtil.sizeLabel(app.size)
        if (sizeLabel != null) {
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
