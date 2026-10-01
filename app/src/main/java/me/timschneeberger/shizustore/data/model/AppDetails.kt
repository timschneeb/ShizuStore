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
import me.timschneeberger.shizustore.data.room.entity.TrackerTag

@Immutable
data class AppDetails(
    val packageName: String,
    val slug: String,
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
    /** Badging and signer facts from the primary APK; empty on link-only or pre-analysis entries. */
    val targetSdk: Int? = null,
    val compileSdk: Int? = null,
    val localeCount: Int? = null,
    val locales: List<String> = emptyList(),
    val localizedLabels: Map<String, String> = emptyMap(),
    /** [name] translated into one of the current device locales, when the APK ships one. */
    val localizedName: String? = null,
    val abis: List<String> = emptyList(),
    val signerDn: String? = null,
    val signerScheme: String? = null,
    val signerKeyAlgorithm: String? = null,
    /** AI source analysis; null until the server analyzed the app (SPEC 5.4). */
    val usageShort: String? = null,
    val usageMarkdown: String? = null,
    val usageAnalyzedAt: String? = null,
    val permissions: List<String>,
    val installedVersionCode: Long?,
    val availability: Availability = Availability.DIRECT_APK,
    val storeUrl: String? = null,
    val url: String? = null,
    val sourceUrl: String? = null,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val hasAds: Boolean = false,
    val dhizukuDeclared: Boolean = false,
    val trackers: List<String> = emptyList(),
    val trackerTags: List<TrackerTag> = emptyList(),
    val stars: Int? = null,
    val installCount: Long = 0,
    val downloadTotal: Long? = null,
    val versionUpdatedAtMillis: Long? = null,
    val fullDescription: String? = null,
    /** Raw markdown URL the live README is fetched from; null for Play-only entries. */
    val readmeUrl: String? = null,
    val sourceKind: SourceKind? = null,
    val sourceName: String? = null,
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
