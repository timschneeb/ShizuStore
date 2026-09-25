/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.model.AppDetails

private const val OBTAINIUM_REDIRECT_BASE = "https://apps.obtainium.imranr.dev/redirect?r="

/**
 * The repository Obtainium should track: a forge or F-Droid page. Link-only
 * entries stay out because Obtainium cannot manage a listing without a source.
 */
internal fun obtainiumRepoUrl(details: AppDetails): String? =
    obtainiumRepoUrl(details.availability, details.sourceUrl, details.url)

/**
 * Raw-field variant for cached catalog rows, so the action can render before
 * the network detail fetch settles.
 */
internal fun obtainiumRepoUrl(
    availability: Availability,
    sourceUrl: String?,
    url: String?
): String? {
    if (availability == Availability.LINK_ONLY) return null
    val candidate = sourceUrl?.takeIf { it.isNotBlank() }
        ?: url?.takeIf { it.isNotBlank() }
        ?: return null
    return candidate.takeIf { isForgeRepositoryUrl(it) || isFDroidUrl(it) }
}

/** The app link that pre-fills Obtainium's add screen. */
internal fun obtainiumDeepLink(repoUrl: String): String =
    "obtainium://add/" + URLEncoder.encode(repoUrl, Charsets.UTF_8.name())

/**
 * Whether Obtainium (or another handler of its scheme) is installed. The app
 * holds QUERY_ALL_PACKAGES, so the lookup is not filtered by package visibility.
 */
internal fun canHandleObtainium(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("obtainium://add/"))
    return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
}

/**
 * The https redirect page keeps the action usable when Obtainium is absent: it
 * offers the download instead of failing to resolve the custom scheme.
 */
internal fun obtainiumRedirectUrl(repoUrl: String): String =
    OBTAINIUM_REDIRECT_BASE + URLEncoder.encode(obtainiumDeepLink(repoUrl), Charsets.UTF_8.name())
