/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import me.timschneeberger.shizustore.data.api.Availability

sealed interface SourceTarget {
    data object Apk : SourceTarget

    data class Play(val storeUrl: String) : SourceTarget

    data class Link(val url: String) : SourceTarget

    data object None : SourceTarget
}

fun sourceTarget(
    availability: Availability,
    storeUrl: String?,
    url: String?,
    sourceUrl: String?
): SourceTarget = when (availability) {
    Availability.DIRECT_APK -> SourceTarget.Apk

    Availability.PLAY_REDIRECT -> storeUrl?.takeIf { it.isNotBlank() }
        ?.let { SourceTarget.Play(it) }
        ?: SourceTarget.None

    Availability.LINK_ONLY -> (url ?: sourceUrl)?.takeIf { it.isNotBlank() }
        ?.let { SourceTarget.Link(it) }
        ?: SourceTarget.None

    Availability.EXCLUDED -> SourceTarget.None
}

object SourceLauncher {
    fun launch(
        context: Context,
        availability: Availability,
        storeUrl: String?,
        url: String?,
        sourceUrl: String?
    ) {
        when (val target = sourceTarget(availability, storeUrl, url, sourceUrl)) {
            is SourceTarget.Play -> open(context, target.storeUrl)
            is SourceTarget.Link -> open(context, target.url)
            SourceTarget.Apk, SourceTarget.None -> Unit
        }
    }

    fun open(context: Context, link: String) {
        runCatching {
            CustomTabsIntent.Builder().build().launchUrl(context, link.toUri())
        }
    }
}
