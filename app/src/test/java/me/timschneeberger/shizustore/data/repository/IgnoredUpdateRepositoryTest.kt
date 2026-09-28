/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.RobolectricTestBase
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IgnoredUpdateRepositoryTest : RobolectricTestBase() {

    @Test
    fun ignoreVersionStoresTheOfferedVersionAndSuppresses() = runTest {
        seedInstalledApp()

        ignoredUpdateRepository().ignoreVersion("com.app")

        val row = db.ignoredUpdateDao().getAll().single()
        assertEquals("com.app", row.packageName)
        assertEquals(8L, row.versionCode)
        assertTrue(db.appDao().get("app")!!.updateIgnored)
    }

    @Test
    fun ignoreAllPersistsUntilStopped() = runTest {
        seedInstalledApp()
        val repository = ignoredUpdateRepository()

        repository.ignoreAll("com.app")

        assertTrue(db.appDao().get("app")!!.updateIgnored)
        assertNull(db.ignoredUpdateDao().getAll().single().versionCode)

        repository.stopIgnoring("com.app")

        assertFalse(db.appDao().get("app")!!.updateIgnored)
        assertTrue(db.ignoredUpdateDao().getAll().isEmpty())
    }

    private suspend fun seedInstalledApp() {
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 8)
        )
        db.appDownloadDao().upsertAll(
            listOf(
                AppDownloadEntity(
                    appSlug = "app",
                    packageName = "com.app",
                    apkUrl = "https://example/app.apk",
                    versionCode = 8,
                    sigSha256 = "aaaaaaaa",
                    isPrimary = true,
                    sigKey = AppDownloadEntity.sigKeyOf(
                        "aaaaaaaa",
                        null,
                        "https://example/app.apk"
                    )
                )
            )
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.app",
                versionCode = 4,
                versionName = "0.9",
                signer = "aaaaaaaa"
            )
        )
        updateStateRepository().recomputeAll()
    }
}
