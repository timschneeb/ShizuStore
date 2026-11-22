/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import me.timschneeberger.shizustore.data.model.AppCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFromCatalogTest {

    @Test
    fun flavorCandidateInstallsUnderItsOwnPackage() {
        val download = Download.fromCatalog(app = app(), candidate = candidate("com.app.play"))
        assertEquals("com.app.play", download.packageName)
    }

    @Test
    fun legacyCandidateFallsBackToTheAppPackage() {
        val download = Download.fromCatalog(app = app(), candidate = candidate(null))
        assertEquals("com.app", download.packageName)
    }

    @Test
    fun completedRowBelongsToOnlyTheListingThatStagedIt() {
        val candidate = candidate("com.app")
        val download = Download.fromCatalog(app(), candidate)

        assertTrue(download.belongsTo("app", candidate))
        assertFalse(download.belongsTo("fork", candidate))
    }

    @Test
    fun legacyRowWithoutProvenanceRequiresTheSameApk() {
        val original = candidate("com.app", sha256 = "abc")
        val legacy = Download.fromCatalog(app(), original).copy(appSlug = null)

        assertTrue(legacy.belongsTo("other", original))
        assertFalse(legacy.belongsTo("other", original.copy(apkUrl = "https://example/fork.apk")))
        assertFalse(legacy.belongsTo("other", original.copy(sha256 = "def")))
    }

    private fun app(): AppEntity = AppEntity(slug = "app", name = "App", packageName = "com.app")

    private fun candidate(
        packageName: String?,
        url: String = "https://example/app.apk",
        sha256: String = "abc"
    ): AppCandidate = AppCandidate(
        id = 1,
        appSlug = "app",
        packageName = packageName,
        versionCode = 1,
        versionName = "1.0",
        source = null,
        apkUrl = url,
        archiveEntry = null,
        size = 10,
        sha256 = sha256,
        sigSha256 = null,
        sigMd5 = null,
        minSdk = 24,
        abi = null,
        isPrimary = true
    )
}
