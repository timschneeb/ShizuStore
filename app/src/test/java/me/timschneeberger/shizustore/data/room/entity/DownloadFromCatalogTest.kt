/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import me.timschneeberger.shizustore.data.model.AppCandidate
import org.junit.Assert.assertEquals
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

    private fun app(): AppEntity = AppEntity(slug = "app", name = "App", packageName = "com.app")

    private fun candidate(packageName: String?): AppCandidate = AppCandidate(
        id = 1,
        appSlug = "app",
        packageName = packageName,
        versionCode = 1,
        versionName = "1.0",
        source = null,
        apkUrl = "https://example/app.apk",
        archiveEntry = null,
        size = 10,
        sha256 = null,
        sigSha256 = null,
        sigMd5 = null,
        minSdk = 24,
        abi = null,
        isPrimary = true
    )
}
