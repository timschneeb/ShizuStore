/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
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
    fun providesDatabase(@ApplicationContext context: Context): ShizuStoreDatabase =
        buildDatabase(context, BundledSQLiteDriver())

    internal fun buildDatabase(context: Context, driver: SQLiteDriver): ShizuStoreDatabase =
        Room.databaseBuilder(context, ShizuStoreDatabase::class.java, DATABASE_NAME)
            .setDriver(driver)
            .addMigrations(MIGRATION_1_2)
            .build()

    /**
     * APK analysis signals added to `app`. The catalog cache is disposable but
     * favourites and install state are not, so this must never be destructive.
     */
    internal val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN dhizukuDeclared INTEGER NOT NULL DEFAULT 0"
            )
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN trackers TEXT NOT NULL DEFAULT '[]'"
            )
        }
    }

    /** Driver-mode migrations use the raw connection, not SupportSQLiteDatabase. */
    private fun execSql(connection: SQLiteConnection, sql: String) {
        val statement = connection.prepare(sql)
        try {
            statement.step()
        } finally {
            statement.close()
        }
    }

    @Provides
    fun providesAppDao(db: ShizuStoreDatabase): AppDao = db.appDao()

    @Provides
    fun providesAppDownloadDao(db: ShizuStoreDatabase): AppDownloadDao = db.appDownloadDao()

    @Provides
    fun providesCategoryDao(db: ShizuStoreDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun providesSyncStateDao(db: ShizuStoreDatabase): SyncStateDao = db.syncStateDao()

    @Provides
    fun providesInstalledDao(db: ShizuStoreDatabase): InstalledDao = db.installedDao()

    @Provides
    fun providesDownloadDao(db: ShizuStoreDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun providesFavouriteDao(db: ShizuStoreDatabase): FavouriteDao = db.favouriteDao()

    @Provides
    fun providesBlacklistDao(db: ShizuStoreDatabase): BlacklistDao = db.blacklistDao()

    @Provides
    fun providesIgnoredUpdateDao(db: ShizuStoreDatabase): IgnoredUpdateDao = db.ignoredUpdateDao()
}
