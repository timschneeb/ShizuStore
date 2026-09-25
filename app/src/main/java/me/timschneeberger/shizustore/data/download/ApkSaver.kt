/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies a verified, staged APK into the public Downloads collection. Only
 * API 29+ can write there without a storage permission; older devices go
 * through the SAF export picker instead.
 */
object ApkSaver {
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"

    fun canSaveDirectly(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun fileName(packageName: String, versionCode: Long): String = "${packageName}_$versionCode.apk"

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveToDownloads(context: Context, source: File, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = runCatching {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            }.getOrNull() ?: return@withContext false

            try {
                val stream = resolver.openOutputStream(uri) ?: return@withContext false
                stream.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failed: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                false
            }
        }
}
