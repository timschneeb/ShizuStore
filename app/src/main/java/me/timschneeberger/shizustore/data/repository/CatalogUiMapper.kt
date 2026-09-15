/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.ShizuUrls
import me.timschneeberger.shizustore.data.api.SourceKind
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.data.model.AppSource
import me.timschneeberger.shizustore.data.model.CertFingerprint
import me.timschneeberger.shizustore.data.model.DetailedApp
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.util.ServerConfig

/**
 * Adapts the catalog Room entities into the presentation models the inherited
 * AuroraDroid UI consumes. M8 replaces this shim with the catalog models proper.
 */
@Singleton
class CatalogUiMapper @Inject constructor() {

    /** Prefers the server's friendly source label; falls back to the raw kind. */
    private fun displayRepoName(app: AppEntity): String {
        app.sourceName?.takeIf { it.isNotBlank() }?.let { return it }
        return if (app.availability == Availability.PLAY_REDIRECT) {
            "play"
        } else {
            app.sourceKind?.name?.lowercase() ?: "Shizu Store"
        }
    }

    /** Friendly labels for version rows, matching the server's app-level sourceName. */
    private fun SourceKind.displayName(): String = when (this) {
        SourceKind.GITHUB -> "GitHub"
        SourceKind.GITLAB -> "GitLab"
        SourceKind.CODEBERG -> "Codeberg"
        SourceKind.FDROID -> "F-Droid"
        SourceKind.IZZY -> "IzzyOnDroid"
        SourceKind.PLAY -> "Play Store"
        SourceKind.OTHER -> "Website"
    }

    fun toResolvedApp(app: AppEntity): ResolvedApp = ResolvedApp(
        packageName = app.packageName ?: app.slug,
        repoId = 0,
        repoName = displayRepoName(app),
        repoAddress = "",
        name = app.name,
        summary = app.description,
        authorName = null,
        iconUrl = ShizuUrls.icon(ServerConfig.baseUrl, app.iconHash),
        versionCode = app.versionCode ?: 0L,
        versionName = app.versionName.orEmpty(),
        signer = app.sigSha256,
        apkName = "",
        hash = "",
        size = app.size ?: 0L,
        minSdk = app.minSdk ?: 0,
        installedVersionCode = app.installedVersionCode,
        installedSigner = null,
        slug = app.slug,
        iconHash = app.iconHash,
        availability = app.availability,
        storeUrl = app.storeUrl,
        url = app.url,
        sourceUrl = app.sourceUrl,
        iconAdaptive = app.iconAdaptive,
        signerMd5 = app.sigMd5,
        updateAvailable = app.updateAvailable,
        updateCandidateId = app.updateCandidateId,
        hasPaid = app.hasPaid,
        hasIap = app.hasIap,
        stars = app.stars,
        installCount = app.installCount
    )

    fun toAppDetails(detailed: DetailedApp): AppDetails {
        val app = detailed.app
        val primary = detailed.primaryCandidate
        return AppDetails(
            packageName = app.packageName ?: app.slug,
            repoId = 0,
            repoName = displayRepoName(app),
            repoAddress = "",
            name = app.name,
            summary = app.description,
            description = app.description,
            iconUrl = ShizuUrls.icon(ServerConfig.baseUrl, app.iconHash),
            license = app.license.orEmpty(),
            authorName = app.authorName,
            authorEmail = null,
            authorPhone = null,
            authorWebSite = null,
            webSite = app.url,
            sourceCode = app.sourceUrl,
            issueTracker = null,
            changelog = null,
            translation = null,
            donate = emptyList(),
            liberapay = null,
            openCollective = null,
            bitcoin = null,
            litecoin = null,
            categories = listOfNotNull(
                app.categoryPath.lastOrNull()?.name?.takeIf { it.isNotBlank() }
                    ?: app.categorySlug
            ),
            antiFeatures = emptyList(),
            screenshots = emptyList(),
            added = 0L,
            lastUpdated = 0L,
            versionCode = app.versionCode ?: 0L,
            versionName = app.versionName.orEmpty(),
            signer = primary?.sigSha256 ?: app.sigSha256,
            size = primary?.size ?: 0L,
            minSdk = app.minSdk ?: 0,
            whatsNew = "",
            permissions = app.permissions.filterNot { it.endsWith(DYNAMIC_RECEIVER_SUFFIX) },
            installedVersionCode = app.installedVersionCode,
            installedSigner = null,
            slug = app.slug,
            iconHash = app.iconHash,
            availability = app.availability,
            storeUrl = app.storeUrl,
            url = app.url,
            sourceUrl = app.sourceUrl,
            authorUrl = app.authorUrl,
            iconAdaptive = app.iconAdaptive,
            hasPaid = app.hasPaid,
            hasIap = app.hasIap,
            stars = app.stars
        )
    }

    fun toSources(detailed: DetailedApp, installed: CertFingerprint?): List<AppSource> =
        detailed.candidates.map { candidate ->
            AppSource(
                app = candidateToResolvedApp(detailed.app, candidate, installed),
                added = 0L,
                releaseChannels = emptyList(),
                signerMatch = candidate.matchesInstalled(installed)
            )
        }

    private fun candidateToResolvedApp(
        app: AppEntity,
        candidate: AppCandidate,
        installed: CertFingerprint?
    ): ResolvedApp = ResolvedApp(
        packageName = app.packageName ?: app.slug,
        repoId = 0,
        // The candidate's own origin, not the app's: one app can ship both a forge
        // build and an F-Droid build signed by different keys.
        repoName = candidate.source?.displayName()
            ?: app.sourceName?.takeIf { it.isNotBlank() }
            ?: "Shizu Store",
        repoAddress = "",
        name = app.name,
        summary = app.description,
        authorName = null,
        iconUrl = ShizuUrls.icon(ServerConfig.baseUrl, app.iconHash),
        versionCode = candidate.versionCode ?: 0L,
        versionName = candidate.versionName.orEmpty(),
        signer = candidate.sigSha256,
        apkName = candidate.fileName(),
        hash = candidate.sha256.orEmpty(),
        size = candidate.size ?: 0L,
        minSdk = candidate.minSdk ?: app.minSdk ?: 0,
        installedVersionCode = app.installedVersionCode,
        installedSigner = installed?.takeIf { it.isKnown }?.sha256?.joinToString(" "),
        slug = app.slug,
        iconHash = app.iconHash,
        availability = app.availability,
        storeUrl = app.storeUrl,
        url = app.url,
        sourceUrl = app.sourceUrl,
        iconAdaptive = app.iconAdaptive,
        signerMd5 = candidate.sigMd5,
        updateAvailable = app.updateAvailable,
        updateCandidateId = app.updateCandidateId,
        candidateId = candidate.id
    )

    private fun AppCandidate.fileName(): String =
        archiveEntry?.takeIf { it.isNotBlank() }?.substringAfterLast('/')
            ?: apkUrl.substringBefore('?').substringAfterLast('/')

    fun fromEntities(downloads: List<AppDownloadEntity>): List<AppCandidate> =
        downloads.map { AppCandidate.from(it, null) }
}

/** AndroidX's ContextCompat declares this per-app permission; it is not a user grant. */
private const val DYNAMIC_RECEIVER_SUFFIX = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
