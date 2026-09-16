/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import android.os.Build
import androidx.room.Room
import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.data.api.BaseUrlProvider
import me.timschneeberger.shizustore.data.api.OkHttpShizuApi
import me.timschneeberger.shizustore.data.api.RequestThrottle
import me.timschneeberger.shizustore.data.api.ShizuJson
import me.timschneeberger.shizustore.data.room.ShizuStoreDatabase
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class DetailedAppRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var db: ShizuStoreDatabase
    private lateinit var repository: DetailedAppRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ShizuStoreDatabase::class.java
        ).allowMainThreadQueries().build()

        val api = OkHttpShizuApi(
            client = OkHttpClient(),
            json = ShizuJson,
            baseUrlProvider = BaseUrlProvider({ server.url("/").toString() }, defaultValue = ""),
            throttle = RequestThrottle(minSpacingMillis = 0)
        )
        repository = DetailedAppRepository(
            api,
            db.appDao(),
            db.appDownloadDao(),
            UpdateStateRepository(db, db.appDao(), db.appDownloadDao(), db.installedDao())
        )
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    @Test
    fun fetchPersistsDetailAndReplacesCandidates() = runTest {
        db.appDao().upsert(AppEntity(slug = "alpha", name = "Alpha"))
        server.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(DETAIL)
        )

        val result = repository.fetchAndPersist("alpha") as DetailedAppResult.Success

        assertEquals(2, result.downloads.size)
        assertEquals("https://example.com/alpha", db.appDao().get("alpha")!!.url)
        assertTrue(db.appDao().get("alpha")!!.iconAdaptive)

        val candidates = db.appDownloadDao().forApp("alpha")
        assertEquals(2, candidates.size)
        assertEquals("github", candidates.first().source!!.wire)
        assertTrue(candidates.first().isPrimary)
        assertEquals("https://example.com/alpha.apk", candidates.first().apkUrl)
        assertEquals("# Alpha readme", repository.fullDescription("alpha"))
        assertEquals("## 1.0\n- First release", repository.changelog("alpha"))
        assertEquals(
            "https://f-droid.org/repo/example/en-US/phoneScreenshots/00.png",
            repository.screenshots("alpha")?.single()
        )
    }

    @Test
    fun notFoundIsReported() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(DetailedAppResult.NotFound, repository.fetchAndPersist("nope"))
    }

    private companion object {
        val DETAIL = """
            {
              "slug": "alpha", "name": "Alpha", "description": "first",
              "listing": "main", "type": "app", "availability": "direct_apk",
              "packageName": "com.alpha", "versionCode": 5, "versionName": "1.0",
              "iconHash": "aa", "iconAdaptive": true, "categorySlug": "tools",
              "updatedAt": "2026-05-01T00:00:00+00:00",
              "url": "https://example.com/alpha", "sourceUrl": null, "sourceKind": "github",
              "downloads": [
                {
                  "source": "github", "apkUrl": "https://example.com/alpha.apk",
                  "versionCode": 5, "versionName": "1.0", "size": 123,
                  "sha256": "deadbeef", "sigSha256": "AAA BBB", "sigMd5": "ccc",
                  "minSdk": 24, "primary": true
                },
                {
                  "source": "fdroid", "apkUrl": "https://f-droid.org/alpha.apk",
                  "versionCode": 5, "primary": false
                }
              ],
              "storeUrl": null, "excludedReason": null,
              "categoryPath": [ { "slug": "apps", "name": "Apps" } ],
              "parentSlug": null, "addedAt": "2026-01-01T00:00:00+00:00",
              "lastCheckedAt": "2026-05-01T00:00:00+00:00",
              "fullDescription": "# Alpha readme",
              "changelog": "## 1.0\n- First release",
              "screenshots": ["https://f-droid.org/repo/example/en-US/phoneScreenshots/00.png"]
            }
        """.trimIndent()
    }
}
