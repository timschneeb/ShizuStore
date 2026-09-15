/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

object ShizuUrls {
    /** Immutable icon URL, or null when the app has no icon. */
    fun icon(baseUrl: String, hash: String?): String? {
        val cleanHash = hash?.trim().orEmpty()
        val cleanBase = baseUrl.trim().trimEnd('/')
        if (cleanHash.isEmpty() || cleanBase.isEmpty()) return null
        return "$cleanBase/icons/$cleanHash.png"
    }
}
