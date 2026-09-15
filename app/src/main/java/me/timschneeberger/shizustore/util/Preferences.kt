/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences as StoredPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.timschneeberger.shizustore.extensions.isOneUI

object Preferences {
    private const val TAG = "Preferences"

    val dynamicColorsDefault: Boolean
        get() = !isOneUI

    const val PREFERENCE_INSTALLER_ID = "PREFERENCE_INSTALLER_ID"
    const val PREFERENCE_THEME_STYLE = "PREFERENCE_THEME_STYLE"
    const val PREFERENCE_DYNAMIC_COLORS = "PREFERENCE_DYNAMIC_COLORS"
    const val PREFERENCE_BLACK_NIGHT = "PREFERENCE_BLACK_NIGHT"

    const val PREFERENCE_NOTIFICATION_ASKED = "PREFERENCE_NOTIFICATION_ASKED"
    const val PREFERENCE_DOZE_ASKED = "PREFERENCE_DOZE_ASKED"

    const val PREFERENCE_PROXY_INFO = "PREFERENCE_PROXY_INFO"

    const val PREFERENCE_SHIZUKU_CARD_DISMISSED = "PREFERENCE_SHIZUKU_CARD_DISMISSED"

    const val PREFERENCE_API_BASE_URL = "PREFERENCE_API_BASE_URL"

    const val PREFERENCE_SEARCH_HISTORY = "PREFERENCE_SEARCH_HISTORY"

    const val PREFERENCE_SYNC_ON_WIFI_ONLY = "PREFERENCE_SYNC_ON_WIFI_ONLY"
    const val PREFERENCE_UPDATES_CHECK_INTERVAL = "PREFERENCE_UPDATES_CHECK_INTERVAL"

    const val PREFERENCE_UPDATES_UNATTENDED = "PREFERENCE_UPDATES_UNATTENDED"

    internal const val STORE_NAME = "shizu_preferences"

    private var store: DataStore<StoredPreferences>? = null
    private var storeFile: File? = null
    private var storeScope: CoroutineScope? = null

    private fun store(context: Context): DataStore<StoredPreferences> {
        val app = context.applicationContext
        val file = app.preferencesDataStoreFile(STORE_NAME)

        synchronized(this) {
            store?.let { cached -> if (storeFile == file) return cached }

            storeScope?.cancel()

            val scope = isolatedIoScope(TAG)
            val created = PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
                produceFile = { file }
            )

            store = created
            storeFile = file
            storeScope = scope
            return created
        }
    }

    private fun data(context: Context): Flow<StoredPreferences> =
        store(context).data.catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Preferences are unreadable; reading them as unset", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    private suspend fun write(context: Context, block: (MutablePreferences) -> Unit) {
        try {
            store(context).edit(block)
        } catch (e: IOException) {
            Log.w(TAG, "A preference change could not be persisted", e)
        }
    }

    fun booleanFlow(context: Context, key: String, default: Boolean = false): Flow<Boolean> =
        data(context).map { it[booleanPreferencesKey(key)] ?: default }

    fun integerFlow(context: Context, key: String, default: Int = 0): Flow<Int> =
        data(context).map { it[intPreferencesKey(key)] ?: default }

    fun stringFlow(context: Context, key: String, default: String = ""): Flow<String> =
        data(context).map { it[stringPreferencesKey(key)] ?: default }

    suspend fun readBoolean(context: Context, key: String, default: Boolean = false): Boolean =
        booleanFlow(context, key, default).first()

    suspend fun readInteger(context: Context, key: String, default: Int = 0): Int =
        integerFlow(context, key, default).first()

    suspend fun readString(context: Context, key: String, default: String = ""): String =
        stringFlow(context, key, default).first()

    suspend fun putBoolean(context: Context, key: String, value: Boolean) =
        write(context) { it[booleanPreferencesKey(key)] = value }

    suspend fun putInteger(context: Context, key: String, value: Int) =
        write(context) { it[intPreferencesKey(key)] = value }

    suspend fun putString(context: Context, key: String, value: String) =
        write(context) { it[stringPreferencesKey(key)] = value }

    suspend fun remove(context: Context, key: String) =
        write(context) { it.remove(booleanPreferencesKey(key)) }
}
