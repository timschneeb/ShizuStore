/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Droid-ify (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.network

import okhttp3.Headers

@Suppress("NOTHING_TO_INLINE")
inline fun Headers.Builder.addIfNotBlank(name: String, value: String?): Headers.Builder {
    if (!value.isNullOrBlank()) add(name, value)
    return this
}

fun Headers.Builder.inRange(start: Long, end: Long? = null): Headers.Builder =
    addIfNotBlank("Range", if (end != null) "bytes=$start-$end" else "bytes=$start-")
