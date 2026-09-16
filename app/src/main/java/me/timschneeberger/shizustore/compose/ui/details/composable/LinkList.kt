/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.extensions.viewExternal

@Composable
fun LinkList(details: AppDetails, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val websiteLabel = stringResource(R.string.details_website)
    val fdroidLabel = stringResource(R.string.details_fdroid)
    val sourceCodeLabel = stringResource(R.string.details_source_code)
    val storeLabel = stringResource(R.string.details_store)
    val issueTrackerLabel = stringResource(R.string.details_issue_tracker)
    val translationLabel = stringResource(R.string.details_translation)
    val donateLabel = stringResource(R.string.details_donate)

    val sourceCodeUrl = details.sourceUrl?.takeIf { it.isNotBlank() }
    val websiteUrl = details.url?.takeIf { it.isNotBlank() }

    // A forge website with no dedicated source link is the source link: label
    // it as source code and hide the otherwise duplicate Website row.
    val forgeAsSource = websiteUrl?.takeIf {
        sourceCodeUrl == null && isForgeRepositoryUrl(it)
    }

    val links = remember(
        details,
        sourceCodeLabel,
        fdroidLabel,
        websiteLabel,
        storeLabel,
        issueTrackerLabel,
        translationLabel,
        donateLabel
    ) {
        buildList {
            val seen = mutableSetOf<String>()

            fun addOnce(label: String, url: String) {
                if (url.isNotBlank() && seen.add(url)) add(label to url)
            }

            (sourceCodeUrl ?: forgeAsSource)?.let { addOnce(sourceCodeLabel, it) }

            // Play-only apps link to the store through the notice card above,
            // so a Website/Store row would just repeat that link.
            if (details.availability != Availability.PLAY_REDIRECT) {
                if (forgeAsSource == null) {
                    websiteUrl?.let {
                        addOnce(if (isFDroidUrl(it)) fdroidLabel else websiteLabel, it)
                    }
                }
                details.storeUrl?.let { addOnce(storeLabel, it) }
            }

            details.issueTracker?.let { addOnce(issueTrackerLabel, it) }
            details.translation?.let { addOnce(translationLabel, it) }
            details.donate.forEach { addOnce(donateLabel, it) }
        }
    }

    if (links.isEmpty()) return

    Column(modifier = modifier) {
        links.forEach { (label, url) ->
            SectionHeader(
                title = label,
                subtitle = url,
                onClick = { context.viewExternal(url) },
                trailing = {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = stringResource(R.string.action_open_external),
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_default)),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}

private val FORGE_HOSTS = setOf("github.com", "gitlab.com", "codeberg.org")

private const val FDROID_HOST = "f-droid.org"

private val urlHostRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/?#]+)", RegexOption.IGNORE_CASE)

internal fun urlHost(url: String): String? = urlHostRegex.find(url)?.groupValues?.get(1)
    ?.substringAfterLast('@')
    ?.substringBefore(':')
    ?.lowercase()

internal fun isForgeRepositoryUrl(url: String): Boolean {
    val host = urlHost(url) ?: return false
    return FORGE_HOSTS.any { host == it || host.endsWith(".$it") }
}

internal fun isFDroidUrl(url: String): Boolean {
    val host = urlHost(url) ?: return false
    return host == FDROID_HOST || host.endsWith(".$FDROID_HOST")
}
