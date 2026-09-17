/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import androidx.compose.runtime.Immutable
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.SourceKind

@Immutable
data class AppDetails(
    val packageName: String,
    val repoName: String,
    val name: String,
    val description: String,
    val iconUrl: String?,
    val license: String,
    val authorName: String?,
    val authorUrl: String? = null,
    val changelog: String?,
    val categories: List<String>,
    val screenshots: List<String>,
    val lastUpdated: Long,
    val versionName: String,
    val size: Long,
    val minSdk: Int,
    val permissions: List<String>,
    val installedVersionCode: Long?,
    val availability: Availability = Availability.DIRECT_APK,
    val storeUrl: String? = null,
    val url: String? = null,
    val sourceUrl: String? = null,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val stars: Int? = null,
    val fullDescription: String? = null,
    val sourceKind: SourceKind? = null,
    val changelogUrl: String? = null,
    val listing: Listing = Listing.MAIN
) {
    /** Forge or listing page the description screen opens. */
    val browserUrl: String?
        get() = sourceUrl?.takeIf { it.isNotBlank() } ?: url?.takeIf { it.isNotBlank() }

    /** Release page the changelog was read from; index-sourced notes fall back to [browserUrl]. */
    val changelogBrowserUrl: String?
        get() = changelogUrl?.takeIf { it.isNotBlank() } ?: browserUrl
}
