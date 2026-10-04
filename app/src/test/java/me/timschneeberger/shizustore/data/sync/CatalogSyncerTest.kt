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
import me.timschneeberger.shizustore.data.room.entity.FavouriteEntity
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity
import me.timschneeberger.shizustore.data.room.entity.SyncStateEntity
import me.timschneeberger.shizustore.data.room.entity.UseCaseEntity
import me.timschneeberger.shizustore.util.CommonUtil
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
            useCaseDao = db.useCaseDao(),
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
        val alpha = db.appDao().get("alpha")!!
        assertEquals(34, alpha.targetSdk)
        assertEquals(35, alpha.compileSdk)
        assertEquals(3, alpha.localeCount)
        assertEquals(listOf("arm64-v8a"), alpha.abis)
        assertEquals(mapOf("de" to "Alpha DE"), alpha.localizedLabels)
        assertEquals(BOOTSTRAP_CURSOR, db.syncStateDao().get()!!.cursor)
        assertEquals(listOf("tools"), db.categoryDao().observeAll().first().map { it.slug })
        assertEquals(listOf("install-apps"), alpha.useCases)

        // The cursor is captured before the pages are read: meta first, apps second.
        assertEquals("/v1/meta", server.takeRequest().path)
        assertTrue(server.takeRequest().path!!.startsWith("/v1/apps"))
    }

    @Test
    fun bootstrapRefreshesUseCasesAndStoresTheEtag() = runTest {
        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES),
            "/v1/use-cases" to json(USE_CASES).addHeader("ETag", "\"uc-2-4\"")
        )

        val outcome = syncer.sync()

        assertTrue(outcome is CatalogSyncOutcome.Success)
        val useCases = db.useCaseDao().observeAll().first()
        assertEquals(listOf("install-apps", "settings-writes"), useCases.map { it.slug })
        assertEquals("Install and uninstall apps", useCases.first().name)
        assertEquals("\"uc-2-4\"", db.syncStateDao().get()!!.useCasesEtag)
    }

    @Test
    fun useCaseRefreshFailuresAreNonFatal() = runTest {
        db.useCaseDao().upsertAll(
            listOf(UseCaseEntity("install-apps", "Install and uninstall apps"))
        )

        server.dispatcher = routes(
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES),
            "/v1/use-cases" to MockResponse().setResponseCode(500)
        )

        val outcome = syncer.sync()

        assertTrue(outcome is CatalogSyncOutcome.Success)
        assertEquals(
            listOf("install-apps"),
            db.useCaseDao().observeAll().first().map { it.slug }
        )
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
        val alpha = db.appDao().get("alpha")!!
        assertEquals(35, alpha.targetSdk)
        assertEquals(36, alpha.compileSdk)
        assertEquals(2, alpha.localeCount)
        assertEquals(listOf("arm64-v8a", "armeabi-v7a"), alpha.abis)
        assertEquals(mapOf("de" to "Alpha DE", "zh" to "Alpha 中文"), alpha.localizedLabels)
        assertEquals(0, db.appDownloadDao().count())
        // The delta's own cursor wins over the later /v1/meta timestamp.
        assertEquals(CHANGES_CURSOR, db.syncStateDao().get()!!.cursor)
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
        // Older servers send no delta cursor, so meta's timestamp is the fallback.
        assertEquals(GENERATED_AT, db.syncStateDao().get()!!.cursor)
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
        db.useCaseDao().upsertAll(listOf(UseCaseEntity("stale-tag", "Stale tag")))

        syncer.clearCatalog()

        assertEquals(0, db.appDao().count())
        assertEquals(0, db.appDownloadDao().count())
        assertEquals(0, db.categoryDao().observeAll().first().size)
        assertEquals(0, db.useCaseDao().observeAll().first().size)
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
        assertEquals(BOOTSTRAP_CURSOR, db.syncStateDao().get()!!.cursor)
    }

    @Test
    fun remotePurgeWipesCatalogAndBootstrapsPreservingUserData() = runTest {
        setLastPurgeApplied(0L)
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR, categoriesEtag = "etag-1"))
        db.appDao().upsert(AppEntity(slug = "gone", name = "Gone"))
        db.appDownloadDao().upsertAll(
            listOf(
                AppDownloadEntity(appSlug = "gone", apkUrl = "https://x/gone.apk", sigKey = "k")
            )
        )
        db.categoryDao().upsertAll(listOf(CategoryEntity(slug = "stale", name = "Stale")))
        db.favouriteDao().insert(FavouriteEntity("com.gone"))

        server.dispatcher = routes(
            "/v1/changes" to json(CHANGES_WITH_PURGE),
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES)
        )

        val outcome = syncer.sync()

        assertTrue(outcome is CatalogSyncOutcome.Success)
        assertEquals(2, db.appDao().count())
        assertNull(db.appDao().get("gone"))
        // The delta entry for "dead" is dropped: a purge refills via bootstrap.
        assertNull(db.appDao().get("dead"))
        assertEquals(0, db.appDownloadDao().count())
        assertEquals(listOf("tools"), db.categoryDao().observeAll().first().map { it.slug })
        assertEquals(listOf("com.gone"), db.favouriteDao().observeAll().first())
        assertEquals(PURGE_AT_MILLIS, lastPurgeApplied())
        assertEquals(BOOTSTRAP_CURSOR, db.syncStateDao().get()!!.cursor)

        // The purge dropped the old ETag, so categories must be refetched in
        // full. Requests: changes, pre-bootstrap meta, apps, then categories.
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        val categoriesRequest = server.takeRequest()
        assertTrue(categoriesRequest.path!!.startsWith("/v1/categories"))
        assertNull(categoriesRequest.getHeader("If-None-Match"))
    }

    @Test
    fun remotePurgeAppliesOnceThenSyncsIncrementally() = runTest {
        setLastPurgeApplied(0L)
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR))
        db.appDao().upsert(AppEntity(slug = "gone", name = "Gone"))

        server.dispatcher = routes(
            "/v1/changes" to json(CHANGES_WITH_PURGE),
            "/v1/apps" to json(BOOTSTRAP_PAGE),
            "/v1/meta" to json(META),
            "/v1/categories" to json(CATEGORIES)
        )
        assertTrue(syncer.sync() is CatalogSyncOutcome.Success)
        assertEquals(PURGE_AT_MILLIS, lastPurgeApplied())

        // Same high-water mark on the next sync: incremental path, no /v1/apps
        // route on this dispatcher, so a second purge would fail the sync.
        server.dispatcher = routes(
            "/v1/changes" to json(CHANGES_WITH_PURGE),
            "/v1/meta" to json(META),
            "/v1/categories" to MockResponse().setResponseCode(304)
        )
        val outcome = syncer.sync() as CatalogSyncOutcome.Success

        assertEquals(1, outcome.updated)
        assertEquals("Dead", db.appDao().get("dead")!!.name)
    }

    @Test
    fun olderPurgeTimestampIsIgnored() = runTest {
        setLastPurgeApplied(PURGE_AT_MILLIS + 60_000)
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR))
        db.appDao().upsert(AppEntity(slug = "keep", name = "Keep"))

        server.dispatcher = routes(
            "/v1/changes" to json(CHANGES_PURGE_ONLY),
            "/v1/meta" to json(META),
            "/v1/categories" to MockResponse().setResponseCode(304)
        )

        assertTrue(syncer.sync() is CatalogSyncOutcome.Success)
        assertNotNull(db.appDao().get("keep"))
        assertEquals(PURGE_AT_MILLIS + 60_000, lastPurgeApplied())
    }

    @Test
    fun malformedPurgeTimestampIsIgnored() = runTest {
        setLastPurgeApplied(0L)
        db.syncStateDao().upsert(SyncStateEntity(cursor = OLD_CURSOR))
        db.appDao().upsert(AppEntity(slug = "keep", name = "Keep"))

        server.dispatcher = routes(
            "/v1/changes" to json(CHANGES_WITH_BAD_PURGE),
            "/v1/meta" to json(META),
            "/v1/categories" to MockResponse().setResponseCode(304)
        )

        assertTrue(syncer.sync() is CatalogSyncOutcome.Success)
        assertNotNull(db.appDao().get("keep"))
        assertEquals(0L, lastPurgeApplied())
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

        server.takeRequest()
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

        server.takeRequest()
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

    private suspend fun lastPurgeApplied(): Long = Preferences.readLong(
        RuntimeEnvironment.getApplication(),
        Preferences.PREFERENCE_LAST_CATALOG_PURGE_AT,
        0L
    )

    private suspend fun setLastPurgeApplied(value: Long) = Preferences.putLong(
        RuntimeEnvironment.getApplication(),
        Preferences.PREFERENCE_LAST_CATALOG_PURGE_AT,
        value
    )

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

        // Cursors step one second back from the server timestamp so a commit on
        // the boundary replays through the next delta.
        const val BOOTSTRAP_CURSOR = "2026-05-31T23:59:59Z"
        const val CHANGES_GENERATED_AT = "2026-05-30T12:00:00+00:00"
        const val CHANGES_CURSOR = "2026-05-30T11:59:59Z"

        val BOOTSTRAP_PAGE = """
            {
              "items": [
                {
                  "slug": "alpha", "name": "Alpha", "description": "first",
                  "listing": "main", "type": "app", "availability": "direct_apk",
                  "packageName": "com.alpha", "versionCode": 5, "versionName": "1.0",
                  "categorySlug": "tools", "updatedAt": "2026-05-01T00:00:00+00:00",
                  "iconHash": "aa", "sigSha256": "AAA BBB", "sigMd5": "ccc",
                  "targetSdk": 34, "compileSdk": 35, "localeCount": 3,
                  "abis": ["arm64-v8a"],
                  "localizedLabels": {"de": "Alpha DE"},
                  "managers": ["shizuku"],
                  "useCases": [{ "slug": "install-apps", "name": "Install and uninstall apps" }]
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
                  "targetSdk": 35, "compileSdk": 36, "localeCount": 2,
                  "abis": ["arm64-v8a", "armeabi-v7a"],
                  "localizedLabels": {"de": "Alpha DE", "zh": "Alpha 中文"},
                  "managers": ["shizuku", "root"],
                  "updatedAt": "2026-05-03T00:00:00+00:00"
                }
              ],
              "removed": [
                { "slug": "gone", "name": "Gone", "removedAt": "2026-05-20T00:00:00+00:00" }
              ],
              "generatedAt": "$CHANGES_GENERATED_AT"
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

        const val PURGE_AT = "2026-06-15T00:00:00+00:00"
        val PURGE_AT_MILLIS = CommonUtil.parseIsoUtcMillis(PURGE_AT)!!

        val CHANGES_WITH_PURGE = """
            {
              "added": [],
              "updated": [
                {
                  "slug": "dead", "name": "Dead", "description": "stale",
                  "listing": "main", "type": "app", "availability": "link_only",
                  "categorySlug": "tools", "updatedAt": "2026-05-03T00:00:00+00:00"
                }
              ],
              "removed": [
                { "slug": "gone", "name": "Gone", "removedAt": "2026-05-20T00:00:00+00:00" }
              ],
              "catalogPurgeRequestedAt": "$PURGE_AT"
            }
        """.trimIndent()

        val CHANGES_PURGE_ONLY = """
            {
              "added": [],
              "updated": [],
              "removed": [],
              "catalogPurgeRequestedAt": "$PURGE_AT"
            }
        """.trimIndent()

        val CHANGES_WITH_BAD_PURGE = """
            {
              "added": [],
              "updated": [],
              "removed": [],
              "catalogPurgeRequestedAt": "not-a-date"
            }
        """.trimIndent()

        val CATEGORIES = """
            [ { "slug": "tools", "name": "Tools", "section": "apps", "appCount": 2, "children": [] } ]
        """.trimIndent()

        val USE_CASES = """
            [
              { "slug": "install-apps", "name": "Install and uninstall apps", "appCount": 3 },
              { "slug": "settings-writes", "name": "Change system settings", "appCount": 1 }
            ]
        """.trimIndent()
    }
}
