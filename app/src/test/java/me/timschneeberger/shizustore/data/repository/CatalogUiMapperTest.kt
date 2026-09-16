/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import android.os.Build
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.DetailedApp
import me.timschneeberger.shizustore.data.model.preferredForThisDevice
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
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
        sigKey = AppDownloadEntity.sigKeyOf(sigSha256, null, "https://example/$url.apk", abi)
    )
}
