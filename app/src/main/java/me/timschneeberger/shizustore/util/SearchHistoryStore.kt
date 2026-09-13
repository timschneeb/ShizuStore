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
        val updated = (
            listOf(trimmed) + current.filterNot {
                it.equals(trimmed, ignoreCase = true)
            }
            )
            .take(MAX_ENTRIES)
        Preferences.putString(context, Preferences.PREFERENCE_SEARCH_HISTORY, encode(updated))
    }

    suspend fun clear() {
        Preferences.putString(context, Preferences.PREFERENCE_SEARCH_HISTORY, "")
    }

    private fun decode(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching { ShizuJson.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    private fun encode(values: List<String>): String = ShizuJson.encodeToString(serializer, values)

    private companion object {
        const val MAX_ENTRIES = 3
        val serializer = ListSerializer(String.serializer())
    }
}
