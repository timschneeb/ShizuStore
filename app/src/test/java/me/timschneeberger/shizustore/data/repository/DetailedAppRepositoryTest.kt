/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.ApiTestBase
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DetailedAppRepositoryTest : ApiTestBase() {

    private lateinit var repository: DetailedAppRepository

    @Before
    fun setUp() {
        repository = DetailedAppRepository(
            api(),
            db.appDao(),
            db.appDownloadDao(),
            updateStateRepository()
        )
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
