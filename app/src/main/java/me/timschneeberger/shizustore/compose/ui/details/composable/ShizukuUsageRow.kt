// SPDX-FileCopyrightText: 2026 Tim Schneeberger <spam@timschneeberger.me>
// SPDX-License-Identifier: GPL-3.0-or-later

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.data.model.AppDetails

/**
 * One-line AI source analysis, matching the section rows below it. Tapping
 * opens the full markdown report. Hidden until the server has an analysis, so
 * apps without a public source repo show nothing rather than a guess.
 */
@Composable
fun ShizukuUsageRow(details: AppDetails, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val subtitle = details.usageShort?.takeIf { it.isNotBlank() } ?: return

    SectionHeader(
        modifier = modifier,
        title = stringResource(R.string.details_shizuku_title),
        subtitle = subtitle,
        icon = R.drawable.ic_shizuku_icon,
        onClick = onClick
    )
}
