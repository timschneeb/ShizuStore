/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusStoreTest {

    @Test
    fun refreshIconsIncrementsGeneration() {
        val store = SyncStatusStore()

        assertEquals(0, store.iconGeneration.value)
        store.refreshIcons()
        store.refreshIcons()
        assertEquals(2, store.iconGeneration.value)
    }
}
