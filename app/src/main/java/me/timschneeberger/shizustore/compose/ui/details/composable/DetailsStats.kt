/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.util.CommonUtil

private data class DetailStat(
    val iconRes: Int,
    val value: String,
    val label: String,
    val tooltip: String? = null
)

/** Readable floor per cell; below it the row scrolls instead of clipping. */
private val StatMinWidth = 64.dp

/** Four-metric strip under the install section; unknown or zero metrics are omitted. */
@Composable
fun DetailsStats(details: AppDetails, modifier: Modifier = Modifier) {
    // Non-installable entries carry no download metrics; their star count is
    // shown as a chip in the chip row instead.
    if (details.availability != Availability.DIRECT_APK) return

    val stats = buildList {
        if (details.installCount > 0L) {
            add(
                DetailStat(
                    iconRes = R.drawable.ic_download_manager,
                    value = CommonUtil.formatCount(details.installCount),
                    label = stringResource(R.string.details_stats_store),
                    tooltip = stringResource(R.string.details_stats_installs_tooltip)
                )
            )
        }
        details.downloadTotal?.takeIf { it > 0L }?.let {
            val source = details.sourceName?.takeIf { name -> name.isNotBlank() }
            add(
                DetailStat(
                    iconRes = R.drawable.ic_download_manager,
                    value = CommonUtil.formatCount(it),
                    // The count comes from the upstream release page, so the
                    // label names the source (GitHub, GitLab, ...).
                    label = source ?: stringResource(R.string.details_stats_downloads),
                    tooltip = stringResource(
                        R.string.details_stats_downloads_tooltip,
                        source ?: stringResource(R.string.details_stats_downloads)
                    )
                )
            )
        }
        details.stars?.takeIf { it > 0 }?.let {
            add(
                DetailStat(
                    iconRes = R.drawable.ic_star,
                    value = CommonUtil.formatCount(it.toLong()),
                    label = stringResource(R.string.details_stats_stars),
                    tooltip = stringResource(R.string.details_stats_stars_tooltip)
                )
            )
        }
        CommonUtil.sizeLabel(details.size)?.let {
            add(
                DetailStat(
                    iconRes = R.drawable.ic_sd_card,
                    value = it,
                    label = stringResource(R.string.details_stats_size),
                    tooltip = stringResource(R.string.details_stats_size_tooltip)
                )
            )
        }
    }

    if (stats.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A width change (multi-window resize) re-probes the weighted layout:
        // key() discards the remembered overflow flag and measures again.
        key(maxWidth) {
            var overflowed by remember { mutableStateOf(false) }
            // Equal cells below the readable floor switch to the scrollable
            // layout before anything clips; the overflow probe catches long
            // values and labels on top of that.
            val available = maxWidth - dimensionResource(R.dimen.spacing_large) * 2
            val scrollMode = overflowed || available / stats.size < StatMinWidth
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Equal cell heights so every tap target covers the whole
                    // strip, not just its own text bounds.
                    .height(IntrinsicSize.Min)
                    .padding(
                        // 2dp over the standard rhythm so the strip breathes a little.
                        top = dimensionResource(R.dimen.spacing_xsmall) + 2.dp,
                        bottom = dimensionResource(R.dimen.spacing_small) + 2.dp
                    )
                    .then(if (scrollMode) Modifier.horizontalScroll(scrollState) else Modifier)
                    // Inside the scroll viewport so items scroll to the screen
                    // edge instead of clipping at the margin early.
                    .padding(
                        start = dimensionResource(R.dimen.spacing_large),
                        end = dimensionResource(R.dimen.spacing_large)
                    ),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stats.forEachIndexed { index, stat ->
                    if (index > 0) {
                        VerticalDivider(
                            modifier = Modifier.height(40.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    StatCell(
                        stat = stat,
                        onOverflow = { overflowed = true },
                        modifier = if (scrollMode) {
                            Modifier.widthIn(min = StatMinWidth)
                        } else {
                            Modifier.weight(1f)
                        }
                    )
                }
            }
        }
    }
}

/** One metric cell; tapping a cell with a tooltip explains where its count comes from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatCell(stat: DetailStat, onOverflow: () -> Unit, modifier: Modifier = Modifier) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    // The weight/width modifier must sit on a direct Row child: TooltipBox
    // applies its modifier inside its own wrapper Box, where a RowScope
    // weight would be ignored.
    Box(modifier = modifier.fillMaxHeight()) {
        TooltipBox(
            // The deprecated plain provider centers without clamping, so the
            // leftmost tooltip ran off screen; the positioned provider clamps
            // to the window.
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above
            ),
            tooltip = {
                stat.tooltip?.let { text -> PlainTooltip { Text(text) } }
            },
            state = tooltipState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_xsmall),
                    Alignment.CenterVertically
                ),
                modifier = Modifier
                    .fillMaxSize()
                    // Rounded ripple over the full cell; padding sits after
                    // clickable so the tap target includes it.
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                    .then(
                        if (stat.tooltip != null) {
                            Modifier.clickable { scope.launch { tooltipState.show() } }
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = dimensionResource(R.dimen.spacing_xsmall))
            ) {
                Icon(
                    painter = painterResource(stat.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_chip))
                )
                Text(
                    text = stat.value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    // A clipped value means the equal-width cells are too narrow;
                    // switch to the scrollable, wrap-content fallback.
                    onTextLayout = { if (it.hasVisualOverflow) onOverflow() }
                )
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Labels may carry explicit line breaks; let them wrap instead
                    // of clipping and forcing scroll mode.
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
