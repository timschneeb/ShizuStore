/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.room.entity.IgnoredUpdateEntity

@Dao
interface IgnoredUpdateDao {

    @Query("SELECT * FROM ignored_update ORDER BY packageName ASC")
    fun observeAll(): Flow<List<IgnoredUpdateEntity>>

    @Query("SELECT * FROM ignored_update WHERE packageName = :packageName")
    fun observe(packageName: String): Flow<IgnoredUpdateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: IgnoredUpdateEntity)

    @Query("DELETE FROM ignored_update WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
