/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.util.CommonUtil

private data class DetailStat(val iconRes: Int, val value: String, val label: String)

/** Four-metric strip under the install section; unknown or zero metrics are omitted. */
@Composable
fun DetailsStats(
    details: AppDetails,
    modifier: Modifier = Modifier
) {
    val stats = buildList {
        if (details.installCount > 0L) {
            add(
                DetailStat(
                    iconRes = R.drawable.ic_download_manager,
                    value = CommonUtil.formatCount(details.installCount),
                    label = stringResource(R.string.details_stats_installs)
                )
            )
        }
        details.downloadTotal?.takeIf { it > 0L }?.let {
            add(
                DetailStat(
                    iconRes = R.drawable.ic_arrow_down,
                    value = CommonUtil.formatCount(it),
                    label = stringResource(R.string.details_stats_downloads)
                )
            )
        }
        details.stars?.takeIf { it > 0 }?.let {
            add(
                DetailStat(
                    iconRes = R.drawable.ic_star,
                    value = CommonUtil.formatCount(it.toLong()),
                    label = stringResource(R.string.details_stats_stars)
                )
            )
        }
        CommonUtil.sizeLabel(details.size)?.let {
            add(
                DetailStat(
                    iconRes = R.drawable.ic_sd_card,
                    value = it,
                    label = stringResource(R.string.details_stats_size)
                )
            )
        }
    }

    if (stats.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        stats.forEach { stat ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
            ) {
                Icon(
                    painter = painterResource(stat.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_chip))
                )
                Text(
                    text = stat.value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
