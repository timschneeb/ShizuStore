/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import android.os.LocaleList
import java.util.Locale

/** Device locales in descending preference, for choosing a localized app label. */
object DeviceLocales {
    val current: List<String> by lazy {
        runCatching {
            val list = LocaleList.getAdjustedDefault()
            (0 until list.size()).map { list[it].toLanguageTag() }
        }.getOrElse { listOf(Locale.getDefault().toLanguageTag()) }
    }
}

/**
 * Picks the best `application-label-<locale>` for the device. Each device locale is
 * tried as full tag, language+script, language+region, then bare language, so
 * `b+zh+Hans` and `de-rDE` style aapt2 qualifiers both resolve.
 */
fun pickLocalizedLabel(
    labels: Map<String, String>,
    deviceLocales: List<String> = DeviceLocales.current
): String? {
    val key = pickLocalizedLabelKey(labels, deviceLocales) ?: return null
    return normalizedLabels(labels)[key]
}

/**
 * Normalized qualifier of the entry [pickLocalizedLabel] would choose. Lets the
 * language dialog mark the same row the header uses.
 */
fun pickLocalizedLabelKey(
    labels: Map<String, String>,
    deviceLocales: List<String> = DeviceLocales.current
): String? {
    if (labels.isEmpty()) return null
    val normalized = normalizedLabels(labels)
    deviceLocales.forEach { device ->
        val tag = normalizeLocaleQualifier(device) ?: return@forEach
        localeCandidates(tag).forEach { candidate ->
            if (normalized.containsKey(candidate)) return candidate
        }
    }
    return null
}

private fun normalizedLabels(labels: Map<String, String>): Map<String, String> = buildMap {
    labels.forEach { (key, label) ->
        val normalizedKey = normalizeLocaleQualifier(key)
        if (normalizedKey != null && label.isNotBlank()) {
            putIfAbsent(normalizedKey, label)
        }
    }
}

internal fun normalizeLocaleQualifier(raw: String): String? {
    val value = raw.trim().lowercase().replace('_', '-')
    if (value.isEmpty()) return null
    val withoutPrefix = value.removePrefix("b+").replace('+', '-')
    return withoutPrefix.replace(Regex("-r([a-z]{2}|[0-9]{3})$"), "-$1")
}

private fun localeCandidates(tag: String): List<String> {
    val parts = tag.split('-').filter { it.isNotBlank() }
    if (parts.isEmpty()) return emptyList()
    val language = parts[0]
    val second = parts.getOrNull(1)
    val script = second?.takeIf { it.length == 4 && it.all(Char::isLetter) }
    val region = (if (script != null) parts.getOrNull(2) else second)?.takeIf {
        (it.length == 2 && it.all(Char::isLetter)) || (it.length == 3 && it.all(Char::isDigit))
    }
    return buildList {
        add(tag)
        script?.let { add("$language-$it") }
        region?.let { add("$language-$it") }
        add(language)
    }.distinct()
}
