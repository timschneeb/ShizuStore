/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.sync

import me.timschneeberger.shizustore.data.api.AppDetailDto
import me.timschneeberger.shizustore.data.api.AppSummaryDto
import me.timschneeberger.shizustore.data.api.DownloadDto
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMappersTest {

    @Test
    fun summaryCarriesPopularity() {
        val entity = AppSummaryDto(
            slug = "shizuku",
            stars = 10137,
            downloadTotal = 2500000000L,
            versionUpdatedAt = "2026-08-20T12:00:00+00:00",
            listUpdatedAt = "2026-08-19T12:00:00+00:00"
        ).toEntity(syncedAt = 7L)

        assertEquals(10137, entity.stars)
        assertEquals(2500000000L, entity.downloadTotal)
        assertEquals("2026-08-20T12:00:00+00:00", entity.versionUpdatedAt)
        assertEquals("2026-08-19T12:00:00+00:00", entity.listUpdatedAt)
        assertEquals(7L, entity.syncedAt)
    }

    @Test
    fun detailOverwritesPopularity() {
        val existing = AppEntity(slug = "shizuku", stars = 1, downloadTotal = 2L)

        val entity = existing.applyDetail(
            AppDetailDto(
                slug = "shizuku",
                stars = 42,
                downloadTotal = 1234L,
                versionUpdatedAt = "2026-08-21T12:00:00+00:00",
                listUpdatedAt = "2026-08-18T12:00:00+00:00"
            ),
            fetchedAt = 9L
        )

        assertEquals(42, entity.stars)
        assertEquals(1234L, entity.downloadTotal)
        assertEquals("2026-08-21T12:00:00+00:00", entity.versionUpdatedAt)
        assertEquals("2026-08-18T12:00:00+00:00", entity.listUpdatedAt)
        assertEquals(9L, entity.detailsFetchedAt)
    }

    @Test
    fun downloadCandidateMapsSignatureColumns() {
        val entity = DownloadDto(
            source = "fdroid",
            apkUrl = "https://f-droid.org/x.apk",
            sigSha256 = "aa",
            sigMd5 = "bb",
            minSdk = 21,
            primary = true
        ).toEntity(appSlug = "shizuku")

        assertEquals("aa", entity.sigSha256)
        assertEquals("bb", entity.sigMd5)
        assertEquals(21, entity.minSdk)
        assertTrue(entity.isPrimary)
        assertEquals(
            AppDownloadEntity.sigKeyOf("aa", "bb", "https://f-droid.org/x.apk"),
            entity.sigKey
        )
    }
}
