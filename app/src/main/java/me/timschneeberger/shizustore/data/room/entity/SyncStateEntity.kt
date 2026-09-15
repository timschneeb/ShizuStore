/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val cursor: String? = null,
    val categoriesEtag: String? = null,
    val listCommit: String? = null,
    val syncedAt: Long = 0L,
    /** Server popularity mode from /v1/meta: sort by installCount, not downloadTotal. */
    val useInstallCountsForPopularity: Boolean = false
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
