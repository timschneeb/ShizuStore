/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.room.entity.BlacklistEntity

@Dao
interface BlacklistDao {

    @Query("SELECT packageName FROM blacklist ORDER BY packageName ASC")
    fun observeAll(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE packageName = :packageName)")
    fun observeIsBlacklisted(packageName: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE packageName = :packageName)")
    suspend fun isBlacklisted(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BlacklistEntity)

    @Query("DELETE FROM blacklist WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Transaction
    suspend fun toggle(packageName: String) {
        if (isBlacklisted(packageName)) {
            delete(packageName)
        } else {
            insert(BlacklistEntity(packageName))
        }
    }
}
