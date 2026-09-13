/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.work

object WorkTags {

    private const val PACKAGE_PREFIX = "pkg:"

    fun forPackage(packageName: String): String = PACKAGE_PREFIX + packageName

    fun packageOf(tag: String): String? =
        if (tag.startsWith(PACKAGE_PREFIX)) tag.removePrefix(PACKAGE_PREFIX) else null
}
