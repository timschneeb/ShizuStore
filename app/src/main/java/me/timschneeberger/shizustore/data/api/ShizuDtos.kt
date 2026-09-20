/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.serialization.Serializable

/**
 * Wire models mirroring `Api/Dtos.cs`. Dates stay as raw ISO-8601 strings
 * because the `since` cursor is passed back to the server verbatim.
 */

@Serializable
data class AppSummaryDto(
    val slug: String,
    val name: String = "",
    val description: String = "",
    val license: String? = null,
    val listing: String = "",
    val type: String = "",
    val isRecommended: Boolean = false,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val hasAds: Boolean = false,
    val trialDays: Int? = null,
    val requiresRoot: Boolean = false,
    val availability: String = "",
    val packageName: String? = null,
    val versionCode: Long? = null,
    val versionName: String? = null,
    val minSdk: Int? = null,
    val size: Long? = null,
    val iconHash: String? = null,
    val iconAdaptive: Boolean = false,
    val categorySlug: String = "",
    val updatedAt: String = "",
    val sigSha256: String? = null,
    val sigMd5: String? = null,
    val stars: Int? = null,
    val downloadTotal: Long? = null,
    val installCount: Long = 0,
    val versionUpdatedAt: String? = null,
    val listUpdatedAt: String? = null,
    val authorKey: String? = null,
    val authorName: String? = null,
    val sourceName: String? = null
)

@Serializable
data class DownloadDto(
    val source: String = "",
    // The package this flavor installs as; differs from the app's canonical package.
    val packageName: String? = null,
    val apkUrl: String,
    val archiveEntry: String? = null,
    val versionCode: Long? = null,
    val versionName: String? = null,
    val size: Long? = null,
    val sha256: String? = null,
    val sigSha256: String? = null,
    val sigMd5: String? = null,
    val minSdk: Int? = null,
    val abi: String? = null,
    val primary: Boolean = false
)

@Serializable
data class CategoryPathDto(
    val slug: String,
    val name: String = ""
)

@Serializable
data class AppDetailDto(
    val slug: String,
    val name: String = "",
    val description: String = "",
    val license: String? = null,
    val listing: String = "",
    val type: String = "",
    val isRecommended: Boolean = false,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val hasAds: Boolean = false,
    val trialDays: Int? = null,
    val requiresRoot: Boolean = false,
    val availability: String = "",
    val packageName: String? = null,
    val versionCode: Long? = null,
    val versionName: String? = null,
    val minSdk: Int? = null,
    val iconHash: String? = null,
    val iconAdaptive: Boolean = false,
    val categorySlug: String = "",
    val updatedAt: String = "",
    val url: String? = null,
    val sourceUrl: String? = null,
    val sourceKind: String = "",
    val downloads: List<DownloadDto> = emptyList(),
    val storeUrl: String? = null,
    val excludedReason: String? = null,
    val categoryPath: List<CategoryPathDto> = emptyList(),
    val parentSlug: String? = null,
    val addedAt: String = "",
    val lastCheckedAt: String? = null,
    val stars: Int? = null,
    val downloadTotal: Long? = null,
    val installCount: Long = 0,
    val versionUpdatedAt: String? = null,
    val listUpdatedAt: String? = null,
    val authorName: String? = null,
    val authorUrl: String? = null,
    val permissions: List<String> = emptyList(),
    val fullDescription: String? = null,
    val changelog: String? = null,
    val changelogUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val sourceName: String? = null
)

@Serializable
data class CategoryNodeDto(
    val slug: String,
    val name: String = "",
    val section: String = "",
    val appCount: Int = 0,
    val children: List<CategoryNodeDto> = emptyList()
)

@Serializable
data class PagedAppsDto(
    val items: List<AppSummaryDto> = emptyList(),
    val total: Int = 0
)

@Serializable
data class RemovedAppDto(
    val slug: String
)

@Serializable
data class ChangesDto(
    val added: List<AppSummaryDto> = emptyList(),
    val updated: List<AppSummaryDto> = emptyList(),
    val removed: List<RemovedAppDto> = emptyList(),
    // Delta counts applied directly onto stored rows, never as summaries, so a
    // popular app cannot force a detail refetch.
    val installsUpdated: Map<String, Long> = emptyMap(),
    // Operator high-water mark for remote catalog purges; older or absent
    // means no purge. Raw ISO string, parsed only when compared.
    val catalogPurgeRequestedAt: String? = null
)

@Serializable
data class MetaDto(
    val generatedAt: String = "",
    val listCommit: String? = null,
    val useInstallCountsForPopularity: Boolean = false
)

@Serializable
data class HealthDto(
    val status: String = ""
)

@Serializable
data class InstallRecordedDto(
    val slug: String = "",
    val installCount: Long = 0
)
