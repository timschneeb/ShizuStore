/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizuDtoParsingTest {
    private val json: Json = ShizuJson

    @Test
    fun parsesListEnvelope() {
        val page = json.decodeFromString(
            PagedAppsDto.serializer(),
            """
            {
              "items": [
                {
                  "slug": "aurorastore",
                  "name": "AuroraStore",
                  "description": "A store",
                  "license": "GPL-3.0",
                  "listing": "main",
                  "type": "app",
                  "isRecommended": true,
                  "hasPaid": false,
                  "hasIap": false,
                  "hasAds": false,
                  "trialDays": null,
                  "requiresRoot": false,
                  "availability": "direct_apk",
                  "packageName": "com.aurora.store",
                  "versionCode": 55,
                  "versionName": "4.6.2",
                  "minSdk": 21,
                  "iconHash": "abc",
                  "iconAdaptive": false,
                  "categorySlug": "app-stores",
                  "updatedAt": "2026-05-01T00:00:00+00:00",
                  "sigSha256": "aa bb",
                  "sigMd5": "cc dd",
                  "stars": 10137,
                  "downloadTotal": 2500000000,
                  "versionUpdatedAt": "2026-08-20T12:00:00+00:00",
                  "listUpdatedAt": "2026-08-19T12:00:00+00:00",
                  "unknownField": {"ignored": true}
                }
              ],
              "total": 385,
              "page": 1,
              "pageSize": 50
            }
            """.trimIndent()
        )

        assertEquals(385, page.total)
        assertEquals(1, page.page)
        assertEquals(50, page.pageSize)
        val item = page.items.single()
        assertEquals("aurorastore", item.slug)
        assertEquals("AuroraStore", item.name)
        assertEquals("direct_apk", item.availability)
        assertEquals(55L, item.versionCode)
        assertEquals("aa bb", item.sigSha256)
        assertEquals("cc dd", item.sigMd5)
        assertEquals(10137, item.stars)
        assertEquals(2500000000L, item.downloadTotal)
        assertEquals("2026-08-20T12:00:00+00:00", item.versionUpdatedAt)
        assertEquals("2026-08-19T12:00:00+00:00", item.listUpdatedAt)
    }

    @Test
    fun parsesDetailWithMultipleDownloadsAndJoinedSignatureSets() {
        val detail = json.decodeFromString(
            AppDetailDto.serializer(),
            """
            {
              "slug": "aurorastore",
              "name": "AuroraStore",
              "description": "A store",
              "license": null,
              "listing": "main",
              "type": "app",
              "isRecommended": false,
              "hasPaid": false,
              "hasIap": false,
              "hasAds": false,
              "trialDays": null,
              "requiresRoot": false,
              "availability": "direct_apk",
              "packageName": "com.aurora.store",
              "versionCode": 55,
              "versionName": "4.6.2",
              "minSdk": 21,
              "iconHash": "abc",
              "iconAdaptive": true,
              "categorySlug": "app-stores",
              "updatedAt": "2026-05-01T00:00:00+00:00",
              "url": "https://github.com/whyorean/AuroraStore",
              "sourceUrl": null,
              "sourceKind": "github",
              "downloads": [
                {
                  "source": "gitlab",
                  "apkUrl": "https://gitlab.com/x/y.apk",
                  "archiveEntry": null,
                  "versionCode": 55,
                  "versionName": "4.6.2",
                  "size": 8123456,
                  "sha256": "9f86",
                  "sigSha256": "1111 2222",
                  "sigMd5": "aaaa bbbb",
                  "minSdk": 21,
                  "primary": true
                },
                {
                  "source": "fdroid",
                  "apkUrl": "https://f-droid.org/x.apk",
                  "archiveEntry": "base.apk",
                  "versionCode": 54,
                  "versionName": "4.6.1",
                  "size": null,
                  "sha256": null,
                  "sigSha256": "3333",
                  "sigMd5": "cccc",
                  "minSdk": 21,
                  "primary": false
                }
              ],
              "storeUrl": "https://play.google.com/x",
              "excludedReason": null,
              "categoryPath": [
                { "slug": "apps", "name": "Apps" }
              ],
              "parentSlug": null,
              "addedAt": "2024-01-01T00:00:00+00:00",
              "lastCheckedAt": "2026-09-12T03:00:00+00:00",
              "stars": 42,
              "downloadTotal": 1234,
              "versionUpdatedAt": "2026-08-20T12:00:00+00:00",
              "listUpdatedAt": "2026-08-19T12:00:00+00:00"
            }
            """.trimIndent()
        )

        assertEquals(2, detail.downloads.size)
        assertEquals("1111 2222", detail.downloads[0].sigSha256)
        assertEquals("aaaa bbbb", detail.downloads[0].sigMd5)
        assertTrue(detail.downloads[0].primary)
        assertEquals("base.apk", detail.downloads[1].archiveEntry)
        assertEquals("apps", detail.categoryPath.single().slug)
        assertNull(detail.sourceUrl)
        assertEquals(42, detail.stars)
        assertEquals(1234L, detail.downloadTotal)
        assertEquals("2026-08-20T12:00:00+00:00", detail.versionUpdatedAt)
        assertEquals("2026-08-19T12:00:00+00:00", detail.listUpdatedAt)
    }

    @Test
    fun parsesChangesCategoriesAndMeta() {
        val changes = json.decodeFromString(
            ChangesDto.serializer(),
            """
            {
              "added": [{"slug": "a", "name": "A", "availability": "link_only"}],
              "updated": [],
              "removed": [{"slug": "b", "name": null, "removedAt": "2026-07-02T00:00:00+00:00"}]
            }
            """.trimIndent()
        )
        assertEquals("a", changes.added.single().slug)
        assertNull(changes.added.single().packageName)
        assertEquals("b", changes.removed.single().slug)
        assertNull(changes.removed.single().name)

        val categories = json.decodeFromString(
            ListSerializer(CategoryNodeDto.serializer()),
            """
            [
              {"slug":"audio","name":"Audio","section":"apps","appCount":2,
               "children":[{"slug":"miui","name":"MIUI","section":"apps","appCount":2,"children":[]}]}
            ]
            """.trimIndent()
        )
        assertEquals(1, categories.size)
        assertEquals("miui", categories.single().children.single().slug)

        val meta = json.decodeFromString(
            MetaDto.serializer(),
            """{"generatedAt":"2026-09-12T19:05:00+00:00","listCommit":null,"counts":{"apps":385,"categories":15}}"""
        )
        assertEquals("2026-09-12T19:05:00+00:00", meta.generatedAt)
        assertEquals(385, meta.counts.apps)
        assertNull(meta.listCommit)
    }
}
