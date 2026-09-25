/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.app.AnimatedAppIcon
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.extensions.viewExternal

@Composable
fun DetailsHeader(
    details: AppDetails,
    modifier: Modifier = Modifier,
    inProgress: Boolean = false,
    progress: Float = 0F,
    status: String? = null,
    statusIsError: Boolean = false,
    statusKey: Any? = null
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.spacing_large),
                top = dimensionResource(R.dimen.spacing_medium),
                end = dimensionResource(R.dimen.spacing_large)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedAppIcon(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_large)),
            iconUrl = details.iconUrl.orEmpty(),
            inProgress = inProgress,
            progress = progress
        )
        Column(
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            Text(
                text = details.name.ifBlank { details.packageName },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            details.authorName?.takeIf { it.isNotBlank() }?.let { author ->
                val authorUrl = details.authorUrl?.takeIf { it.isNotBlank() }
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (authorUrl != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Unspecified
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (authorUrl != null) {
                        Modifier.clickable { context.viewExternal(authorUrl) }
                    } else {
                        Modifier
                    }
                )
            }
            AnimatedContent(
                targetState = statusKey,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "DetailsHeaderStatus"
            ) { key ->
                val versionLine = listOfNotNull(
                    details.versionName.takeIf { it.isNotBlank() },
                    details.repoName.takeIf { it.isNotBlank() }
                ).joinToString(" · ")

                val line = when (key) {
                    null -> versionLine
                    else -> status ?: versionLine
                }

                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusIsError && key != null && status != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.Unspecified
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
