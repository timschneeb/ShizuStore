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
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.extensions.viewExternal

@Composable
fun LinkList(details: AppDetails, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val websiteLabel = stringResource(R.string.details_website)
    val sourceCodeLabel = stringResource(R.string.details_source_code)
    val storeLabel = stringResource(R.string.details_store)
    val issueTrackerLabel = stringResource(R.string.details_issue_tracker)
    val changelogLabel = stringResource(R.string.details_changelog)
    val translationLabel = stringResource(R.string.details_translation)
    val donateLabel = stringResource(R.string.details_donate)

    val links = buildList {
        val seen = mutableSetOf<String>()

        fun addOnce(label: String, url: String) {
            if (url.isNotBlank() && seen.add(url)) add(label to url)
        }

        details.sourceCode?.let { addOnce(sourceCodeLabel, it) }

        // Play-only apps link to the store through the notice card above,
        // so a Website/Store row would just repeat that link.
        if (details.availability != Availability.PLAY_REDIRECT) {
            details.webSite?.let { addOnce(websiteLabel, it) }
            details.storeUrl?.let { addOnce(storeLabel, it) }
        }

        details.issueTracker?.let { addOnce(issueTrackerLabel, it) }
        details.changelog?.let { addOnce(changelogLabel, it) }
        details.translation?.let { addOnce(translationLabel, it) }
        details.donate.forEach { addOnce(donateLabel, it) }
    }

    if (links.isEmpty()) return

    Column(modifier = modifier) {
        links.forEach { (label, url) ->
            AuroraListItem(
                headline = label,
                supporting = url,
                onClick = { context.viewExternal(url) }
            )
        }
    }
}
