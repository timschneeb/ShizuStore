/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.mergeDetailFrom

@Dao
interface AppDao {

    @Query("SELECT * FROM app WHERE slug = :slug")
    suspend fun get(slug: String): AppEntity?

    @Query("SELECT * FROM app WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): AppEntity?

    @Query("SELECT * FROM app WHERE slug = :slug")
    fun observe(slug: String): Flow<AppEntity?>

    @Query("SELECT * FROM app WHERE packageName = :packageName")
    fun observeByPackage(packageName: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM app")
    suspend fun getAll(): List<AppEntity>

    @Query("SELECT slug FROM app")
    suspend fun allSlugs(): List<String>

    @Query("SELECT COUNT(*) FROM app")
    suspend fun count(): Int

    @Query(
        "UPDATE app SET installedVersionCode = NULL, updateAvailable = 0, updateCandidateId = NULL"
    )
    suspend fun clearUpdateState()

    @Upsert
    suspend fun upsert(app: AppEntity)

    @Upsert
    suspend fun upsertAll(apps: List<AppEntity>)

    @Query("DELETE FROM app WHERE slug = :slug")
    suspend fun delete(slug: String)

    @Query("DELETE FROM app WHERE slug IN (:slugs)")
    suspend fun deleteBySlugs(slugs: List<String>)

    @Query("DELETE FROM app")
    suspend fun clear()

    /** Summary-only refresh that preserves already fetched detail columns. */
    @Transaction
    suspend fun upsertSummaries(summaries: List<AppEntity>) {
        summaries.forEach { summary ->
            upsert(summary.mergeDetailFrom(get(summary.slug)))
        }
    }

    @Query(
        "UPDATE app SET installedVersionCode = :installedVersionCode," +
            " updateAvailable = :updateAvailable, updateCandidateId = :updateCandidateId" +
            " WHERE slug = :slug"
    )
    suspend fun setUpdateState(
        slug: String,
        installedVersionCode: Long?,
        updateAvailable: Boolean,
        updateCandidateId: Long?
    )

    /** One bound query for every filter combination; see `AppListQueryBuilder`. */
    @RawQuery(observedEntities = [AppEntity::class])
    fun pagedFiltered(query: SupportSQLiteQuery): PagingSource<Int, AppEntity>

    @Query("SELECT * FROM app WHERE updateAvailable = 1 ORDER BY name COLLATE NOCASE ASC")
    fun pagedUpdatable(): PagingSource<Int, AppEntity>

    @Query("SELECT COUNT(*) FROM app WHERE updateAvailable = 1")
    fun observeUpdatableCount(): Flow<Int>

    @Query(
        "SELECT * FROM app WHERE installedVersionCode IS NOT NULL ORDER BY name COLLATE NOCASE ASC"
    )
    fun pagedInstalled(): PagingSource<Int, AppEntity>

    @Query(
        "SELECT * FROM app ORDER BY listUpdatedAt IS NULL, listUpdatedAt DESC," +
            " name COLLATE NOCASE ASC LIMIT :limit"
    )
    fun observeRecentlyAdded(limit: Int): Flow<List<AppEntity>>

    @Query("SELECT * FROM app WHERE isRecommended = 1 ORDER BY slug ASC")
    fun observeRecommendedPool(): Flow<List<AppEntity>>

    @Query(
        "SELECT * FROM app ORDER BY versionUpdatedAt IS NULL, versionUpdatedAt DESC," +
            " name COLLATE NOCASE ASC LIMIT :limit"
    )
    fun observeRecentlyUpdated(limit: Int): Flow<List<AppEntity>>

    @Query("SELECT * FROM app ORDER BY slug ASC")
    fun observeAll(): Flow<List<AppEntity>>

    @Query(
        "SELECT a.* FROM app a JOIN favourite f ON f.packageName = a.packageName" +
            " WHERE a.packageName IS NOT NULL ORDER BY a.name COLLATE NOCASE ASC"
    )
    fun pagedFavourites(): PagingSource<Int, AppEntity>

    @Query(
        "SELECT a.* FROM app a JOIN blacklist b ON b.packageName = a.packageName" +
            " WHERE a.packageName IS NOT NULL ORDER BY a.name COLLATE NOCASE ASC"
    )
    fun observeBlacklisted(): Flow<List<AppEntity>>

    @Query(
        "SELECT a.* FROM app a JOIN ignored_update i ON i.packageName = a.packageName" +
            " WHERE a.packageName IS NOT NULL ORDER BY a.name COLLATE NOCASE ASC"
    )
    fun observeIgnoredUpdates(): Flow<List<AppEntity>>
}
