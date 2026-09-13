/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.CertFingerprint
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.dao.InstalledDao
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity

/**
 * Recomputes the denormalized update columns on catalog apps. Matching is candidate level:
 * only a candidate whose signing set contains the installed fingerprint can be offered, so a
 * user is never moved between signing keys. When a detail was never fetched (no candidates),
 * it falls back to a summary version comparison without pinning a candidate.
 */
@Singleton
class UpdateStateRepository @Inject constructor(
    private val appDao: AppDao,
    private val appDownloadDao: AppDownloadDao,
    private val installedDao: InstalledDao
) {
    suspend fun recomputeAll() {
        appDao.clearUpdateState()
        val installedByPackage = installedDao.getAll().associateBy { it.packageName }
        appDao.getAll().forEach { app ->
            val installed = app.packageName?.let { installedByPackage[it] }
            applyState(app.slug, installed)
        }
    }

    suspend fun recompute(packageName: String) {
        val app = appDao.getByPackage(packageName) ?: return
        applyState(app.slug, installedDao.getByPackage(packageName))
    }

    private suspend fun applyState(slug: String, installed: InstalledEntity?) {
        if (installed == null) {
            appDao.setUpdateState(slug, null, updateAvailable = false, updateCandidateId = null)
            return
        }

        val app = appDao.get(slug) ?: return
        val installedFingerprint = CertFingerprint.of(installed.signer, installed.signerMd5)
        val candidates = appDownloadDao.forApp(slug).map { AppCandidate.from(it, app.packageName) }
        val match = candidates.firstOrNull { it.matchesInstalled(installedFingerprint) }

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
        appDao.setUpdateState(slug, installed.versionCode, available, candidateId)
    }
}
