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
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.util.ServerConfig

@Singleton
class CatalogUiMapper @Inject constructor() {

    private fun displayRepoName(app: AppEntity): String {
        app.sourceName?.takeIf { it.isNotBlank() }?.let { return it }
        return if (app.availability == Availability.PLAY_REDIRECT) {
            "play"
        } else {
            app.sourceKind?.name?.lowercase() ?: "Shizu Store"
        }
    }

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
            permissions = app.permissions.filterNot { it.endsWith(DYNAMIC_RECEIVER_SUFFIX) },
            installedVersionCode = app.installedVersionCode,
            installedSigner = null,
            slug = app.slug,
            iconHash = app.iconHash,
            availability = app.availability,
            storeUrl = app.storeUrl,
            url = app.url,
            sourceUrl = app.sourceUrl,
            sourceKind = app.sourceKind,
            authorUrl = app.authorUrl,
            iconAdaptive = app.iconAdaptive,
            hasPaid = app.hasPaid,
            hasIap = app.hasIap,
            stars = app.stars
        )
    }

    fun toSources(
        detailed: DetailedApp,
        installed: CertFingerprint?,
        installedPackage: String? = null
    ): List<AppSource> {
        val app = detailed.app
        // A per-ABI release ships one download per architecture and each may carry a
        // distinct version code, so key on the version name: those are the same
        // source, not several. Collapse them to one row per (package, version label,
        // signing identity), keeping the build this device can run.
        return detailed.candidates
            .groupBy { candidate ->
                Triple(
                    candidate.packageName ?: app.packageName,
                    candidate.versionName ?: candidate.versionCode,
                    candidate.sigSha256 ?: candidate.sigMd5
                )
            }
            .map { (_, candidates) ->
                val candidate =
                    candidates.firstOrNull {
                        it.isPrimary && it.supportsAbi(AppCandidate.deviceAbis)
                    }
                        ?: candidates.firstOrNull { it.supportsAbi(AppCandidate.deviceAbis) }
                        ?: candidates.firstOrNull { it.isPrimary }
                        ?: candidates.first()
                // Flavors usually share a signing key, so only the package ties the
                // installed app to its own source; the fingerprint alone cannot.
                val sourcePackage = candidate.packageName ?: app.packageName
                val installedPackageMatch =
                    installedPackage != null && sourcePackage == installedPackage
                AppSource(
                    app = candidateToResolvedApp(app, candidate, installed, installedPackageMatch),
                    added = 0L,
                    releaseChannels = emptyList(),
                    nativeCode = candidate.abi?.let { listOf(it) } ?: emptyList(),
                    signerMatch = candidate.matchesInstalled(installed),
                    installedPackageMatch = installedPackageMatch
                )
            }
    }

    private fun candidateToResolvedApp(
        app: AppEntity,
        candidate: AppCandidate,
        installed: CertFingerprint?,
        installedPackageMatch: Boolean
    ): ResolvedApp = ResolvedApp(
        // The candidate's own package wins so a flavor source shows the package
        // it actually installs as.
        packageName = candidate.packageName ?: app.packageName ?: app.slug,
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
        // Only the installed flavor's row carries the installed version, so the
        // "Installed" chip and update hint never leak onto a sibling flavor.
        installedVersionCode = if (installedPackageMatch) app.installedVersionCode else null,
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
}

/** AndroidX's ContextCompat declares this per-app permission; it is not a user grant. */
private const val DYNAMIC_RECEIVER_SUFFIX = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
