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
import me.timschneeberger.shizustore.data.room.entity.FavouriteEntity

@Dao
interface FavouriteDao {

    @Query("SELECT packageName FROM favourite ORDER BY packageName ASC")
    fun observeAll(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite WHERE packageName = :packageName)")
    fun observeIsFavourite(packageName: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favourite WHERE packageName = :packageName)")
    suspend fun isFavourite(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favourite: FavouriteEntity)

    @Query("DELETE FROM favourite WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Transaction
    suspend fun toggle(packageName: String) {
        if (isFavourite(packageName)) {
            delete(packageName)
        } else {
            insert(FavouriteEntity(packageName))
        }
    }
}
