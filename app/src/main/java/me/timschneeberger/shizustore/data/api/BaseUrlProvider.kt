/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * A Settings override wins over the build-time default; release builds default
 * to empty and require the override.
 */
class BaseUrlProvider(
    private val readOverride: suspend () -> String?,
    private val observeOverride: () -> Flow<String> = { flowOf("") },
    private val defaultValue: String
) {
    suspend fun current(): String = pick().trim().trimEnd('/')

    suspend fun isConfigured(): Boolean = pick().isNotBlank()

    /** Reactive normalized base, seeded with the default so collectors see a value immediately. */
    fun observe(): Flow<String> = observeOverride()
        .map { it.ifBlank { defaultValue } }
        .onStart { emit(defaultValue) }
        .map { it.trim().trimEnd('/') }
        .distinctUntilChanged()

    private suspend fun pick(): String = readOverride()?.takeIf { it.isNotBlank() } ?: defaultValue
}
