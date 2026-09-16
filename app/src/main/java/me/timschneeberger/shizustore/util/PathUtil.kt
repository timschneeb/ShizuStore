/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.content.Context
import java.io.File

object PathUtil {

    fun getApkDir(context: Context): File =
        File(context.filesDir, "apk").apply { if (!exists()) mkdirs() }

    fun getApkFile(context: Context, packageName: String, versionCode: Long): File =
        File(getApkDir(context), "${packageName}_$versionCode.apk")

    /** Staging file for a zip artifact that still has to be unpacked into an apk. */
    fun getArchiveFile(context: Context, packageName: String, versionCode: Long): File =
        File(getApkDir(context), "${packageName}_$versionCode.archive")
}
