/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.RobolectricTestBase
import me.timschneeberger.shizustore.data.api.AppType
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.CategorySection
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.SourceKind
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.repository.AppRepository
import me.timschneeberger.shizustore.data.repository.CatalogUiMapper
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryPath
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity
import me.timschneeberger.shizustore.data.room.entity.TrackerTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogDaoTest : RobolectricTestBase() {

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
    fun analysisSignalsRoundTrip() = runTest {
        val appDao = db.appDao()
        appDao.upsert(
            app("foo", "Foo").copy(
                dhizukuDeclared = true,
                trackers = listOf("AppLovin", "Google Analytics"),
                trackerTags = listOf(
                    TrackerTag("AppLovin", listOf("Analytics", "Advertisement")),
                    TrackerTag("Google Analytics", listOf("Analytics"))
                )
            )
        )

        val stored = appDao.get("foo")

        assertNotNull(stored)
        assertTrue(stored!!.dhizukuDeclared)
        assertEquals(listOf("AppLovin", "Google Analytics"), stored.trackers)
        assertEquals(
            listOf(
                TrackerTag("AppLovin", listOf("Analytics", "Advertisement")),
                TrackerTag("Google Analytics", listOf("Analytics"))
            ),
            stored.trackerTags
        )
    }

    @Test
    fun apkFactsRoundTrip() = runTest {
        val appDao = db.appDao()
        val downloadDao = db.appDownloadDao()
        appDao.upsert(
            app("foo", "Foo").copy(
                targetSdk = 35,
                compileSdk = 36,
                localeCount = 2,
                abis = listOf("arm64-v8a", "armeabi-v7a"),
                localizedLabels = mapOf("de" to "Foo DE")
            )
        )
        downloadDao.replaceForApp(
            "foo",
            listOf(
                AppDownloadEntity(
                    appSlug = "foo",
                    source = SourceKind.GITHUB,
                    apkUrl = "https://example.com/foo.apk",
                    sigKey = "aaa",
                    isPrimary = true,
                    targetSdk = 35,
                    compileSdk = 36,
                    locales = listOf("en", "de"),
                    abis = listOf("arm64-v8a"),
                    localizedLabels = mapOf("en" to "Foo", "de" to "Foo DE"),
                    signerDn = "CN=Foo",
                    signerScheme = "v2+v3",
                    signerKeyAlgorithm = "RSA 2048"
                )
            )
        )

        val storedApp = appDao.get("foo")!!
        assertEquals(35, storedApp.targetSdk)
        assertEquals(36, storedApp.compileSdk)
        assertEquals(2, storedApp.localeCount)
        assertEquals(listOf("arm64-v8a", "armeabi-v7a"), storedApp.abis)
        assertEquals(mapOf("de" to "Foo DE"), storedApp.localizedLabels)

        val storedDownload = downloadDao.forApp("foo").single()
        assertEquals(35, storedDownload.targetSdk)
        assertEquals(36, storedDownload.compileSdk)
        assertEquals(listOf("en", "de"), storedDownload.locales)
        assertEquals(listOf("arm64-v8a"), storedDownload.abis)
        assertEquals(mapOf("en" to "Foo", "de" to "Foo DE"), storedDownload.localizedLabels)
        assertEquals("CN=Foo", storedDownload.signerDn)
        assertEquals("v2+v3", storedDownload.signerScheme)
        assertEquals("RSA 2048", storedDownload.signerKeyAlgorithm)
    }

    @Test
    fun usageReportRoundTrip() = runTest {
        val appDao = db.appDao()
        appDao.upsert(
            app("foo", "Foo").copy(
                usageShort = "Can install apps using PackageManager.",
                usageMarkdown = "Installs via `PackageManager`.",
                usageAnalyzedAt = "2026-05-02T00:00:00+00:00"
            )
        )

        val stored = appDao.get("foo")

        assertNotNull(stored)
        assertEquals("Can install apps using PackageManager.", stored!!.usageShort)
        assertEquals("Installs via `PackageManager`.", stored.usageMarkdown)
        assertEquals("2026-05-02T00:00:00+00:00", stored.usageAnalyzedAt)
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
            updateCandidateId = 9L,
            updateIgnored = false
        )

        assertEquals(1, appDao.observeUpdatableCount().first())
        val stored = appDao.get("foo")!!
        assertEquals(1L, stored.installedVersionCode)
        assertTrue(stored.updateAvailable)
        assertEquals(9L, stored.updateCandidateId)

        appDao.setUpdateState(
            "foo",
            installedVersionCode = 1L,
            updateAvailable = true,
            updateCandidateId = 9L,
            updateIgnored = true
        )

        assertEquals(0, appDao.observeUpdatableCount().first())
        assertTrue(appDao.getUpdatable().isEmpty())
    }

    @Test
    fun installedPackageForResolvesTheInstalledFlavor() = runTest {
        val appDao = db.appDao()
        appDao.upsert(app("mihon", "Mihon").copy(packageName = "app.mihon"))
        db.appDownloadDao().replaceForApp(
            "mihon",
            listOf(
                AppDownloadEntity(
                    appSlug = "mihon",
                    source = SourceKind.GITHUB,
                    apkUrl = "https://example.com/base.apk",
                    versionCode = 29L,
                    sigKey = "aaa",
                    packageName = null
                ),
                AppDownloadEntity(
                    appSlug = "mihon",
                    source = SourceKind.GITHUB,
                    apkUrl = "https://example.com/foss.apk",
                    versionCode = 29L,
                    sigKey = "bbb",
                    packageName = "app.mihon.foss"
                )
            )
        )
        val repository = AppRepository(
            appDao,
            db.appDownloadDao(),
            db.categoryDao(),
            db.useCaseDao(),
            db.syncStateDao(),
            db.installedDao(),
            CatalogUiMapper()
        )

        assertNull(repository.installedPackageFor("mihon"))

        db.installedDao().upsert(
            InstalledEntity(
                packageName = "app.mihon.foss",
                versionCode = 24L,
                versionName = "0.20.4",
                signer = "sig"
            )
        )

        assertEquals("app.mihon.foss", repository.installedPackageFor("mihon"))
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
    fun popularQueryExcludesZeroInstallsAndOrdersByCount() = runTest {
        val appDao = db.appDao()
        appDao.upsert(app("a", "Alpha").copy(installCount = 5))
        appDao.upsert(app("b", "Beta").copy(installCount = 0))
        appDao.upsert(app("c", "Gamma").copy(installCount = 12))
        appDao.upsert(app("d", "Delta").copy(installCount = 5))

        val rows = appDao.observePopular(limit = 10).first().map { it.name }

        assertEquals(listOf("Gamma", "Alpha", "Delta"), rows)
    }
}
