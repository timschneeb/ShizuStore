/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** `versionUpdatedAt` drives the Recently updated list, so old rows must gain the column. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app ADD COLUMN versionUpdatedAt TEXT")
    }
}

/** Summaries now carry the primary APK size, shown in list rows instead of "NA". */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app ADD COLUMN size INTEGER")
    }
}

/** `listUpdatedAt` is the list-change clock that drives Recently added. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app ADD COLUMN listUpdatedAt TEXT")
    }
}

/** Developer identity and the APK permission list shown on the details screen. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app ADD COLUMN authorKey TEXT")
        connection.execSQL("ALTER TABLE app ADD COLUMN authorName TEXT")
        connection.execSQL("ALTER TABLE app ADD COLUMN authorUrl TEXT")
        connection.execSQL("ALTER TABLE app ADD COLUMN permissions TEXT NOT NULL DEFAULT ''")
    }
}

/** Friendly source labels ("GitHub", "Play Store") sent by the server. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app ADD COLUMN sourceName TEXT")
    }
}

/**
 * Popularity flag: per-app install counts plus the server mode switch that
 * selects installCount over downloadTotal for the popularity sort.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE app ADD COLUMN installCount INTEGER NOT NULL DEFAULT 0")
        connection.execSQL(
            "ALTER TABLE sync_state ADD COLUMN useInstallCountsForPopularity INTEGER NOT NULL DEFAULT 0"
        )
    }
}
