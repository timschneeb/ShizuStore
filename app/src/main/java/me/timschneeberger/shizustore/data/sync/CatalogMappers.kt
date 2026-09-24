/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.sync

import me.timschneeberger.shizustore.data.api.AppDetailDto
import me.timschneeberger.shizustore.data.api.AppSummaryDto
import me.timschneeberger.shizustore.data.api.AppType
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.CategoryNodeDto
import me.timschneeberger.shizustore.data.api.CategorySection
import me.timschneeberger.shizustore.data.api.DownloadDto
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.SourceKind
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryPath

/** Enums fall back to entity defaults for unknown server values, so an addition never drops a row. */

fun AppSummaryDto.toEntity(syncedAt: Long): AppEntity = AppEntity(
    slug = slug,
    name = name,
    description = description,
    packageName = packageName,
    license = license,
    listing = Listing.fromWire(listing) ?: Listing.MAIN,
    type = AppType.fromWire(type) ?: AppType.APP,
    isRecommended = isRecommended,
    hasPaid = hasPaid,
    hasIap = hasIap,
    hasAds = hasAds,
    dhizukuDeclared = dhizukuDeclared,
    trackers = trackers,
    trialDays = trialDays,
    requiresRoot = requiresRoot,
    availability = Availability.fromWire(availability) ?: Availability.LINK_ONLY,
    versionCode = versionCode,
    versionName = versionName,
    minSdk = minSdk,
    size = size,
    iconHash = iconHash,
    iconAdaptive = iconAdaptive,
    categorySlug = categorySlug.ifBlank { null },
    updatedAt = updatedAt,
    sigSha256 = sigSha256,
    sigMd5 = sigMd5,
    stars = stars,
    downloadTotal = downloadTotal,
    installCount = installCount,
    versionUpdatedAt = versionUpdatedAt,
    listUpdatedAt = listUpdatedAt,
    authorKey = authorKey,
    authorName = authorName,
    sourceName = sourceName,
    syncedAt = syncedAt
)

/**
 * Detail carries no top-level signatures; the summary columns keep the primary
 * candidate's sets (server `AppSummaryDto` uses the same rule).
 */
fun AppEntity.applyDetail(detail: AppDetailDto, fetchedAt: Long): AppEntity {
    val primary = detail.downloads.firstOrNull { it.primary } ?: detail.downloads.firstOrNull()
    return copy(
        name = detail.name,
        description = detail.description,
        packageName = detail.packageName,
        license = detail.license,
        listing = Listing.fromWire(detail.listing) ?: listing,
        type = AppType.fromWire(detail.type) ?: type,
        isRecommended = detail.isRecommended,
        hasPaid = detail.hasPaid,
        hasIap = detail.hasIap,
        hasAds = detail.hasAds,
        dhizukuDeclared = detail.dhizukuDeclared,
        trackers = detail.trackers,
        trialDays = detail.trialDays,
        requiresRoot = detail.requiresRoot,
        availability = Availability.fromWire(detail.availability) ?: availability,
        versionCode = detail.versionCode,
        versionName = detail.versionName,
        minSdk = detail.minSdk,
        size = primary?.size ?: size,
        iconHash = detail.iconHash,
        iconAdaptive = detail.iconAdaptive,
        categorySlug = detail.categorySlug.ifBlank { null },
        updatedAt = detail.updatedAt,
        sigSha256 = primary?.sigSha256 ?: sigSha256,
        sigMd5 = primary?.sigMd5 ?: sigMd5,
        stars = detail.stars,
        downloadTotal = detail.downloadTotal,
        installCount = detail.installCount,
        versionUpdatedAt = detail.versionUpdatedAt,
        listUpdatedAt = detail.listUpdatedAt,
        url = detail.url,
        sourceUrl = detail.sourceUrl,
        sourceKind = SourceKind.fromWire(detail.sourceKind),
        sourceName = detail.sourceName,
        storeUrl = detail.storeUrl,
        authorName = detail.authorName,
        authorUrl = detail.authorUrl,
        permissions = detail.permissions,
        excludedReason = detail.excludedReason,
        parentSlug = detail.parentSlug,
        categoryPath = detail.categoryPath.map { CategoryPath(it.slug, it.name) },
        addedAt = detail.addedAt.ifBlank { null },
        lastCheckedAt = detail.lastCheckedAt,
        detailsFetchedAt = fetchedAt
    )
}

fun DownloadDto.toEntity(appSlug: String): AppDownloadEntity = AppDownloadEntity(
    appSlug = appSlug,
    packageName = packageName,
    source = SourceKind.fromWire(source),
    apkUrl = apkUrl,
    archiveEntry = archiveEntry,
    versionCode = versionCode,
    versionName = versionName,
    size = size,
    sha256 = sha256,
    sigSha256 = sigSha256,
    sigMd5 = sigMd5,
    minSdk = minSdk,
    abi = abi,
    isPrimary = primary,
    sigKey = AppDownloadEntity.sigKeyOf(sigSha256, sigMd5, apkUrl, abi)
)

/** Depth-first flatten of the server category tree, parents before children. */
fun List<CategoryNodeDto>.toEntities(): List<CategoryEntity> {
    val out = mutableListOf<CategoryEntity>()
    var order = 0

    fun walk(nodes: List<CategoryNodeDto>, parent: String?) {
        nodes.forEach { node ->
            out += CategoryEntity(
                slug = node.slug,
                name = node.name,
                section = CategorySection.fromWire(node.section) ?: CategorySection.APPS,
                parentSlug = parent,
                appCount = node.appCount,
                sortOrder = order++
            )
            walk(node.children, node.slug)
        }
    }

    walk(this, null)
    return out
}
