/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.download

import java.io.File
import java.util.zip.ZipFile

/** Some upstream releases ship the apk inside a zip; the server names the member in `archiveEntry`. */
object ArchiveExtractor {

    fun extract(archive: File, entryName: String, target: File): Boolean = runCatching {
        target.parentFile?.let { if (!it.exists()) it.mkdirs() }

        ZipFile(archive).use { zip ->
            val entry = zip.getEntry(entryName)
            if (entry == null || entry.isDirectory) {
                false
            } else {
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        }
    }.getOrDefault(false)
}
