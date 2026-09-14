/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import android.content.Context
import androidx.room.Room
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.dao.BlacklistDao
import me.timschneeberger.shizustore.data.room.dao.CategoryDao
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.dao.FavouriteDao
import me.timschneeberger.shizustore.data.room.dao.IgnoredUpdateDao
import me.timschneeberger.shizustore.data.room.dao.InstalledDao
import me.timschneeberger.shizustore.data.room.dao.SyncStateDao

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    const val DATABASE_NAME = "shizu.db"

    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): AuroraDatabase =
        buildDatabase(context, BundledSQLiteDriver())

    internal fun buildDatabase(context: Context, driver: SQLiteDriver): AuroraDatabase =
        Room.databaseBuilder(context, AuroraDatabase::class.java, DATABASE_NAME)
            .setDriver(driver)
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6
            )
            .build()

    @Provides
    fun providesAppDao(db: AuroraDatabase): AppDao = db.appDao()

    @Provides
    fun providesAppDownloadDao(db: AuroraDatabase): AppDownloadDao = db.appDownloadDao()

    @Provides
    fun providesCategoryDao(db: AuroraDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun providesSyncStateDao(db: AuroraDatabase): SyncStateDao = db.syncStateDao()

    @Provides
    fun providesInstalledDao(db: AuroraDatabase): InstalledDao = db.installedDao()

    @Provides
    fun providesDownloadDao(db: AuroraDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun providesFavouriteDao(db: AuroraDatabase): FavouriteDao = db.favouriteDao()

    @Provides
    fun providesBlacklistDao(db: AuroraDatabase): BlacklistDao = db.blacklistDao()

    @Provides
    fun providesIgnoredUpdateDao(db: AuroraDatabase): IgnoredUpdateDao = db.ignoredUpdateDao()
}
