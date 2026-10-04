/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObtainiumExporterTest {
    private fun forgeApp() = AppEntity(
        slug = "obtainium",
        name = "Obtainium",
        packageName = "dev.imranr.obtainium",
        authorName = "ImranR98",
        versionName = "1.1.30",
        availability = Availability.DIRECT_APK,
        sourceUrl = "https://github.com/ImranR98/Obtainium",
        url = "https://github.com/ImranR98/Obtainium"
    )

    @Test
    fun emitsSchemaTwoDocumentWithObtainiumFields() {
        val json = buildObtainiumExportJson(
            listOf(forgeApp()),
            exportedAt = "2026-10-04T00:00:00Z"
        )!!
        val root = Json.parseToJsonElement(json).jsonObject
        assertEquals(2, root.getValue("schemaVersion").jsonPrimitive.content.toInt())
        assertEquals("2026-10-04T00:00:00Z", root.getValue("exportedAt").jsonPrimitive.content)

        val app = root.getValue("apps").jsonArray.single().jsonObject
        assertEquals("dev.imranr.obtainium", app.getValue("id").jsonPrimitive.content)
        assertEquals(
            "https://github.com/ImranR98/Obtainium",
            app.getValue("url").jsonPrimitive.content
        )
        assertEquals("ImranR98", app.getValue("author").jsonPrimitive.content)
        assertEquals("Obtainium", app.getValue("name").jsonPrimitive.content)
        assertEquals("1.1.30", app.getValue("latestVersion").jsonPrimitive.content)
        assertEquals("{}", app.getValue("additionalSettings").jsonPrimitive.content)
        assertEquals("[]", app.getValue("apkUrls").jsonPrimitive.content)
    }

    @Test
    fun fallsBackToPackageNameSourceAndUnknownVersion() {
        val app = forgeApp().copy(
            name = " ",
            authorName = null,
            sourceName = "GitHub",
            versionName = null
        )
        val json = buildObtainiumExportJson(listOf(app), exportedAt = "t")!!
        val entry = Json.parseToJsonElement(json)
            .jsonObject.getValue("apps").jsonArray.single().jsonObject
        assertEquals("dev.imranr.obtainium", entry.getValue("name").jsonPrimitive.content)
        assertEquals("GitHub", entry.getValue("author").jsonPrimitive.content)
        assertEquals("unknown", entry.getValue("latestVersion").jsonPrimitive.content)
    }

    @Test
    fun skipsRowsWithoutPackageOrTrackableSource() {
        val linkOnly = forgeApp().copy(slug = "link", availability = Availability.LINK_ONLY)
        val noPackage = forgeApp().copy(slug = "nopkg", packageName = null)
        assertTrue(obtainiumExportApps(listOf(linkOnly, noPackage)).isEmpty())
        assertNull(buildObtainiumExportJson(listOf(linkOnly, noPackage), exportedAt = "t"))
    }
}
