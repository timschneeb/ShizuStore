/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import android.os.Build
import androidx.room.Room
import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.data.room.ShizuStoreDatabase
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class UpdateStateRepositoryTest {

    private lateinit var db: ShizuStoreDatabase
    private lateinit var repository: UpdateStateRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ShizuStoreDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = UpdateStateRepository(db, db.appDao(), db.appDownloadDao(), db.installedDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun candidateMatchingInstalledKeyMarksUpdateAndPinsCandidate() = runTest {
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 7)
        )
        db.appDownloadDao().upsertAll(
            listOf(
                download(
                    slug = "app",
                    sigSha256 = "aaaaaaaa",
                    versionCode = 5,
                    primary = true,
                    url = "a"
                ),
                download(
                    slug = "app",
                    sigSha256 = "bbbbbbbb",
                    versionCode = 8,
                    primary = false,
                    url = "b"
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

        repository.recomputeAll()

        val app = db.appDao().get("app")!!
        assertTrue(app.updateAvailable)
        assertEquals(4L, app.installedVersionCode)
        val matching = db.appDownloadDao().forApp("app").first { it.sigSha256 == "aaaaaaaa" }
        assertEquals(matching.id, app.updateCandidateId)
    }

    @Test
    fun differentSigningKeyNeverOffersNewerCandidate() = runTest {
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 7)
        )
        db.appDownloadDao().upsertAll(
            listOf(
                download(
                    slug = "app",
                    sigSha256 = "bbbbbbbb",
                    versionCode = 8,
                    primary = false,
                    url = "b"
                )
            )
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.app",
                versionCode = 4,
                versionName = "0.9",
                signer = "cccccccc"
            )
        )

        repository.recomputeAll()

        val app = db.appDao().get("app")!!
        assertFalse(app.updateAvailable)
        assertEquals(null, app.updateCandidateId)
    }

    @Test
    fun md5OnlyMatchIsAccepted() = runTest {
        val md5 = "d41d8cd98f00b204e9800998ecf8427e"
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 9)
        )
        db.appDownloadDao().upsertAll(
            listOf(download(slug = "app", sigMd5 = "$md5 deadbeef", versionCode = 6, url = "m"))
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.app",
                versionCode = 5,
                versionName = "1.0",
                signer = null,
                signerMd5 = md5
            )
        )

        repository.recomputeAll()

        assertTrue(db.appDao().get("app")!!.updateAvailable)
    }

    @Test
    fun summaryFallbackAppliesWhenNoCandidatesFetched() = runTest {
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 7)
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.app",
                versionCode = 4,
                versionName = "0.9",
                signer = "aaaa"
            )
        )

        repository.recomputeAll()

        val app = db.appDao().get("app")!!
        assertTrue(app.updateAvailable)
        assertEquals(null, app.updateCandidateId)
    }

    @Test
    fun recomputeSinglePackageClearsStateWhenUninstalled() = runTest {
        db.appDao().upsert(
            AppEntity(
                slug = "app",
                name = "App",
                packageName = "com.app",
                installedVersionCode = 4,
                updateAvailable = true,
                updateCandidateId = 3
            )
        )

        repository.recompute("com.app")

        val app = db.appDao().get("app")!!
        assertEquals(null, app.installedVersionCode)
        assertFalse(app.updateAvailable)
        assertEquals(null, app.updateCandidateId)
    }

    @Test
    fun installedFlavorPackageMarksEntryInstalledAndOffersUpdate() = runTest {
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 8)
        )
        db.appDownloadDao().upsertAll(
            listOf(
                download(
                    slug = "app",
                    packageName = "com.app",
                    sigSha256 = "aaaaaaaa",
                    versionCode = 8,
                    primary = true,
                    url = "base"
                ),
                download(
                    slug = "app",
                    packageName = "com.app.play",
                    sigSha256 = "aaaaaaaa",
                    versionCode = 8,
                    primary = false,
                    url = "play"
                )
            )
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.app.play",
                versionCode = 4,
                versionName = "0.9",
                signer = "aaaaaaaa"
            )
        )

        repository.recomputeAll()

        val app = db.appDao().get("app")!!
        assertTrue(app.updateAvailable)
        assertEquals(4L, app.installedVersionCode)
        val play = db.appDownloadDao().forApp("app").first { it.packageName == "com.app.play" }
        assertEquals(play.id, app.updateCandidateId)
    }

    @Test
    fun recomputeByFlavorPackageResolvesTheOwningEntry() = runTest {
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 8)
        )
        db.appDownloadDao().upsertAll(
            listOf(
                download(
                    slug = "app",
                    packageName = "com.app.play",
                    sigSha256 = "aaaaaaaa",
                    versionCode = 8,
                    primary = true,
                    url = "play"
                )
            )
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.app.play",
                versionCode = 4,
                versionName = "0.9",
                signer = "aaaaaaaa"
            )
        )

        repository.recompute("com.app.play")

        val app = db.appDao().get("app")!!
        assertEquals(4L, app.installedVersionCode)
        assertTrue(app.updateAvailable)
    }

    @Test
    fun recomputeByCanonicalPackageResolvesInstalledFlavor() = runTest {
        db.appDao().upsert(
            AppEntity(slug = "app", name = "App", packageName = "com.app", versionCode = 8)
        )
        db.appDownloadDao().upsertAll(
            listOf(
                download(
                    slug = "app",
                    packageName = "com.app",
                    sigSha256 = "aaaaaaaa",
                    versionCode = 8,
                    primary = true,
                    url = "base"
                ),
                download(
                    slug = "app",
                    packageName = "com.app.play",
                    sigSha256 = "aaaaaaaa",
                    versionCode = 8,
                    url = "play"
                )
            )
        )
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.app.play",
                versionCode = 4,
                versionName = "0.9",
                signer = "aaaaaaaa"
            )
        )

        repository.recompute("com.app")

        val app = db.appDao().get("app")!!
        val play = db.appDownloadDao().forApp("app").first { it.packageName == "com.app.play" }
        assertEquals(4L, app.installedVersionCode)
        assertTrue(app.updateAvailable)
        assertEquals(play.id, app.updateCandidateId)
    }

    private fun download(
        slug: String,
        packageName: String? = null,
        sigSha256: String? = null,
        sigMd5: String? = null,
        versionCode: Long? = 1,
        primary: Boolean = false,
        url: String
    ): AppDownloadEntity = AppDownloadEntity(
        appSlug = slug,
        packageName = packageName,
        apkUrl = "https://example/$url.apk",
        versionCode = versionCode,
        sigSha256 = sigSha256,
        sigMd5 = sigMd5,
        isPrimary = primary,
        sigKey = AppDownloadEntity.sigKeyOf(sigSha256, sigMd5, "https://example/$url.apk")
    )
}
