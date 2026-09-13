/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity

@Dao
interface InstalledDao {

    @Query("SELECT * FROM installed")
    fun observeAll(): Flow<List<InstalledEntity>>

    @Query("SELECT * FROM installed")
    suspend fun getAll(): List<InstalledEntity>

    @Query("SELECT * FROM installed WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): InstalledEntity?

    @Query("SELECT * FROM installed WHERE packageName = :packageName")
    fun observeByPackage(packageName: String): Flow<InstalledEntity?>

    @Upsert
    suspend fun upsert(installed: InstalledEntity)

    @Insert
    suspend fun insertAll(installed: List<InstalledEntity>)

    @Query("DELETE FROM installed WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM installed")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(installed: List<InstalledEntity>) {
        clear()
        insertAll(installed)
    }
}
