/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.room.entity.SyncStateEntity

@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state WHERE id = ${SyncStateEntity.SINGLETON_ID}")
    suspend fun get(): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE id = ${SyncStateEntity.SINGLETON_ID}")
    fun observe(): Flow<SyncStateEntity?>

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    @Query("DELETE FROM sync_state")
    suspend fun clear()

    /** Forces the next sync to bootstrap after the listing set changed. */
    @Query("UPDATE sync_state SET cursor = NULL")
    suspend fun clearCursor()
}
