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
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM category")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>) {
        clear()
        upsertAll(categories)
    }
}
