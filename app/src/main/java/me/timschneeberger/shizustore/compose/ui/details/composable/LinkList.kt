/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.extensions.viewExternal

@Composable
fun LinkList(details: AppDetails, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val websiteLabel = stringResource(R.string.details_website)
    val sourceCodeLabel = stringResource(R.string.details_source_code)
    val issueTrackerLabel = stringResource(R.string.details_issue_tracker)
    val changelogLabel = stringResource(R.string.details_changelog)
    val translationLabel = stringResource(R.string.details_translation)
    val donateLabel = stringResource(R.string.details_donate)

    val links = buildList {
        details.webSite?.takeIf { it.isNotBlank() }?.let { add(websiteLabel to it) }
        details.sourceCode?.takeIf { it.isNotBlank() }?.let { add(sourceCodeLabel to it) }
        details.issueTracker?.takeIf { it.isNotBlank() }?.let { add(issueTrackerLabel to it) }
        details.changelog?.takeIf { it.isNotBlank() }?.let { add(changelogLabel to it) }
        details.translation?.takeIf { it.isNotBlank() }?.let { add(translationLabel to it) }
        details.donate.filter { it.isNotBlank() }.forEach { add(donateLabel to it) }
    }

    if (links.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.details_links))
        links.forEach { (label, url) ->
            AuroraListItem(
                headline = label,
                supporting = url,
                onClick = { context.viewExternal(url) }
            )
        }
    }
}
