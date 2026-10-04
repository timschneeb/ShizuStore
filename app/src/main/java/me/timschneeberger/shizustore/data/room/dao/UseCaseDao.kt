/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.room.entity.UseCaseEntity

@Dao
interface UseCaseDao {

    @Query("SELECT * FROM use_case ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<UseCaseEntity>>

    @Upsert
    suspend fun upsertAll(useCases: List<UseCaseEntity>)

    @Query("DELETE FROM use_case")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(useCases: List<UseCaseEntity>) {
        clear()
        upsertAll(useCases)
    }
}
