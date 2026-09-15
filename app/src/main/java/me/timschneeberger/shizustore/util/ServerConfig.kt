/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import me.timschneeberger.shizustore.BuildConfig

/**
 * Process-wide snapshot of the active server base URL, so UI mapping (icon
 * URLs) can stay synchronous.
 */
object ServerConfig {
    /** The production catalog server, used whenever the custom option is off. */
    val productionBaseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')

    @Volatile
    var baseUrl: String = productionBaseUrl
        internal set
}
