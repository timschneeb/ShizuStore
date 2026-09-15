/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R

/** Shown when the last catalog pass failed, so cached data is clearly stale. */
@Composable
fun OfflineBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.spacing_medium)
            ),
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.spacing_large),
                top = dimensionResource(R.dimen.spacing_xsmall),
                end = dimensionResource(R.dimen.spacing_medium),
                bottom = dimensionResource(R.dimen.spacing_xsmall)
            )
        ) {
            Icon(imageVector = Icons.Rounded.CloudOff, contentDescription = null)
            Text(
                text = stringResource(R.string.apps_sync_failed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            // Plain image button: just the glyph, tinted by the surface content
            // color. No elevation, border, or label.
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.action_retry)
                )
            }
        }
    }
}
