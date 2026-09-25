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
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6
            )
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

    /**
     * Per-tracker category tags added to `app`. Additive like MIGRATION_1_2:
     * the catalog cache is disposable but user state is not.
     */
    internal val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN trackerTags TEXT NOT NULL DEFAULT '[]'"
            )
        }
    }

    /**
     * Badging and signer facts added to `app` and `app_download`. Additive like
     * the previous migrations: the catalog cache is disposable but user state is not.
     */
    internal val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(connection, "ALTER TABLE app ADD COLUMN targetSdk INTEGER")
            execSql(connection, "ALTER TABLE app ADD COLUMN compileSdk INTEGER")
            execSql(connection, "ALTER TABLE app ADD COLUMN localeCount INTEGER")
            execSql(connection, "ALTER TABLE app ADD COLUMN abis TEXT NOT NULL DEFAULT '[]'")
            execSql(connection, "ALTER TABLE app_download ADD COLUMN targetSdk INTEGER")
            execSql(connection, "ALTER TABLE app_download ADD COLUMN compileSdk INTEGER")
            execSql(
                connection,
                "ALTER TABLE app_download ADD COLUMN locales TEXT NOT NULL DEFAULT '[]'"
            )
            execSql(
                connection,
                "ALTER TABLE app_download ADD COLUMN abis TEXT NOT NULL DEFAULT '[]'"
            )
            execSql(
                connection,
                "ALTER TABLE app_download ADD COLUMN localizedLabels TEXT NOT NULL DEFAULT '{}'"
            )
            execSql(connection, "ALTER TABLE app_download ADD COLUMN signerDn TEXT")
            execSql(connection, "ALTER TABLE app_download ADD COLUMN signerScheme TEXT")
            execSql(connection, "ALTER TABLE app_download ADD COLUMN signerKeyAlgorithm TEXT")
        }
    }

    /**
     * Shizuku usage classification added to `app`. Additive like the previous
     * migrations: the catalog cache is disposable but user state is not.
     */
    internal val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(connection, "ALTER TABLE app ADD COLUMN managers TEXT NOT NULL DEFAULT '[]'")
            execSql(connection, "ALTER TABLE app ADD COLUMN apiForm TEXT")
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN capabilities TEXT NOT NULL DEFAULT '[]'"
            )
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN usageOptional INTEGER NOT NULL DEFAULT 0"
            )
            execSql(connection, "ALTER TABLE app ADD COLUMN usageSummary TEXT")
            execSql(connection, "ALTER TABLE app ADD COLUMN signals TEXT NOT NULL DEFAULT '[]'")
        }
    }

    /**
     * Locale labels added to `app` so lists can show the APK's own name for the
     * device locale. Additive like the previous migrations.
     */
    internal val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN localizedLabels TEXT NOT NULL DEFAULT '{}'"
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
