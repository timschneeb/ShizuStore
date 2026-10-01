/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Parses the storefront links (`https://shizustore.com/apps/{slug}` and
 * `shizustore://apps/{slug}`) into the catalog identifier of the target app.
 *
 * The custom scheme also accepts `?package={pkg}`; a valid package wins over
 * the path slug so callers that know the package need no slug lookup.
 *
 * Parsing is done on the raw string with [URI] instead of `android.net.Uri` so
 * the rules stay unit-testable on the JVM, and it never throws on hostile input.
 */
object DeepLinks {
    const val HOST = "shizustore.com"
    const val PATH_PREFIX = "/apps/"
    const val SCHEME = "shizustore"

    /** Storefront page of a catalog slug, the inverse of [parseAppId]. */
    fun appPage(slug: String): String = "https://$HOST$PATH_PREFIX$slug"

    private const val PACKAGE_PARAM = "package"
    private const val PACKAGE_MAX_LENGTH = 128
    private val packagePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun parseAppId(uri: String?): String? {
        val raw = uri?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val parsed = runCatching { URI(raw) }.getOrNull() ?: return null
        val segment = when (parsed.scheme?.lowercase()) {
            "https" -> {
                if (!HOST.equals(parsed.host, ignoreCase = true)) return null
                val path = parsed.path ?: return null
                if (!path.startsWith(PATH_PREFIX)) return null
                path.removePrefix(PATH_PREFIX)
            }
            SCHEME -> {
                if (!"apps".equals(parsed.host, ignoreCase = true)) return null
                packageParameter(parsed) ?: (parsed.path ?: return null).removePrefix("/")
            }
            else -> return null
        }

        return segment.trim('/').takeIf { it.isNotEmpty() && !it.contains('/') }
    }

    private fun packageParameter(uri: URI): String? = uri.rawQuery
        ?.split('&')
        ?.firstNotNullOfOrNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0 || part.substring(0, separator) != PACKAGE_PARAM) {
                return@firstNotNullOfOrNull null
            }
            decode(part.substring(separator + 1))
        }
        ?.takeIf {
            it.isNotEmpty() &&
                it.length <= PACKAGE_MAX_LENGTH &&
                packagePattern.matches(it)
        }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)
}
