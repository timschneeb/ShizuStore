/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.model.AppPrice
import me.timschneeberger.shizustore.data.model.AppSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListQueryBuilderTest {

    @Test
    fun noFiltersUsesNameOrderAndNoBinds() {
        val query = AppListQueryBuilder.build(AppListArgs())

        assertEquals("SELECT * FROM app ORDER BY name COLLATE NOCASE ASC", query.sql)
        assertEquals(0, query.argCount)
    }

    @Test
    fun sortPresetsChangeTheOrder() {
        assertTrue(
            AppListQueryBuilder.build(AppListArgs(sort = AppSort.RECENTLY_ADDED)).sql
                .endsWith(
                    "ORDER BY listUpdatedAt IS NULL, listUpdatedAt DESC," +
                        " name COLLATE NOCASE ASC"
                )
        )
        assertTrue(
            AppListQueryBuilder.build(AppListArgs(sort = AppSort.RECENTLY_UPDATED)).sql
                .endsWith(
                    "ORDER BY versionUpdatedAt IS NULL, versionUpdatedAt DESC," +
                        " name COLLATE NOCASE ASC"
                )
        )
        assertTrue(
            AppListQueryBuilder.build(AppListArgs(sort = AppSort.STARS)).sql
                .endsWith("ORDER BY stars IS NULL, stars DESC, name COLLATE NOCASE ASC")
        )
        assertTrue(
            AppListQueryBuilder.build(AppListArgs(sort = AppSort.DOWNLOADS)).sql
                .endsWith(
                    "ORDER BY downloadTotal IS NULL, downloadTotal DESC, name COLLATE NOCASE ASC"
                )
        )
        assertTrue(
            AppListQueryBuilder.build(AppListArgs(sort = AppSort.SIZE_DESC)).sql
                .endsWith("ORDER BY size IS NULL, size DESC, name COLLATE NOCASE ASC")
        )
    }

    @Test
    fun downloadsSortUsesInstallCountsWhenFlagged() {
        assertTrue(
            AppListQueryBuilder.build(
                AppListArgs(sort = AppSort.DOWNLOADS),
                useInstallCountsForPopularity = true
            ).sql.endsWith("ORDER BY installCount DESC, name COLLATE NOCASE ASC")
        )
    }

    @Test
    fun searchBindsAnEscapedPatternPerColumn() {
        val query = AppListQueryBuilder.build(AppListArgs(query = "50%"))

        assertTrue(query.sql.contains("name LIKE '%' || ? || '%' ESCAPE '\\'"))
        assertTrue(query.sql.contains("packageName LIKE '%' || ? || '%' ESCAPE '\\'"))
        assertTrue(query.sql.contains("description LIKE '%' || ? || '%' ESCAPE '\\'"))
        assertEquals(3, query.argCount)
    }

    @Test
    fun categoryUsesRecursiveSubtreeAndBindsTheSlug() {
        val query = AppListQueryBuilder.build(AppListArgs(categorySlug = "vendor-specific"))

        assertTrue(query.sql.startsWith("WITH RECURSIVE tree(slug) AS ("))
        assertTrue(query.sql.contains("categorySlug IN (SELECT slug FROM tree)"))
        assertEquals(1, query.argCount)
    }

    @Test
    fun priceFilterEmitsTheRightBucket() {
        val free = AppListQueryBuilder.build(AppListArgs(price = AppPrice.FREE))
        assertTrue(free.sql.contains("hasPaid = 0 AND hasIap = 0"))

        val iap = AppListQueryBuilder.build(AppListArgs(price = AppPrice.IAP))
        assertTrue(iap.sql.contains("hasPaid = 0 AND hasIap = 1"))

        val iapOrPaid = AppListQueryBuilder.build(AppListArgs(price = AppPrice.IAP_OR_PAID))
        assertTrue(iapOrPaid.sql.contains("(hasPaid = 1 OR hasIap = 1)"))

        assertEquals(0, free.argCount)
        assertEquals(0, iap.argCount)
        assertEquals(0, iapOrPaid.argCount)
    }

    @Test
    fun recommendedFilterEmitsBooleanMatch() {
        val query = AppListQueryBuilder.build(AppListArgs(recommended = true))

        assertTrue(query.sql.contains("isRecommended = 1"))
        assertEquals(0, query.argCount)
    }

    @Test
    fun allFiltersCombineWithAnd() {
        val query = AppListQueryBuilder.build(
            AppListArgs(
                query = "foo",
                categorySlug = "audio",
                price = AppPrice.IAP,
                recommended = true,
                sort = AppSort.RECENTLY_UPDATED
            )
        )

        assertTrue(query.sql.contains(" AND "))
        assertEquals(4, query.argCount)
    }

    @Test
    fun likeWildcardsAreEscaped() {
        assertEquals("50\\%\\_a\\\\b", AppListQueryBuilder.escapeLikePattern("50%_a\\b"))
    }
}
