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
import org.junit.Test

class AppRepositoryDuplicatePackageTest : RobolectricTestBase() {

    @Test
    fun exactSlugWinsOverPackageLookup() = runTest {
        db.appDao().upsert(app("fork-a"))
        db.appDao().upsert(app("fork-b"))

        assertEquals("fork-b", appRepository().findByKey("fork-b")?.slug)
        assertEquals("fork-a", appRepository().findByKey("com.shared")?.slug)
    }

    @Test
    fun packageLookupUsesInstalledSigningKey() = runTest {
        db.appDao().upsert(app("fork-a"))
        db.appDao().upsert(app("fork-b"))
        db.appDownloadDao().upsertAll(
            listOf(download("fork-a", "aaaaaaaa"), download("fork-b", "bbbbbbbb"))
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.shared",
                versionCode = 10,
                versionName = "1.0",
                signer = "bbbbbbbb"
            )
        )

        assertEquals("fork-b", appRepository().resolveByPackage("com.shared")?.slug)
    }

    @Test
    fun packageLookupFallsBackToCanonicalListing() = runTest {
        db.appDao().upsert(app("fork-a"))
        db.appDao().upsert(app("fork-b"))
        db.appDownloadDao().upsertAll(
            listOf(download("fork-a", "aaaaaaaa"), download("fork-b", "bbbbbbbb"))
        )

        assertEquals("fork-a", appRepository().resolveByPackage("com.shared")?.slug)
    }

    private fun app(slug: String) = AppEntity(
        slug = slug,
        name = slug,
        packageName = "com.shared"
    )

    private fun download(slug: String, sig: String) = AppDownloadEntity(
        appSlug = slug,
        packageName = "com.shared",
        apkUrl = "https://example/$slug.apk",
        sigSha256 = sig,
        isPrimary = true,
        sigKey = AppDownloadEntity.sigKeyOf(sig, null, "https://example/$slug.apk")
    )
}
