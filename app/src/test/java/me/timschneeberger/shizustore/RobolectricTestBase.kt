/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore

import android.os.Build
import androidx.room.Room
import me.timschneeberger.shizustore.data.repository.UpdateStateRepository
import me.timschneeberger.shizustore.data.room.ShizuStoreDatabase
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
abstract class RobolectricTestBase {

    protected lateinit var db: ShizuStoreDatabase

    @Before
    fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ShizuStoreDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    protected fun updateStateRepository() =
        UpdateStateRepository(db, db.appDao(), db.appDownloadDao(), db.installedDao())
}
