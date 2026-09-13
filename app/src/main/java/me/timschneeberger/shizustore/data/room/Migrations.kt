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
