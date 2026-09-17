/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.sync

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.ApiTestBase
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.helper.SyncStatusStore
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity
import me.timschneeberger.shizustore.data.room.entity.SyncStateEntity
import me.timschneeberger.shizustore.util.Preferences
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class CatalogSyncerTest : ApiTestBase() {

    private lateinit var syncer: CatalogSyncer

    @Before
    fun setUp() {
        syncer = CatalogSyncer(
            context = RuntimeEnvironment.getApplication(),
            api = api(),
            appDao = db.appDao(),
            categoryDao = db.categoryDao(),
            syncStateDao = db.syncStateDao(),
            updateStateRepository = updateStateRepository(),
            syncStatus = SyncStatusStore()
        )
    }

    @Test
    fun bootstrapPagesCatalogAndAdvancesCursor() = runTest {
        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES)
        )

        val outcome = syncer.sync()

        assertTrue(outcome is CatalogSyncOutcome.Success)
        assertEquals(2, db.appDao().count())
        assertEquals("Alpha", db.appDao().get("alpha")!!.name)
        assertEquals("com.alpha", db.appDao().get("alpha")!!.packageName)
        assertEquals(GENERATED_AT, db.syncStateDao().get()!!.cursor)
        assertEquals(listOf("tools"), db.categoryDao().observeAll().first().map { it.slug })
    }

    @Test
    fun incrementalAppliesAddedUpdatedAndRemoved() = runTest {
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR, categoriesEtag = "etag-1"))
        db.appDao().upsert(AppEntity(slug = "alpha", name = "Alpha"))
        db.appDao().upsert(AppEntity(slug = "gone", name = "Gone"))
        db.appDownloadDao().upsertAll(
            listOf(
                AppDownloadEntity(appSlug = "gone", apkUrl = "https://x/gone.apk", sigKey = "k")
            )
        )

        server.dispatcher = routes(
            "/v1/changes" to json(CHANGES),
            "/v1/meta" to json(META),
            "/v1/categories" to MockResponse().setResponseCode(304)
        )

        val outcome = syncer.sync() as CatalogSyncOutcome.Success

        assertEquals(1, outcome.added)
        assertEquals(1, outcome.updated)
        assertEquals(1, outcome.removed)
        assertNotNull(db.appDao().get("beta"))
        assertNull(db.appDao().get("gone"))
        assertEquals(0, db.appDownloadDao().count())
        assertEquals(GENERATED_AT, db.syncStateDao().get()!!.cursor)
        assertEquals("etag-1", db.syncStateDao().get()!!.categoriesEtag)
    }

    @Test
    fun syncPersistsPopularityFlagFromMeta() = runTest {
        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META_FLAGGED),
            "/v1/categories" to json(CATEGORIES)
        )

        val outcome = syncer.sync()

        assertTrue(outcome is CatalogSyncOutcome.Success)
        assertTrue(db.syncStateDao().get()!!.useInstallCountsForPopularity)
    }

    @Test
    fun incrementalAppliesInstallCountDeltasWithoutTouchingRows() = runTest {
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR))
        db.appDao().upsert(
            AppEntity(slug = "alpha", name = "Alpha", installCount = 2)
        )

        server.dispatcher = routes(
            "/v1/changes" to json(CHANGES_WITH_INSTALLS),
            "/v1/meta" to json(META),
            "/v1/categories" to MockResponse().setResponseCode(304)
        )

        val outcome = syncer.sync() as CatalogSyncOutcome.Success

        assertEquals(0, outcome.added)
        assertEquals(0, outcome.updated)
        assertEquals(0, outcome.removed)
        val alpha = db.appDao().get("alpha")!!
        assertEquals(9, alpha.installCount)
        assertEquals("Alpha", alpha.name)
        assertNull(db.appDao().get("ghost"))
    }

    @Test
    fun clearCatalogWipesCacheSoNextSyncBootstraps() = runTest {
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR))
        db.appDao().upsert(AppEntity(slug = "gone", name = "Gone"))
        db.appDownloadDao().upsertAll(
            listOf(
                AppDownloadEntity(appSlug = "gone", apkUrl = "https://x/gone.apk", sigKey = "k")
            )
        )
        db.categoryDao().upsertAll(listOf(CategoryEntity(slug = "stale", name = "Stale")))

        syncer.clearCatalog()

        assertEquals(0, db.appDao().count())
        assertEquals(0, db.appDownloadDao().count())
        assertEquals(0, db.categoryDao().observeAll().first().size)
        assertNull(db.syncStateDao().get())

        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES)
        )

        val outcome = syncer.sync()

        assertTrue(outcome is CatalogSyncOutcome.Success)
        assertEquals(2, db.appDao().count())
        assertNull(db.appDao().get("gone"))
        assertEquals(listOf("tools"), db.categoryDao().observeAll().first().map { it.slug })
        assertEquals(GENERATED_AT, db.syncStateDao().get()!!.cursor)
    }

    @Test
    fun rateLimitedResponseFailsWithoutAdvancingCursor() = runTest {
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR))
        server.dispatcher = routes(
            "/v1/changes" to MockResponse().setResponseCode(429).addHeader("Retry-After", "7")
        )

        val outcome = syncer.sync()

        assertEquals(
            CatalogSyncFailure.RATE_LIMITED,
            (outcome as CatalogSyncOutcome.Failed).failure
        )
        assertEquals(OLD_CURSOR, db.syncStateDao().get()!!.cursor)
    }

    @Test
    fun updateStateMarksNewerInstalledApps() = runTest {
        db.installedDao().upsert(
            InstalledEntity(
                packageName = "com.alpha",
                versionCode = 4,
                versionName = "0.9",
                signer = "aaa"
            )
        )
        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES)
        )

        syncer.sync()

        val alpha = db.appDao().get("alpha")!!
        assertTrue(alpha.updateAvailable)
        assertEquals(4L, alpha.installedVersionCode)
        assertEquals(false, db.appDao().get("beta")!!.updateAvailable)
    }

    @Test
    fun bootstrapAsksForMainListingByDefault() = runTest {
        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES)
        )

        syncer.sync()

        assertEquals(
            "/v1/apps?page=1&pageSize=200&sort=name&order=asc&listing=main",
            server.takeRequest().path
        )
    }

    @Test
    fun bootstrapAsksForClosedSourceWhenEnabled() = runTest {
        Preferences.putBoolean(
            RuntimeEnvironment.getApplication(),
            Preferences.PREFERENCE_SHOW_CLOSED_SOURCE,
            true
        )
        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES)
        )

        syncer.sync()

        assertEquals(
            "/v1/apps?page=1&pageSize=200&sort=name&order=asc&listing=main%2Cclosed_source",
            server.takeRequest().path
        )
    }

    @Test
    fun disablingClosedSourceDropsRowsAndResetsCursor() = runTest {
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR))
        db.appDao().upsert(AppEntity(slug = "alpha", name = "Alpha"))
        db.appDao().upsert(
            AppEntity(slug = "proprietary", name = "Proprietary", listing = Listing.CLOSED_SOURCE)
        )

        syncer.onShowClosedSourceChanged(false)

        assertNotNull(db.appDao().get("alpha"))
        assertNull(db.appDao().get("proprietary"))
        assertNull(db.syncStateDao().get()!!.cursor)
    }

    private fun routes(vararg pairs: Pair<String, MockResponse>): Dispatcher {
        val map = pairs.toMap()
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path?.substringBefore('?') ?: "/"
                return map[path] ?: MockResponse().setResponseCode(404)
            }
        }
    }

    private fun json(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        const val OLD_CURSOR = "2026-01-01T00:00:00+00:00"
        const val GENERATED_AT = "2026-06-01T00:00:00+00:00"

        val BOOTSTRAP_PAGE = """
            {
              "items": [
                {
                  "slug": "alpha", "name": "Alpha", "description": "first",
                  "listing": "main", "type": "app", "availability": "direct_apk",
                  "packageName": "com.alpha", "versionCode": 5, "versionName": "1.0",
                  "categorySlug": "tools", "updatedAt": "2026-05-01T00:00:00+00:00",
                  "iconHash": "aa", "sigSha256": "AAA BBB", "sigMd5": "ccc"
                },
                {
                  "slug": "beta", "name": "Beta", "description": "second",
                  "listing": "main", "type": "app", "availability": "link_only",
                  "categorySlug": "tools", "updatedAt": "2026-05-02T00:00:00+00:00"
                }
              ],
              "total": 2, "page": 1, "pageSize": 200
            }
        """.trimIndent()

        val CHANGES = """
            {
              "added": [
                {
                  "slug": "beta", "name": "Beta", "description": "second",
                  "listing": "main", "type": "app", "availability": "link_only",
                  "categorySlug": "tools", "updatedAt": "2026-05-02T00:00:00+00:00"
                }
              ],
              "updated": [
                {
                  "slug": "alpha", "name": "Alpha", "description": "first",
                  "listing": "main", "type": "app", "availability": "direct_apk",
                  "packageName": "com.alpha", "versionCode": 6,
                  "updatedAt": "2026-05-03T00:00:00+00:00"
                }
              ],
              "removed": [
                { "slug": "gone", "name": "Gone", "removedAt": "2026-05-20T00:00:00+00:00" }
              ]
            }
        """.trimIndent()

        val META = """
            {
              "generatedAt": "$GENERATED_AT",
              "listCommit": "abc",
              "counts": { "apps": 2, "categories": 1 }
            }
        """.trimIndent()

        val META_FLAGGED = """
            {
              "generatedAt": "$GENERATED_AT",
              "listCommit": "abc",
              "counts": { "apps": 2, "categories": 1 },
              "useInstallCountsForPopularity": true
            }
        """.trimIndent()

        val CHANGES_WITH_INSTALLS = """
            {
              "added": [],
              "updated": [],
              "removed": [],
              "installsUpdated": { "alpha": 9, "ghost": 3 }
            }
        """.trimIndent()

        val CATEGORIES = """
            [ { "slug": "tools", "name": "Tools", "section": "apps", "appCount": 2, "children": [] } ]
        """.trimIndent()
    }
}
