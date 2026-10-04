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
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12
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

    /**
     * AI usage report replaces the marker classification. SQLite is bundled
     * with the app, so DROP COLUMN is available regardless of the device OS.
     */
    internal val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(connection, "ALTER TABLE app DROP COLUMN managers")
            execSql(connection, "ALTER TABLE app DROP COLUMN apiForm")
            execSql(connection, "ALTER TABLE app DROP COLUMN capabilities")
            execSql(connection, "ALTER TABLE app DROP COLUMN usageOptional")
            execSql(connection, "ALTER TABLE app DROP COLUMN usageSummary")
            execSql(connection, "ALTER TABLE app DROP COLUMN signals")
            execSql(connection, "ALTER TABLE app ADD COLUMN usageShort TEXT")
            execSql(connection, "ALTER TABLE app ADD COLUMN usageMarkdown TEXT")
            execSql(connection, "ALTER TABLE app ADD COLUMN usageAnalyzedAt TEXT")
        }
    }

    /**
     * Report format generation for the cached AI usage text. Null rows were
     * fetched before the server added the section headings and refetch once.
     */
    internal val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(connection, "ALTER TABLE app ADD COLUMN usageReportVersion INTEGER")
        }
    }

    /**
     * Blacklist feature removed. Dropping the table is deliberate: the stored
     * package names only fed a UI list that no longer exists.
     */
    internal val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(connection, "DROP TABLE IF EXISTS blacklist")
        }
    }

    /**
     * Ignored-update suppression flag added to `app`. Additive like the previous
     * migrations: the catalog cache is disposable but user state is not.
     */
    internal val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN updateIgnored INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * Detail fetch artifacts moved out of process memory into `app` so reopening
     * a details page composes screenshots, changelog and README from the first
     * emission instead of a burst when the background fetch lands. Additive like
     * the previous migrations: the catalog cache is disposable but user state is not.
     */
    internal val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(
                connection,
                "ALTER TABLE app ADD COLUMN screenshots TEXT NOT NULL DEFAULT '[]'"
            )
            execSql(connection, "ALTER TABLE app ADD COLUMN changelog TEXT")
            execSql(connection, "ALTER TABLE app ADD COLUMN changelogUrl TEXT")
            execSql(connection, "ALTER TABLE app ADD COLUMN readmeUrl TEXT")
            execSql(connection, "ALTER TABLE app ADD COLUMN fullDescription TEXT")
        }
    }

    /**
     * Weekly install window behind the Trending sort. Additive nullable score:
     * rows stay name-ordered until the client refreshes `/v1/trending`.
     */
    internal val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(connection: SQLiteConnection) {
            execSql(connection, "ALTER TABLE app ADD COLUMN trendScore INTEGER")
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
    fun providesIgnoredUpdateDao(db: ShizuStoreDatabase): IgnoredUpdateDao = db.ignoredUpdateDao()
}
