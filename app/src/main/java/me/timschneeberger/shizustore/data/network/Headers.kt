/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Droid-ify (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.network

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.io.encoding.Base64
import okhttp3.Headers

@Suppress("NOTHING_TO_INLINE")
inline fun Headers.Builder.addIfNotBlank(name: String, value: String?): Headers.Builder {
    if (!value.isNullOrBlank()) add(name, value)
    return this
}

fun Headers.Builder.etag(etagString: String): Headers.Builder = addIfNotBlank("ETag", etagString)

fun Headers.Builder.ifModifiedSince(date: Date): Headers.Builder =
    addIfNotBlank("If-Modified-Since", date.toFormattedString())

fun Headers.Builder.ifModifiedSince(date: String): Headers.Builder =
    addIfNotBlank("If-Modified-Since", date)

fun Headers.Builder.authentication(username: String, password: String): Headers.Builder =
    addIfNotBlank(
        "Authorization",
        "Basic ${Base64.encode("$username:$password".encodeToByteArray())}"
    )

fun Headers.Builder.authentication(base64: String): Headers.Builder =
    addIfNotBlank("Authorization", base64)

fun Headers.Builder.inRange(start: Long, end: Long? = null): Headers.Builder =
    addIfNotBlank("Range", if (end != null) "bytes=$start-$end" else "bytes=$start-")

private val HTTP_DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }
}

private fun Date.toFormattedString(): String = HTTP_DATE_FORMAT.get()!!.format(this)
