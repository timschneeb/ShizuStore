/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.LabelChip
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.util.CommonUtil

@Composable
fun AppListItem(
    app: ResolvedApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    showStars: Boolean = false,
    showInstalls: Boolean = false,
    supporting: String? = app.summary
) {
    val installsText = if (showInstalls) {
        stringResource(R.string.app_installs, CommonUtil.formatCount(app.installCount))
    } else {
        null
    }
    val starsText = if (showStars) {
        app.stars?.let { stringResource(R.string.app_stars, CommonUtil.formatCount(it.toLong())) }
    } else {
        null
    }
    val countPart: AnnotatedString? = remember(installsText, starsText) {
        when {
            installsText != null -> buildAnnotatedString {
                appendInlineContent(INSTALLS_ICON_ID, "[downloads]")
                append(" ")
                append(installsText)
            }
            starsText != null -> AnnotatedString(starsText)
            else -> null
        }
    }
    val sizeLabel = CommonUtil.sizeLabel(app.size)
    val versionText = if (sizeLabel != null) {
        stringResource(R.string.app_version_size, app.versionName, sizeLabel)
    } else {
        app.versionName
    }
    val tertiaryText = remember(countPart, versionText) {
        countPart?.let {
            buildAnnotatedString {
                append(it)
                append("  ·  ")
                append(versionText)
            }
        } ?: AnnotatedString(versionText)
    }
    // The installs glyph is the download drawable, tinted to match the
    // tertiary text: InlineTextContent children do not inherit it.
    val installsPainter = if (showInstalls) {
        painterResource(
            R.drawable.ic_download_manager
        )
    } else {
        null
    }
    val tertiaryTint = MaterialTheme.colorScheme.onSurfaceVariant
    val tertiaryInlineContent = remember(installsPainter, tertiaryTint) {
        if (installsPainter != null) {
            mapOf(
                INSTALLS_ICON_ID to InlineTextContent(
                    Placeholder(
                        TERTIARY_ICON_SIZE,
                        TERTIARY_ICON_SIZE,
                        PlaceholderVerticalAlign.Center
                    )
                ) {
                    Image(
                        painter = installsPainter,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tertiaryTint)
                    )
                }
            )
        } else {
            emptyMap()
        }
    }
    val installedIcon = when {
        app.hasUpdate -> R.drawable.ic_updates
        app.isInstalled -> R.drawable.ic_check_circle
        else -> null
    }
    val showBadges = installedIcon != null || app.hasPaid || app.hasIap

    AuroraListItem(
        modifier = modifier,
        headline = app.name.ifBlank { app.packageName },
        supporting = supporting?.takeIf { it.isNotBlank() },
        tertiary = tertiaryText,
        tertiaryInlineContent = tertiaryInlineContent,
        headlineStyle = MaterialTheme.typography.bodyMedium,
        onClick = onClick,
        trailing = trailing,
        badges = if (showBadges) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_xsmall)
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (installedIcon != null) {
                        Icon(
                            painter = painterResource(installedIcon),
                            contentDescription = if (app.hasUpdate) {
                                stringResource(R.string.app_indicator_update)
                            } else {
                                stringResource(R.string.app_indicator_installed)
                            },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
        leading = {
            AsyncImage(
                model = rememberAppIconModel(app.iconUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.icon_size_medium))
                    .clip(appIconShape(app.iconAdaptive))
            )
        }
    )
}

private const val INSTALLS_ICON_ID = "installs"

private val TERTIARY_ICON_SIZE = 14.sp
