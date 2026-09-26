/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import me.timschneeberger.shizustore.data.api.AppType
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.SourceKind

/** Install/update columns are denormalized so paging queries never need a join. */
@Entity(
    tableName = "app",
    indices = [
        Index("packageName"),
        Index("categorySlug"),
        Index("availability"),
        Index("updateAvailable")
    ]
)
data class AppEntity(
    @PrimaryKey val slug: String,
    val name: String = "",
    val description: String = "",
    val packageName: String? = null,
    val license: String? = null,
    val listing: Listing = Listing.MAIN,
    val type: AppType = AppType.APP,
    val isRecommended: Boolean = false,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val hasAds: Boolean = false,
    /** Declared Dhizuku permission, read from the primary APK by the server. */
    @ColumnInfo(defaultValue = "0")
    val dhizukuDeclared: Boolean = false,
    /** Exodus tracker names matched in the primary APK's code signatures. */
    @ColumnInfo(defaultValue = "[]")
    val trackers: List<String> = emptyList(),
    /** The same trackers with their Exodus category tags, for the detail dialog. */
    @ColumnInfo(defaultValue = "[]")
    val trackerTags: List<TrackerTag> = emptyList(),
    val trialDays: Int? = null,
    val requiresRoot: Boolean = false,
    val availability: Availability = Availability.LINK_ONLY,
    val versionCode: Long? = null,
    val versionName: String? = null,
    val minSdk: Int? = null,
    /** Badging facts from the primary APK; null/empty until the server analyzed it. */
    val targetSdk: Int? = null,
    val compileSdk: Int? = null,
    val localeCount: Int? = null,
    @ColumnInfo(defaultValue = "[]")
    val abis: List<String> = emptyList(),
    /** Locale labels that differ from the app name, for device-locale display. */
    @ColumnInfo(defaultValue = "{}")
    val localizedLabels: Map<String, String> = emptyMap(),
    val size: Long? = null,
    val iconHash: String? = null,
    val iconAdaptive: Boolean = false,
    val categorySlug: String? = null,
    val updatedAt: String = "",
    val versionUpdatedAt: String? = null,
    val listUpdatedAt: String? = null,
    val authorKey: String? = null,
    val authorName: String? = null,
    val sigSha256: String? = null,
    val sigMd5: String? = null,
    val stars: Int? = null,
    val downloadTotal: Long? = null,
    /** Successful installs via this app, reported by clients and served by /v1/meta flag. */
    val installCount: Long = 0,
    val url: String? = null,
    val sourceUrl: String? = null,
    val sourceKind: SourceKind? = null,
    val sourceName: String? = null,
    val storeUrl: String? = null,
    val authorUrl: String? = null,
    val permissions: List<String> = emptyList(),
    val excludedReason: String? = null,
    val parentSlug: String? = null,
    val categoryPath: List<CategoryPath> = emptyList(),
    val addedAt: String? = null,
    val lastCheckedAt: String? = null,
    val detailsFetchedAt: Long? = null,
    val installedVersionCode: Long? = null,
    val updateAvailable: Boolean = false,
    val updateCandidateId: Long? = null,
    val syncedAt: Long = 0L,
    /** AI usage report from the server; null until the app was analyzed (SPEC 5.4). */
    val usageShort: String? = null,
    val usageMarkdown: String? = null,
    val usageAnalyzedAt: String? = null,
    /** Report format generation of the cached text; older rows refetch once. */
    val usageReportVersion: Int? = null
)

/** Bump when the server changes the report layout so cached rows refetch once. */
const val USAGE_REPORT_VERSION = 1

/** One detected tracker with its Exodus category tags. */
@Serializable
data class TrackerTag(
    val name: String,
    val tags: List<String> = emptyList()
)

/** Keeps detail columns when a summary upsert refreshes the row, so `/v1/changes` never discards them. */
fun AppEntity.mergeDetailFrom(existing: AppEntity?): AppEntity = if (existing == null) {
    this
} else {
    copy(
        url = existing.url,
        sourceUrl = existing.sourceUrl,
        sourceKind = existing.sourceKind,
        storeUrl = existing.storeUrl,
        authorUrl = existing.authorUrl,
        permissions = existing.permissions,
        excludedReason = existing.excludedReason,
        parentSlug = existing.parentSlug,
        categoryPath = existing.categoryPath,
        addedAt = existing.addedAt,
        lastCheckedAt = existing.lastCheckedAt,
        detailsFetchedAt = existing.detailsFetchedAt,
        installedVersionCode = existing.installedVersionCode,
        updateAvailable = existing.updateAvailable,
        updateCandidateId = existing.updateCandidateId,
        usageShort = existing.usageShort,
        usageMarkdown = existing.usageMarkdown,
        usageAnalyzedAt = existing.usageAnalyzedAt,
        usageReportVersion = existing.usageReportVersion
    )
}
