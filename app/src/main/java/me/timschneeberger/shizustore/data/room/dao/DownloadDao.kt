/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.entity.Download

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query("SELECT COUNT(*) FROM download WHERE status IN (:statuses)")
    suspend fun countByStatus(statuses: List<DownloadStatus>): Int

    @Query("UPDATE download SET status = :status WHERE packageName = :packageName")
    suspend fun updateStatus(packageName: String, status: DownloadStatus)

    @Query(
        "UPDATE download SET status = :status, error = :error WHERE packageName = :packageName"
    )
    suspend fun updateStatusAndError(
        packageName: String,
        status: DownloadStatus,
        error: DownloadFailure?
    )

    @Query(
        "UPDATE download SET status = :status, error = :error " +
            "WHERE packageName = :packageName AND status = :expected"
    )
    suspend fun updateStatusAndErrorIf(
        packageName: String,
        status: DownloadStatus,
        error: DownloadFailure?,
        expected: DownloadStatus
    ): Int

    @Query(
        "UPDATE download SET progress = :progress, speed = :speed, " +
            "timeRemaining = :timeRemaining WHERE packageName = :packageName"
    )
    suspend fun updateProgress(packageName: String, progress: Int, speed: Long, timeRemaining: Long)

    @Query("SELECT * FROM download ORDER BY downloadedAt DESC")
    fun downloads(): Flow<List<Download>>

    @Query("SELECT * FROM download ORDER BY downloadedAt DESC")
    fun pagedDownloads(): PagingSource<Int, Download>

    @Query("SELECT * FROM download WHERE packageName = :packageName")
    suspend fun getDownload(packageName: String): Download?

    @Query("DELETE FROM download WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM download")
    suspend fun deleteAll()
}
