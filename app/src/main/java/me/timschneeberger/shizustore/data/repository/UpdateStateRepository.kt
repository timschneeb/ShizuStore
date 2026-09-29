/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.CertFingerprint
import me.timschneeberger.shizustore.data.room.ShizuStoreDatabase
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.dao.IgnoredUpdateDao
import me.timschneeberger.shizustore.data.room.dao.InstalledDao
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.IgnoredUpdateEntity
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity

/**
 * Only a candidate whose signing set contains the installed fingerprint is offered, so a user is
 * never moved between signing keys; with no candidates it falls back to a summary version compare.
 */
@Singleton
class UpdateStateRepository @Inject constructor(
    private val database: ShizuStoreDatabase,
    private val appDao: AppDao,
    private val appDownloadDao: AppDownloadDao,
    private val installedDao: InstalledDao,
    private val ignoredUpdateDao: IgnoredUpdateDao
) {
    /**
     * One transaction keeps the cleared state invisible to observers so badges do not flicker; the
     * driver API is required because `withTransaction` needs a `SupportSQLiteOpenHelper`.
     */
    suspend fun recomputeAll() = database.useWriterConnection { transactor ->
        transactor.immediateTransaction {
            appDao.clearUpdateState()
            val installedByPackage = installedDao.getAll().associateBy { it.packageName }
            val ignoredByPackage = ignoredUpdateDao.getAll().associateBy { it.packageName }
            appDao.getAll().forEach { app ->
                val installed = resolveInstalled(app, installedByPackage)
                applyState(app.slug, installed, ignoredByPackage)
            }
        }
    }

    suspend fun recompute(packageName: String) = database.useWriterConnection { transactor ->
        transactor.immediateTransaction {
            val app = appDao.getByPackage(packageName)
                ?: appDao.getByDownloadPackage(packageName)
                ?: return@immediateTransaction
            val installedByPackage = installedDao.getAll().associateBy { it.packageName }
            val ignoredByPackage = ignoredUpdateDao.getAll().associateBy { it.packageName }
            applyState(app.slug, resolveInstalled(app, installedByPackage), ignoredByPackage)
        }
    }

    /** The version an update would offer for the installed app, or null when none is offered. */
    suspend fun offeredVersionCode(packageName: String): Long? =
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val app = appDao.getByPackage(packageName)
                    ?: appDao.getByDownloadPackage(packageName)
                    ?: return@immediateTransaction null
                val installedByPackage = installedDao.getAll().associateBy { it.packageName }
                val installed = resolveInstalled(app, installedByPackage)
                    ?: return@immediateTransaction null
                val candidates = appDownloadDao.forApp(app.slug)
                    .map { AppCandidate.from(it, app.packageName) }
                resolveOfferedVersion(app, selectMatch(installed, candidates), candidates)
            }
        }

    /**
     * A flavor installs under its own package, so the entry counts as installed when any of its
     * candidate packages is; the canonical package is only the fast path.
     */
    private suspend fun resolveInstalled(
        app: AppEntity,
        installedByPackage: Map<String, InstalledEntity>
    ): InstalledEntity? {
        app.packageName?.let { installedByPackage[it] }?.let { return it }
        return appDownloadDao.forApp(app.slug)
            .mapNotNull { it.packageName }
            .firstNotNullOfOrNull { installedByPackage[it] }
    }

    /**
     * Flavor builds usually share a signing key, so the package is what ties the
     * installed app to its own candidate; never offer a different flavor's APK.
     */
    private fun selectMatch(
        installed: InstalledEntity,
        candidates: List<AppCandidate>
    ): AppCandidate? {
        val installedFingerprint = CertFingerprint.of(installed.signer, installed.signerMd5)
        val matches = candidates.filter { it.matchesInstalled(installedFingerprint) }
        return matches.firstOrNull {
            it.packageName == installed.packageName &&
                it.supportsAbi(AppCandidate.deviceAbis)
        }
            ?: matches.firstOrNull { it.packageName == installed.packageName }
            ?: matches.firstOrNull { it.supportsAbi(AppCandidate.deviceAbis) }
            ?: matches.firstOrNull()
    }

    private fun resolveOfferedVersion(
        app: AppEntity,
        match: AppCandidate?,
        candidates: List<AppCandidate>
    ): Long? = when {
        match != null -> match.versionCode ?: app.versionCode
        candidates.isEmpty() -> app.versionCode
        else -> null
    }

    private suspend fun applyState(
        slug: String,
        installed: InstalledEntity?,
        ignoredByPackage: Map<String, IgnoredUpdateEntity>
    ) {
        if (installed == null) {
            appDao.setUpdateState(
                slug,
                null,
                updateAvailable = false,
                updateCandidateId = null,
                updateIgnored = false
            )
            return
        }

        val app = appDao.get(slug) ?: return
        val candidates = appDownloadDao.forApp(slug).map { AppCandidate.from(it, app.packageName) }
        val match = selectMatch(installed, candidates)

        val available: Boolean
        val candidateId: Long?
        when {
            match != null -> {
                available = match.isNewerThan(installed.versionCode)
                candidateId = match.id
            }
            candidates.isEmpty() -> {
                available = app.versionCode?.let { it > installed.versionCode } == true
                candidateId = null
            }
            else -> {
                available = false
                candidateId = null
            }
        }

        // The ignore row may be keyed by the canonical or the installed flavor package.
        val ignore = ignoredByPackage[app.packageName] ?: ignoredByPackage[installed.packageName]
        val offeredVersion = resolveOfferedVersion(app, match, candidates)
        val ignored = ignore != null &&
            offeredVersion != null &&
            (ignore.versionCode == null || ignore.versionCode == offeredVersion)
        // A version-scoped ignore has served its purpose once a newer build is offered.
        if (ignore?.versionCode != null &&
            offeredVersion != null &&
            offeredVersion > ignore.versionCode
        ) {
            ignoredUpdateDao.delete(ignore.packageName)
        }
        appDao.setUpdateState(slug, installed.versionCode, available, candidateId, ignored)
    }
}
