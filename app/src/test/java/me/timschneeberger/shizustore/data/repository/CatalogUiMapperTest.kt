/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import android.os.Build
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.DetailedApp
import me.timschneeberger.shizustore.data.model.preferredForThisDevice
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.TrackerTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class CatalogUiMapperTest {

    private val mapper = CatalogUiMapper()

    @Test
    fun perAbiDownloadsCollapseIntoOneSource() {
        val app = app()
        val sources = mapper.toSources(
            DetailedApp(
                app,
                listOf(
                    candidate(
                        download(
                            id = 1,
                            packageName = null,
                            abi = null,
                            primary = true,
                            url = "universal"
                        )
                    ),
                    candidate(
                        download(id = 2, packageName = null, abi = "arm64-v8a", url = "arm64")
                    ),
                    candidate(download(id = 3, packageName = null, abi = "x86_64", url = "x86_64")),
                    candidate(
                        download(id = 4, packageName = "app.mihon.foss", abi = null, url = "foss")
                    )
                )
            ),
            null
        )

        assertEquals(2, sources.size)
        assertEquals(
            setOf("app.mihon", "app.mihon.foss"),
            sources.map {
                it.app.packageName
            }.toSet()
        )
    }

    @Test
    fun abiVariantsOfTheSameVersionKeepOnlyThePickerCandidate() {
        val app = app()
        val sources = mapper.toSources(
            DetailedApp(
                app,
                listOf(
                    candidate(
                        download(
                            id = 1,
                            packageName = "app.mihon",
                            abi = "arm64-v8a",
                            url = "arm64"
                        )
                    ),
                    candidate(
                        download(id = 2, packageName = "app.mihon", abi = "x86_64", url = "x86_64")
                    )
                )
            ),
            null
        )

        assertEquals(1, sources.size)
        assertEquals(1L, sources.single().app.candidateId)
    }

    @Test
    fun distinctVersionsAndSignersStaySeparateSources() {
        val app = app()
        val sources = mapper.toSources(
            DetailedApp(
                app,
                listOf(
                    candidate(
                        download(id = 1, packageName = "app.mihon", versionCode = 29, url = "v29")
                    ),
                    candidate(
                        download(id = 2, packageName = "app.mihon", versionCode = 30, url = "v30")
                    ),
                    candidate(
                        download(
                            id = 3,
                            packageName = "app.mihon",
                            sigSha256 = "bbbbbbbb",
                            url = "other-signer"
                        )
                    )
                )
            ),
            null
        )

        assertEquals(3, sources.size)
    }

    @Test
    fun perAbiDownloadsWithDistinctVersionCodesCollapseOnVersionName() {
        val app = app()
        val sources = mapper.toSources(
            DetailedApp(
                app,
                listOf(
                    candidate(
                        download(
                            id = 1,
                            packageName = null,
                            versionName = "1.6.17",
                            versionCode = 2356,
                            primary = true,
                            url = "universal"
                        )
                    ),
                    candidate(
                        download(
                            id = 2,
                            packageName = null,
                            abi = "arm64-v8a",
                            versionName = "1.6.17",
                            versionCode = 23563,
                            url = "arm64"
                        )
                    ),
                    candidate(
                        download(
                            id = 3,
                            packageName = null,
                            abi = "armeabi-v7a",
                            versionName = "1.6.17",
                            versionCode = 23562,
                            url = "v7a"
                        )
                    )
                )
            ),
            null
        )

        assertEquals(1, sources.size)
    }

    @Test
    fun installedFlavorSourceIsMarkedAndPreferred() {
        val app = app().copy(installedVersionCode = 24)
        val sources = mapper.toSources(
            DetailedApp(
                app,
                listOf(
                    candidate(
                        download(id = 1, packageName = "app.mihon", primary = true, url = "base")
                    ),
                    candidate(download(id = 2, packageName = "app.mihon.foss", url = "foss"))
                )
            ),
            null,
            installedPackage = "app.mihon.foss"
        )

        assertEquals(listOf(false, true), sources.map { it.installedPackageMatch })
        assertEquals("app.mihon.foss", sources.preferredForThisDevice()?.app?.packageName)
        assertNull(sources.first { it.app.packageName == "app.mihon" }.app.installedVersionCode)
        assertEquals(
            24L,
            sources.first {
                it.app.packageName == "app.mihon.foss"
            }.app.installedVersionCode
        )
    }

    @Test
    fun resolvedAppParsesListAndVersionTimestamps() {
        val app = app().copy(
            listUpdatedAt = "2026-05-01T00:00:00+00:00",
            versionUpdatedAt = null
        )

        val resolved = mapper.toResolvedApp(app)

        assertEquals(1_777_593_600_000L, resolved.listUpdatedAtMillis)
        assertNull(resolved.versionUpdatedAtMillis)
    }

    @Test
    fun resolvedAppCarriesCategoryAndMonetizationFields() {
        val app = app().copy(
            hasAds = true,
            downloadTotal = 1234,
            categorySlug = "tool"
        )

        val resolved = mapper.toResolvedApp(app)

        assertEquals(true, resolved.hasAds)
        assertEquals(1234L, resolved.downloadTotal)
        assertEquals("tool", resolved.categorySlug)
    }

    @Test
    fun resolvedAppCarriesAnalysisSignals() {
        val app = app().copy(
            dhizukuDeclared = true,
            trackers = listOf("AppLovin", "Google Analytics")
        )

        val resolved = mapper.toResolvedApp(app)

        assertEquals(true, resolved.dhizukuDeclared)
        assertEquals(listOf("AppLovin", "Google Analytics"), resolved.trackers)
    }

    @Test
    fun resolvedAppPrefersLocalizedSummaryName() {
        val app = app().copy(
            localizedLabels = mapOf(
                "en" to "Mihon English",
                "de" to "Mihon Deutsch"
            )
        )

        val resolved = mapper.toResolvedApp(app)

        assertEquals("Mihon English", resolved.localizedName)
        assertEquals("Mihon English", resolved.displayName)
    }

    @Test
    fun resolvedAppFallsBackToTheCatalogNameWithoutLabels() {
        val resolved = mapper.toResolvedApp(app())

        assertNull(resolved.localizedName)
        assertEquals("Mihon", resolved.displayName)
    }

    @Test
    fun appDetailsCarriesCountsAndAds() {
        val app = app().copy(
            hasAds = true,
            installCount = 42,
            downloadTotal = 12345,
            versionUpdatedAt = "2026-05-01T00:00:00+00:00"
        )

        val details = mapper.toAppDetails(DetailedApp(app, emptyList()))

        assertEquals(true, details.hasAds)
        assertEquals(42L, details.installCount)
        assertEquals(12345L, details.downloadTotal)
        assertEquals(1_777_593_600_000L, details.versionUpdatedAtMillis)
    }

    @Test
    fun appDetailsCarriesAnalysisSignals() {
        val app = app().copy(
            dhizukuDeclared = true,
            trackers = listOf("AppLovin"),
            trackerTags = listOf(TrackerTag("AppLovin", listOf("Analytics", "Advertisement")))
        )

        val details = mapper.toAppDetails(DetailedApp(app, emptyList()))

        assertEquals(true, details.dhizukuDeclared)
        assertEquals(listOf("AppLovin"), details.trackers)
        assertEquals(
            listOf(TrackerTag("AppLovin", listOf("Analytics", "Advertisement"))),
            details.trackerTags
        )
    }

    @Test
    fun appDetailsCarriesListing() {
        val closed = app().copy(listing = Listing.CLOSED_SOURCE)

        val details = mapper.toAppDetails(DetailedApp(closed, emptyList()))

        assertEquals(Listing.CLOSED_SOURCE, details.listing)
    }

    @Test
    fun appDetailsFallsBackToPrimaryApkFacts() {
        val primary = candidate(
            download(
                id = 1,
                packageName = "app.mihon",
                primary = true,
                targetSdk = 35,
                compileSdk = 36,
                locales = listOf("en", "de"),
                abis = listOf("arm64-v8a", "armeabi-v7a"),
                localizedLabels = mapOf("en" to "Mihon English", "de" to "Mihon Deutsch"),
                signerDn = "CN=Mihon",
                signerScheme = "v2+v3",
                signerKeyAlgorithm = "RSA 2048",
                url = "base"
            )
        )

        val details = mapper.toAppDetails(DetailedApp(app(), listOf(primary)))

        assertEquals(35, details.targetSdk)
        assertEquals(36, details.compileSdk)
        assertEquals(2, details.localeCount)
        assertEquals(listOf("en", "de"), details.locales)
        assertEquals(listOf("arm64-v8a", "armeabi-v7a"), details.abis)
        assertEquals("Mihon English", details.localizedName)
        assertEquals("CN=Mihon", details.signerDn)
        assertEquals("v2+v3", details.signerScheme)
        assertEquals("RSA 2048", details.signerKeyAlgorithm)
    }

    @Test
    fun appSummaryFactsWinOverPrimaryFallback() {
        val app = app().copy(
            targetSdk = 34,
            compileSdk = 35,
            localeCount = 5,
            abis = listOf("x86_64")
        )
        val primary = candidate(
            download(
                id = 1,
                packageName = "app.mihon",
                primary = true,
                targetSdk = 35,
                compileSdk = 36,
                locales = listOf("en", "de"),
                abis = listOf("arm64-v8a"),
                url = "base"
            )
        )

        val details = mapper.toAppDetails(DetailedApp(app, listOf(primary)))

        assertEquals(34, details.targetSdk)
        assertEquals(35, details.compileSdk)
        assertEquals(5, details.localeCount)
        assertEquals(listOf("x86_64"), details.abis)
    }

    @Test
    fun appDetailsCarriesAiUsageReport() {
        val app = app().copy(
            usageShort = "Can install apps using PackageManager.",
            usageMarkdown = "Installs via `PackageManager` (`Installer.kt:3`).",
            usageAnalyzedAt = "2026-05-02T00:00:00+00:00"
        )

        val details = mapper.toAppDetails(DetailedApp(app, emptyList()))

        assertEquals("Can install apps using PackageManager.", details.usageShort)
        assertEquals("Installs via `PackageManager` (`Installer.kt:3`).", details.usageMarkdown)
        assertEquals("2026-05-02T00:00:00+00:00", details.usageAnalyzedAt)
    }

    private fun app(): AppEntity =
        AppEntity(slug = "mihon", name = "Mihon", packageName = "app.mihon", versionCode = 29)

    private fun candidate(entity: AppDownloadEntity): AppCandidate = AppCandidate.from(entity, null)

    private fun download(
        id: Long,
        packageName: String?,
        abi: String? = null,
        sigSha256: String = "aaaaaaaa",
        versionCode: Long = 29,
        versionName: String? = null,
        primary: Boolean = false,
        targetSdk: Int? = null,
        compileSdk: Int? = null,
        locales: List<String> = emptyList(),
        abis: List<String> = emptyList(),
        localizedLabels: Map<String, String> = emptyMap(),
        signerDn: String? = null,
        signerScheme: String? = null,
        signerKeyAlgorithm: String? = null,
        url: String
    ): AppDownloadEntity = AppDownloadEntity(
        id = id,
        appSlug = "mihon",
        packageName = packageName,
        apkUrl = "https://example/$url.apk",
        versionCode = versionCode,
        versionName = versionName,
        sigSha256 = sigSha256,
        abi = abi,
        isPrimary = primary,
        targetSdk = targetSdk,
        compileSdk = compileSdk,
        locales = locales,
        abis = abis,
        localizedLabels = localizedLabels,
        signerDn = signerDn,
        signerScheme = signerScheme,
        signerKeyAlgorithm = signerKeyAlgorithm,
        sigKey = AppDownloadEntity.sigKeyOf(sigSha256, null, "https://example/$url.apk", abi)
    )
}
