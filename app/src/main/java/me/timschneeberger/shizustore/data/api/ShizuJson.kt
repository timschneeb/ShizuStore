/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration for the server API. `explicitNulls = false` keeps
 * encoded requests free of null fields; decoding tolerates unknown keys and
 * coerces explicit nulls for non-null fields to their defaults.
 */
val ShizuJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
