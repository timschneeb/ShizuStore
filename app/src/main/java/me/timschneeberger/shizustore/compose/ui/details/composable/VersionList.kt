/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.LabelChip
import me.timschneeberger.shizustore.compose.composable.minTouchTarget
import me.timschneeberger.shizustore.data.model.AppSource
import me.timschneeberger.shizustore.data.model.ReleaseChannel
import me.timschneeberger.shizustore.data.model.preferredForThisDevice
import me.timschneeberger.shizustore.util.CommonUtil

internal data class VersionRow(
    val source: AppSource,
    val isSelected: Boolean,
    val signerDiffers: Boolean
) {
    val isInstalled: Boolean get() = source.isInstalled

    val channel: ReleaseChannel get() = source.channel

    val abiLabel: String? get() = source.abiLabel

    val runsHere: Boolean get() = source.runsOnThisDevice

    val selectable: Boolean get() = !signerDiffers && runsHere
}

internal fun versionRows(sources: List<AppSource>): List<VersionRow> {
    val preselected = sources.indexOf(sources.preferredForThisDevice())
    return sources.mapIndexed { index, source ->
        VersionRow(
            source = source,
            isSelected = index == preselected,
            signerDiffers = source.app.signerDiffersFromInstalled
        )
    }
}

@Composable
fun VersionList(
    sources: List<AppSource>,
    modifier: Modifier = Modifier,
    onSelect: (AppSource) -> Unit = {}
) {
    if (sources.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val rows = remember(sources) { versionRows(sources) }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) CHEVRON_EXPANDED_DEGREES else 0f,
        label = "versionsChevron"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minTouchTarget()
                .clickable { expanded = !expanded }
                .padding(
                    horizontal = dimensionResource(R.dimen.spacing_large),
                    vertical = dimensionResource(R.dimen.spacing_xsmall)
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.details_versions),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.details_versions_count,
                        rows.size,
                        rows.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = stringResource(
                    if (expanded) R.string.action_collapse else R.string.action_expand
                ),
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_default))
                    .rotate(rotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                rows.forEach { row ->
                    val origin = when {
                        row.signerDiffers -> stringResource(
                            R.string.details_version_repo_other_signer,
                            row.source.app.repoName
                        )

                        else -> row.source.app.repoName
                    }

                    AuroraListItem(
                        headline = row.source.app.versionName,
                        supporting = listOfNotNull(releasedOn(row.source.added), origin)
                            .joinToString(SEPARATOR),
                        tertiary = row.abiLabel,
                        tertiaryMaxLines = 2,
                        onClick = { onSelect(row.source) },
                        enabled = row.selectable,
                        headlineStyle = MaterialTheme.typography.bodyLarge,
                        trailing = { VersionChips(row) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionChips(row: VersionRow) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (row.isSelected) {
            LabelChip(
                text = stringResource(R.string.details_version_selected),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (row.isInstalled) {
            LabelChip(
                text = stringResource(R.string.details_version_installed),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val (container, content) = when (row.channel) {
            ReleaseChannel.STABLE ->
                MaterialTheme.colorScheme.secondaryContainer to
                    MaterialTheme.colorScheme.onSecondaryContainer

            ReleaseChannel.BETA ->
                MaterialTheme.colorScheme.tertiaryContainer to
                    MaterialTheme.colorScheme.onTertiaryContainer

            ReleaseChannel.ALPHA ->
                MaterialTheme.colorScheme.errorContainer to
                    MaterialTheme.colorScheme.onErrorContainer
        }
        LabelChip(
            text = stringResource(
                when (row.channel) {
                    ReleaseChannel.STABLE -> R.string.details_channel_stable
                    ReleaseChannel.BETA -> R.string.details_channel_beta
                    ReleaseChannel.ALPHA -> R.string.details_channel_alpha
                }
            ),
            container = container,
            content = content
        )

        if (!row.runsHere) {
            LabelChip(
                text = stringResource(R.string.details_version_incompatible),
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun releasedOn(added: Long): String? =
    remember(added) { if (added <= 0L) null else CommonUtil.formatDate(added) }

private const val CHEVRON_EXPANDED_DEGREES = 180f

private const val SEPARATOR = " · "
