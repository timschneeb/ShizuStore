/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import android.os.Build
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class ShizuStoreDatabaseMigrationTest {

    @Test
    fun migrationMapsFavouritesToASlugAndAddsDownloadProvenance() {
        val file = File(
            RuntimeEnvironment.getApplication().cacheDir,
            "migration-${UUID.randomUUID()}.db"
        )
        val connection = AndroidSQLiteDriver().open(file.absolutePath)
        try {
            execSql(connection, "CREATE TABLE app (slug TEXT NOT NULL, packageName TEXT)")
            execSql(
                connection,
                "CREATE TABLE app_download (appSlug TEXT NOT NULL, packageName TEXT)"
            )
            execSql(connection, "CREATE TABLE favourite (packageName TEXT NOT NULL)")
            execSql(connection, "CREATE TABLE download (packageName TEXT NOT NULL)")
            execSql(connection, "INSERT INTO app VALUES ('fork-b', 'com.shared')")
            execSql(connection, "INSERT INTO app VALUES ('fork-a', 'com.shared')")
            execSql(connection, "INSERT INTO favourite VALUES ('com.shared')")
            execSql(connection, "INSERT INTO download VALUES ('com.shared')")

            RoomModule.MIGRATION_12_13.migrate(connection)

            querySingleText(connection, "SELECT slug FROM favourite").let {
                assertEquals("fork-a", it)
            }
            querySingleText(connection, "SELECT appSlug FROM download").let {
                assertNull(it)
            }
        } finally {
            connection.close()
            file.delete()
        }
    }

    private fun querySingleText(connection: SQLiteConnection, sql: String): String? {
        val statement = connection.prepare(sql)
        return try {
            assertTrue(statement.step())
            if (statement.isNull(0)) null else statement.getText(0)
        } finally {
            statement.close()
        }
    }

    private fun execSql(connection: SQLiteConnection, sql: String) {
        val statement = connection.prepare(sql)
        try {
            statement.step()
        } finally {
            statement.close()
        }
    }
}
