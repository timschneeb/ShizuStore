/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.timschneeberger.shizustore.BuildConfig
import me.timschneeberger.shizustore.compose.ui.details.composable.obtainiumRepoUrl
import me.timschneeberger.shizustore.data.room.entity.AppEntity

private const val TAG = "ObtainiumExporter"

internal const val EXPORT_FILE_NAME = "shizustore-obtainium.json"

/**
 * Obtainium's importer reads every field as a string that itself contains
 * JSON, so the defaults here mirror its own `App.toJson` output. `apkUrls`
 * stays empty on purpose: Obtainium resolves candidates from the source URL,
 * and placeholder entries only add noise.
 */
private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = false
}

@Serializable
internal data class ObtainiumExportApp(
    val id: String,
    val url: String,
    val author: String,
    val name: String,
    val latestVersion: String,
    val apkUrls: String = "[]",
    val otherAssetUrls: String = "[]",
    val preferredApkIndex: Int = -1,
    val additionalSettings: String = "{}",
    val pinned: Boolean = false,
    val categories: List<String> = emptyList(),
    val allowIdChange: Boolean = false
)

@Serializable
internal data class ObtainiumExport(
    val schemaVersion: Int = 2,
    val exportedAt: String,
    val appVersion: String,
    val apps: List<ObtainiumExportApp>
)

/**
 * Rows Obtainium can actually follow: a package to key the app on plus a forge
 * or F-Droid source URL. Link-only and Play-only rows are not trackable there.
 */
internal fun obtainiumExportApps(apps: List<AppEntity>): List<ObtainiumExportApp> =
    apps.mapNotNull { app ->
        val packageName = app.packageName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val url = obtainiumRepoUrl(app.availability, app.sourceUrl, app.url)
            ?: return@mapNotNull null
        ObtainiumExportApp(
            id = packageName,
            url = url,
            author = app.authorName ?: app.sourceName.orEmpty(),
            name = app.name.ifBlank { packageName },
            latestVersion = app.versionName ?: "unknown"
        )
    }

/** Null when nothing is trackable, so callers can surface an empty-state message. */
internal fun buildObtainiumExportJson(
    apps: List<AppEntity>,
    exportedAt: String = Instant.now().toString(),
    appVersion: String = BuildConfig.VERSION_NAME
): String? {
    val entries = obtainiumExportApps(apps)
    if (entries.isEmpty()) return null
    return exportJson.encodeToString(
        ObtainiumExport(exportedAt = exportedAt, appVersion = appVersion, apps = entries)
    )
}

/**
 * Writes the export into the URI the storage access framework returned, so the
 * file lands wherever the user picked it instead of behind a share target.
 */
internal suspend fun ContentResolver.writeObtainiumExport(uri: Uri, json: String): Boolean =
    try {
        withContext(Dispatchers.IO) {
            openOutputStream(uri)?.use { it.write(json.toByteArray()) } != null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Could not write the Obtainium export", e)
        false
    }
