/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.paging.PagingSource
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.data.api.AppType
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.CategorySection
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.SourceKind
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryPath
import me.timschneeberger.shizustore.data.room.entity.SyncStateEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CatalogDaoTest {

    private lateinit var db: AuroraDatabase

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                AuroraDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun app(
        slug: String,
        name: String,
        availability: Availability = Availability.DIRECT_APK
    ) = AppEntity(
        slug = slug,
        name = name,
        description = "summary of $name",
        packageName = "pkg.$slug",
        listing = Listing.MAIN,
        type = AppType.APP,
        availability = availability,
        categorySlug = "tools"
    )

    @Test
    fun summaryUpsertPreservesDetailColumns() = runTest {
        val appDao = db.appDao()
        appDao.upsert(
            app("foo", "Foo").copy(
                url = "https://example.com/foo",
                storeUrl = "https://play.google.com/store/apps/details?id=pkg.foo",
                sourceKind = SourceKind.GITHUB,
                parentSlug = "root",
                categoryPath = listOf(CategoryPath("tools", "Tools")),
                addedAt = "2024-01-01T00:00:00+00:00",
                detailsFetchedAt = 42L,
                installedVersionCode = 1L
            )
        )

        appDao.upsertSummaries(listOf(app("foo", "Foo Renamed").copy(versionCode = 2L)))

        val stored = appDao.get("foo")
        assertNotNull(stored)
        assertEquals("Foo Renamed", stored!!.name)
        assertEquals(2L, stored.versionCode)
        assertEquals("https://example.com/foo", stored.url)
        assertEquals(SourceKind.GITHUB, stored.sourceKind)
        assertEquals(listOf(CategoryPath("tools", "Tools")), stored.categoryPath)
        assertEquals(42L, stored.detailsFetchedAt)
        assertEquals(1L, stored.installedVersionCode)
    }

    @Test
    fun downloadsAreOrderedAndCascadeOnAppDelete() = runTest {
        val appDao = db.appDao()
        val downloadDao = db.appDownloadDao()
        appDao.upsert(app("foo", "Foo"))
        downloadDao.replaceForApp(
            "foo",
            listOf(
                AppDownloadEntity(
                    appSlug = "foo",
                    source = SourceKind.GITLAB,
                    apkUrl = "https://example.com/foo-1.apk",
                    versionCode = 1L,
                    sigKey = "aaa",
                    isPrimary = false
                ),
                AppDownloadEntity(
                    appSlug = "foo",
                    source = SourceKind.FDROID,
                    apkUrl = "https://example.com/foo-2.apk",
                    versionCode = 2L,
                    sigKey = "bbb",
                    isPrimary = true
                )
            )
        )

        val ordered = downloadDao.forApp("foo")
        assertEquals(2, ordered.size)
        assertTrue(ordered.first().isPrimary)
        assertEquals(2L, ordered.first().versionCode)

        appDao.delete("foo")
        assertTrue(downloadDao.forApp("foo").isEmpty())
        assertEquals(0, downloadDao.count())
    }

    @Test
    fun updateStateDrivesUpdatableCount() = runTest {
        val appDao = db.appDao()
        appDao.upsert(app("foo", "Foo"))
        appDao.upsert(app("bar", "Bar"))

        appDao.setUpdateState(
            "foo",
            installedVersionCode = 1L,
            updateAvailable = true,
            updateCandidateId = 9L
        )

        assertEquals(1, appDao.observeUpdatableCount().first())
        val stored = appDao.get("foo")!!
        assertEquals(1L, stored.installedVersionCode)
        assertTrue(stored.updateAvailable)
        assertEquals(9L, stored.updateCandidateId)
    }

    @Test
    fun categorySubtreeResolvesDescendants() = runTest {
        val categoryDao = db.categoryDao()
        val appDao = db.appDao()
        categoryDao.replaceAll(
            listOf(
                CategoryEntity("tools", "Tools", CategorySection.APPS, appCount = 2, sortOrder = 0),
                CategoryEntity(
                    "shell",
                    "Shell",
                    CategorySection.APPS,
                    parentSlug = "tools",
                    sortOrder = 1
                ),
                CategoryEntity("other", "Other", CategorySection.MISC, sortOrder = 2)
            )
        )
        appDao.upsert(app("a", "Alpha").copy(categorySlug = "tools"))
        appDao.upsert(app("b", "Beta").copy(categorySlug = "shell"))
        appDao.upsert(app("c", "Gamma").copy(categorySlug = "other"))

        val page = appDao
            .pagedFiltered(AppListQueryBuilder.build(AppListArgs(categorySlug = "tools")))
            .load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            ) as PagingSource.LoadResult.Page

        assertEquals(listOf("Alpha", "Beta"), page.data.map { it.name })
    }

    @Test
    fun recommendedFilterAndCarouselSelectOnlyFlaggedApps() = runTest {
        val appDao = db.appDao()
        appDao.upsert(app("a", "Alpha").copy(isRecommended = true))
        appDao.upsert(app("b", "Beta").copy(isRecommended = false))
        appDao.upsert(app("c", "Gamma").copy(isRecommended = true))

        val page = appDao
            .pagedFiltered(AppListQueryBuilder.build(AppListArgs(recommended = true)))
            .load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            ) as PagingSource.LoadResult.Page

        assertEquals(listOf("Alpha", "Gamma"), page.data.map { it.name })
        val carousel = appDao.observeRecommendedPool().first().map { it.name }
        assertEquals(listOf("Alpha", "Gamma"), carousel)
    }

    @Test
    fun authorQueryExcludesCurrentAppAndOrdersByName() = runTest {
        val appDao = db.appDao()
        appDao.upsert(app("a", "Alpha").copy(authorKey = "github:papergray"))
        appDao.upsert(app("d", "Delta").copy(authorKey = "github:papergray"))
        appDao.upsert(app("b", "Beta").copy(authorKey = "github:papergray"))
        appDao.upsert(app("c", "Gamma").copy(authorKey = "github:other"))

        val rows = appDao
            .observeByAuthor(authorKey = "github:papergray", excludeSlug = "a", limit = 10)
            .first()
            .map { it.name }

        assertEquals(listOf("Beta", "Delta"), rows)
    }

    @Test
    fun categoryQueryExcludesCurrentAppAndOrdersByName() = runTest {
        val appDao = db.appDao()
        appDao.upsert(app("a", "Alpha").copy(categorySlug = "audio"))
        appDao.upsert(app("d", "Delta").copy(categorySlug = "audio"))
        appDao.upsert(app("b", "Beta").copy(categorySlug = "audio"))
        appDao.upsert(app("c", "Gamma").copy(categorySlug = "video"))

        val rows = appDao
            .observeByCategory(categorySlug = "audio", excludeSlug = "a", limit = 10)
            .first()
            .map { it.name }

        assertEquals(listOf("Beta", "Delta"), rows)
    }

    @Test
    fun syncStateRoundTrips() = runTest {
        val dao = db.syncStateDao()
        assertNull(dao.get())

        dao.upsert(
            SyncStateEntity(
                cursor = "2026-05-01T00:00:00+00:00",
                categoriesEtag = "\"cat-1\"",
                listCommit = "abc",
                syncedAt = 7L
            )
        )
        val stored = dao.get()!!
        assertEquals("2026-05-01T00:00:00+00:00", stored.cursor)
        assertEquals("\"cat-1\"", stored.categoriesEtag)
        assertEquals(7L, stored.syncedAt)
    }
}
