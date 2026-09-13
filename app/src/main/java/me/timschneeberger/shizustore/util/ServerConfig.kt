/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import me.timschneeberger.shizustore.BuildConfig

/**
 * Process-wide snapshot of the active server base URL. [me.timschneeberger.shizustore.MainActivity]
 * keeps it current so UI mapping (icon URLs) can stay synchronous.
 */
object ServerConfig {
    @Volatile
    var baseUrl: String = BuildConfig.API_BASE_URL
        internal set
}
