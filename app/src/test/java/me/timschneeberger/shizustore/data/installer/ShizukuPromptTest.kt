/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuPromptTest {

    @Test
    fun missingPackageAsksToInstall() {
        assertEquals(
            ShizukuPrompt.INSTALL,
            shizukuPrompt(available = false, running = false, permitted = false)
        )
    }

    @Test
    fun stoppedServiceAsksToStartBeforeGranting() {
        assertEquals(
            ShizukuPrompt.START,
            shizukuPrompt(available = true, running = false, permitted = false)
        )
        assertEquals(
            ShizukuPrompt.START,
            shizukuPrompt(available = true, running = false, permitted = true)
        )
    }

    @Test
    fun runningServiceWithoutGrantAsksToGrant() {
        assertEquals(
            ShizukuPrompt.GRANT_PERMISSION,
            shizukuPrompt(available = true, running = true, permitted = false)
        )
    }

    @Test
    fun runningGrantedServiceIsReady() {
        assertEquals(
            ShizukuPrompt.READY,
            shizukuPrompt(available = true, running = true, permitted = true)
        )
    }
}
