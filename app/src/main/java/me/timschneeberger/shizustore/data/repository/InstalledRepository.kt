/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.data.room.dao.InstalledDao
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity
import me.timschneeberger.shizustore.util.CertUtil

@Singleton
open class InstalledRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val installedDao: InstalledDao,
    private val updateStateRepository: UpdateStateRepository
) {
    fun observeAll(): Flow<List<InstalledEntity>> = installedDao.observeAll()

    fun observe(packageName: String): Flow<InstalledEntity?> =
        installedDao.observeByPackage(packageName)

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        val packages = context.packageManager
            .getInstalledPackages(0)
            .mapNotNull { info -> toEntity(info.packageName) }

        installedDao.replaceAll(packages)
        updateStateRepository.recomputeAll()
    }

    open suspend fun onPackageAdded(packageName: String) = withContext(Dispatchers.IO) {
        toEntity(packageName)?.let { installedDao.upsert(it) }
        updateStateRepository.recompute(packageName)
    }

    open suspend fun onPackageRemoved(packageName: String) = withContext(Dispatchers.IO) {
        installedDao.deleteByPackage(packageName)
        updateStateRepository.recompute(packageName)
    }

    private fun toEntity(packageName: String): InstalledEntity? = runCatching {
        val info = context.packageManager.getPackageInfo(packageName, 0)
        val fingerprints = CertUtil.getSigningFingerprints(context, packageName)
        InstalledEntity(
            packageName = packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            versionName = info.versionName.orEmpty(),
            signer = fingerprints.flatMap {
                it.sha256
            }.distinct().joinToString(" ").ifBlank { null },
            signerMd5 = fingerprints.flatMap {
                it.md5
            }.distinct().joinToString(" ").ifBlank { null }
        )
    }.getOrNull()
}
