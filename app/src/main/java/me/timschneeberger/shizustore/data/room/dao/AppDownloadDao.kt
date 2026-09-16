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
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity

@Dao
interface AppDownloadDao {

    @Query(
        "SELECT * FROM app_download WHERE appSlug = :slug ORDER BY isPrimary DESC, versionCode DESC"
    )
    suspend fun forApp(slug: String): List<AppDownloadEntity>

    @Query(
        "SELECT * FROM app_download WHERE appSlug = :slug ORDER BY isPrimary DESC, versionCode DESC"
    )
    fun observeForApp(slug: String): Flow<List<AppDownloadEntity>>

    @Query("SELECT * FROM app_download WHERE id = :id")
    suspend fun getById(id: Long): AppDownloadEntity?

    @Upsert
    suspend fun upsertAll(downloads: List<AppDownloadEntity>)

    @Query("DELETE FROM app_download WHERE appSlug = :slug")
    suspend fun deleteForApp(slug: String)

    @Query("SELECT COUNT(*) FROM app_download")
    suspend fun count(): Int

    @Transaction
    suspend fun replaceForApp(slug: String, downloads: List<AppDownloadEntity>) {
        deleteForApp(slug)
        upsertAll(downloads)
    }
}
