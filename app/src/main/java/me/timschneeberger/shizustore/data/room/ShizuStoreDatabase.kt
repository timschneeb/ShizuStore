/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.dao.BlacklistDao
import me.timschneeberger.shizustore.data.room.dao.CategoryDao
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.dao.FavouriteDao
import me.timschneeberger.shizustore.data.room.dao.IgnoredUpdateDao
import me.timschneeberger.shizustore.data.room.dao.InstalledDao
import me.timschneeberger.shizustore.data.room.dao.SyncStateDao
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.BlacklistEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.data.room.entity.FavouriteEntity
import me.timschneeberger.shizustore.data.room.entity.IgnoredUpdateEntity
import me.timschneeberger.shizustore.data.room.entity.InstalledEntity
import me.timschneeberger.shizustore.data.room.entity.SyncStateEntity

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        AppEntity::class,
        AppDownloadEntity::class,
        CategoryEntity::class,
        SyncStateEntity::class,
        InstalledEntity::class,
        Download::class,
        FavouriteEntity::class,
        BlacklistEntity::class,
        IgnoredUpdateEntity::class
    ]
)
@TypeConverters(Converters::class)
abstract class ShizuStoreDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun appDownloadDao(): AppDownloadDao
    abstract fun categoryDao(): CategoryDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun installedDao(): InstalledDao
    abstract fun downloadDao(): DownloadDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun blacklistDao(): BlacklistDao
    abstract fun ignoredUpdateDao(): IgnoredUpdateDao
}
