/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import me.timschneeberger.shizustore.data.api.ShizuJson

/** Recent search queries, newest first, de-duplicated case-insensitively and capped. */
@Singleton
class SearchHistoryStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    val history: Flow<List<String>> = Preferences
        .stringFlow(context, Preferences.PREFERENCE_SEARCH_HISTORY)
        .map(::decode)
        .distinctUntilChanged()

    suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val current = decode(Preferences.readString(context, Preferences.PREFERENCE_SEARCH_HISTORY))
        val updated = merge(current, trimmed)
        Preferences.putString(context, Preferences.PREFERENCE_SEARCH_HISTORY, encode(updated))
    }

    suspend fun clear() {
        Preferences.putString(context, Preferences.PREFERENCE_SEARCH_HISTORY, "")
    }

    private fun decode(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val values =
            runCatching { ShizuJson.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
        return prune(values)
    }

    private fun encode(values: List<String>): String = ShizuJson.encodeToString(serializer, values)

    internal companion object {
        const val MAX_ENTRIES = 3
        val serializer = ListSerializer(String.serializer())

        /** Drops entries a longer entry extends, which cleans histories written
         * before prefix chains were collapsed on record. */
        fun prune(values: List<String>): List<String> = values.filterNot { candidate ->
            values.any { other ->
                other.length > candidate.length &&
                    other.startsWith(candidate, ignoreCase = true)
            }
        }

        /**
         * The field filters live, so [record] sees every debounced prefix of the
         * query being typed. A remembered query that extends the new one wins, and
         * backspacing to a prefix must not evict it. Otherwise the new (longest)
         * query goes to the front and the prefixes it supersedes are dropped.
         */
        fun merge(current: List<String>, query: String): List<String> {
            if (current.any { it.length > query.length && it.startsWith(query, ignoreCase = true) }) {
                return prune(current)
            }
            return (
                listOf(query) + current.filterNot {
                    it.equals(query, ignoreCase = true) ||
                        (query.length > it.length && query.startsWith(it, ignoreCase = true))
                }
                )
                .take(MAX_ENTRIES)
        }
    }
}
