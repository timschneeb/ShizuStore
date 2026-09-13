/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import me.timschneeberger.shizustore.data.api.AppType
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.SourceKind

/**
 * One catalog entry keyed by the server `slug`. Summary fields are refreshed on
 * every sync; detail fields only from `/v1/apps/{slug}`. The install/update
 * columns are denormalized so paging queries never need a join.
 */
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
    val trialDays: Int? = null,
    val requiresRoot: Boolean = false,
    val availability: Availability = Availability.LINK_ONLY,
    val versionCode: Long? = null,
    val versionName: String? = null,
    val minSdk: Int? = null,
    val size: Long? = null,
    val iconHash: String? = null,
    val iconAdaptive: Boolean = false,
    val categorySlug: String? = null,
    val updatedAt: String = "",
    val versionUpdatedAt: String? = null,
    val listUpdatedAt: String? = null,
    val sigSha256: String? = null,
    val sigMd5: String? = null,
    val stars: Int? = null,
    val downloadTotal: Long? = null,
    val url: String? = null,
    val sourceUrl: String? = null,
    val sourceKind: SourceKind? = null,
    val storeUrl: String? = null,
    val excludedReason: String? = null,
    val parentSlug: String? = null,
    val categoryPath: List<CategoryPath> = emptyList(),
    val addedAt: String? = null,
    val lastCheckedAt: String? = null,
    val detailsFetchedAt: Long? = null,
    val installedVersionCode: Long? = null,
    val updateAvailable: Boolean = false,
    val updateCandidateId: Long? = null,
    val syncedAt: Long = 0L
)

/**
 * Keeps the detail columns when a summary upsert refreshes the same row, so an
 * incremental `/v1/changes` pass never discards previously fetched detail.
 */
fun AppEntity.mergeDetailFrom(existing: AppEntity?): AppEntity = if (existing == null) {
    this
} else {
    copy(
        url = existing.url,
        sourceUrl = existing.sourceUrl,
        sourceKind = existing.sourceKind,
        storeUrl = existing.storeUrl,
        excludedReason = existing.excludedReason,
        parentSlug = existing.parentSlug,
        categoryPath = existing.categoryPath,
        addedAt = existing.addedAt,
        lastCheckedAt = existing.lastCheckedAt,
        detailsFetchedAt = existing.detailsFetchedAt,
        installedVersionCode = existing.installedVersionCode,
        updateAvailable = existing.updateAvailable,
        updateCandidateId = existing.updateCandidateId
    )
}
