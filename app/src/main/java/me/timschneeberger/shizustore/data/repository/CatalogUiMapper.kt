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
import me.timschneeberger.shizustore.util.CommonUtil
import me.timschneeberger.shizustore.util.ServerConfig

@Singleton
class CatalogUiMapper @Inject constructor() {

    private fun displayRepoName(app: AppEntity): String {
        app.sourceName?.takeIf { it.isNotBlank() }?.let { return it }
        return if (app.availability == Availability.PLAY_REDIRECT) {
            "play"
        } else {
            app.sourceKind?.name?.lowercase() ?: "ShizuStore"
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
        repoName = displayRepoName(app),
        name = app.name,
        summary = app.description,
        iconUrl = ShizuUrls.icon(ServerConfig.baseUrl, app.iconHash),
        versionCode = app.versionCode ?: 0L,
        versionName = app.versionName.orEmpty(),
        signer = app.sigSha256,
        size = app.size ?: 0L,
        minSdk = app.minSdk ?: 0,
        installedVersionCode = app.installedVersionCode,
        installedSigner = null,
        slug = app.slug,
        availability = app.availability,
        iconAdaptive = app.iconAdaptive,
        updateAvailable = app.updateAvailable,
        hasPaid = app.hasPaid,
        hasIap = app.hasIap,
        stars = app.stars,
        installCount = app.installCount,
        listUpdatedAtMillis = CommonUtil.parseIsoUtcMillis(app.listUpdatedAt),
        versionUpdatedAtMillis = CommonUtil.parseIsoUtcMillis(app.versionUpdatedAt)
    )

    fun toAppDetails(detailed: DetailedApp): AppDetails {
        val app = detailed.app
        val primary = detailed.primaryCandidate
        return AppDetails(
            packageName = app.packageName ?: app.slug,
            repoName = displayRepoName(app),
            name = app.name,
            description = app.description,
            iconUrl = ShizuUrls.icon(ServerConfig.baseUrl, app.iconHash),
            license = app.license.orEmpty(),
            authorName = app.authorName,
            changelog = null,
            categories = listOfNotNull(
                app.categoryPath.lastOrNull()?.name?.takeIf { it.isNotBlank() }
                    ?: app.categorySlug
            ),
            screenshots = emptyList(),
            lastUpdated = 0L,
            versionName = app.versionName.orEmpty(),
            size = primary?.size ?: 0L,
            minSdk = app.minSdk ?: 0,
            permissions = app.permissions.filterNot { it.endsWith(DYNAMIC_RECEIVER_SUFFIX) },
            installedVersionCode = app.installedVersionCode,
            availability = app.availability,
            storeUrl = app.storeUrl,
            url = app.url,
            sourceUrl = app.sourceUrl,
            sourceKind = app.sourceKind,
            authorUrl = app.authorUrl,
            hasPaid = app.hasPaid,
            hasIap = app.hasIap,
            stars = app.stars,
            listing = app.listing
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
                    nativeCode = candidate.abiTokens,
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
        // The candidate's own origin, not the app's: one app can ship both a forge
        // build and an F-Droid build signed by different keys.
        repoName = candidate.source?.displayName()
            ?: app.sourceName?.takeIf { it.isNotBlank() }
            ?: "ShizuStore",
        name = app.name,
        summary = app.description,
        iconUrl = ShizuUrls.icon(ServerConfig.baseUrl, app.iconHash),
        versionCode = candidate.versionCode ?: 0L,
        versionName = candidate.versionName.orEmpty(),
        signer = candidate.sigSha256,
        size = candidate.size ?: 0L,
        minSdk = candidate.minSdk ?: app.minSdk ?: 0,
        // Only the installed flavor's row carries the installed version, so the
        // "Installed" chip and update hint never leak onto a sibling flavor.
        installedVersionCode = if (installedPackageMatch) app.installedVersionCode else null,
        installedSigner = installed?.takeIf { it.isKnown }?.sha256?.joinToString(" "),
        slug = app.slug,
        availability = app.availability,
        iconAdaptive = app.iconAdaptive,
        updateAvailable = app.updateAvailable,
        candidateId = candidate.id
    )
}

/** AndroidX's ContextCompat declares this per-app permission; it is not a user grant. */
private const val DYNAMIC_RECEIVER_SUFFIX = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
