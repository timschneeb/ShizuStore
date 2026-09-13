/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.download

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArchiveExtractorTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun zip(vararg entries: Pair<String, ByteArray>): File {
        val file = temp.newFile("bundle.zip")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun extractsNamedEntry() {
        val payload = "apk-bytes".toByteArray()
        val archive = zip("app/release.apk" to payload, "readme.txt" to "x".toByteArray())
        val target = File(temp.root, "out/app.apk")

        assertTrue(ArchiveExtractor.extract(archive, "app/release.apk", target))
        assertArrayEquals(payload, target.readBytes())
    }

    @Test
    fun missingEntryFails() {
        val archive = zip("readme.txt" to "x".toByteArray())
        val target = File(temp.root, "out/app.apk")

        assertFalse(ArchiveExtractor.extract(archive, "app/release.apk", target))
    }
}
