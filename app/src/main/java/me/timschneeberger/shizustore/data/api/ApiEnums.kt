/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.serialization.Serializable

/**
 * Typed views of the server's lowercase/snake enum strings. DTOs keep the raw
 * string; these mappings are tolerant by design: an unknown value maps to
 * `null` instead of throwing, so a server-side addition never breaks the app.
 */

enum class Availability(val wire: String) {
    DIRECT_APK("direct_apk"),
    PLAY_REDIRECT("play_redirect"),
    LINK_ONLY("link_only"),
    EXCLUDED("excluded")
    ;

    companion object {
        fun fromWire(raw: String?): Availability? =
            entries.firstOrNull { it.wire == raw.normalizedWire() }
    }
}

@Serializable
enum class Listing(val wire: String) {
    MAIN("main"),
    CLOSED_SOURCE("closed_source")
    ;

    companion object {
        fun fromWire(raw: String?): Listing? =
            entries.firstOrNull { it.wire == raw.normalizedWire() }
    }
}

enum class AppType(val wire: String) {
    APP("app"),
    LIBRARY("library"),
    FLOW("flow")
    ;

    companion object {
        fun fromWire(raw: String?): AppType? =
            entries.firstOrNull { it.wire == raw.normalizedWire() }
    }
}

enum class CategorySection(val wire: String) {
    APPS("apps"),
    LIBRARIES("libraries"),
    MISC("misc")
    ;

    companion object {
        fun fromWire(raw: String?): CategorySection? =
            entries.firstOrNull { it.wire == raw.normalizedWire() }
    }
}

enum class SourceKind(val wire: String) {
    GITHUB("github"),
    GITLAB("gitlab"),
    CODEBERG("codeberg"),
    FDROID("fdroid"),
    IZZY("izzy"),
    PLAY("play"),
    OTHER("other")
    ;

    companion object {
        fun fromWire(raw: String?): SourceKind? =
            entries.firstOrNull { it.wire == raw.normalizedWire() }
    }
}

private fun String?.normalizedWire(): String? = this?.trim()?.lowercase()
