/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.annotation.DrawableRes
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
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.extensions.viewExternal

private data class LinkRow(val label: String, val url: String, val icon: Int)

@Composable
fun LinkList(details: AppDetails, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val websiteLabel = stringResource(R.string.details_website)
    val fdroidLabel = stringResource(R.string.details_fdroid)
    val sourceCodeLabel = stringResource(R.string.details_source_code)
    val storeLabel = stringResource(R.string.details_store)

    val sourceCodeUrl = details.sourceUrl?.takeIf {
        it.isNotBlank() && isSourceCodeLink(details.listing, it)
    }
    val websiteUrl = details.url?.takeIf { it.isNotBlank() }

    // A closed-source project links its forge page for release downloads, so the
    // link stays reachable but is listed as a website, never as source code.
    val forgeWebsiteUrl = details.sourceUrl?.takeIf {
        details.listing == Listing.CLOSED_SOURCE &&
            it.isNotBlank() &&
            it != websiteUrl &&
            isForgeRepositoryUrl(it)
    }

    // A forge website with no dedicated source link is the source link: label
    // it as source code and hide the otherwise duplicate Website row.
    val forgeAsSource = websiteUrl?.takeIf {
        sourceCodeUrl == null &&
            isSourceCodeLink(details.listing, it) &&
            isForgeRepositoryUrl(it)
    }

    val links = remember(
        details,
        sourceCodeLabel,
        fdroidLabel,
        websiteLabel,
        storeLabel
    ) {
        buildList {
            val seen = mutableSetOf<String>()

            fun addOnce(label: String, url: String, @DrawableRes icon: Int) {
                if (url.isNotBlank() && seen.add(url)) add(LinkRow(label, url, icon))
            }

            (sourceCodeUrl ?: forgeAsSource)?.let {
                addOnce(sourceCodeLabel, it, R.drawable.ic_code)
            }

            // Play-only apps link to the store through the notice card above,
            // so a Website/Store row would just repeat that link.
            if (details.availability != Availability.PLAY_REDIRECT) {
                if (forgeAsSource == null) {
                    websiteUrl?.let {
                        addOnce(
                            if (isFDroidUrl(it)) fdroidLabel else websiteLabel,
                            it,
                            R.drawable.ic_language
                        )
                    }
                }
                forgeWebsiteUrl?.let { addOnce(websiteLabel, it, R.drawable.ic_language) }
                details.storeUrl?.let { addOnce(storeLabel, it, R.drawable.ic_storefront) }
            }
        }
    }

    if (links.isEmpty()) return

    Column(modifier = modifier) {
        links.forEach { link ->
            SectionHeader(
                title = link.label,
                subtitle = link.url,
                icon = link.icon,
                onClick = { context.viewExternal(link.url) },
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

/**
 * Whether a link may be labelled as source code. Forges host release binaries
 * for closed-source projects, so their links are websites, not source.
 */
internal fun isSourceCodeLink(listing: Listing, url: String): Boolean =
    listing != Listing.CLOSED_SOURCE || !isForgeRepositoryUrl(url)

internal fun isFDroidUrl(url: String): Boolean {
    val host = urlHost(url) ?: return false
    return host == FDROID_HOST || host.endsWith(".$FDROID_HOST")
}
