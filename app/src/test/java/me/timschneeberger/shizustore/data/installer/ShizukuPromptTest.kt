/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuPromptTest {

    @Test
    fun missingShizukuAsksToInstall() {
        assertEquals(ShizukuPrompt.INSTALL, shizukuPrompt(available = false, permitted = false))
    }

    @Test
    fun installedWithoutPermissionAsksToGrant() {
        assertEquals(
            ShizukuPrompt.GRANT_PERMISSION,
            shizukuPrompt(available = true, permitted = false)
        )
    }

    @Test
    fun installedAndPermittedIsReady() {
        assertEquals(ShizukuPrompt.READY, shizukuPrompt(available = true, permitted = true))
    }

    @Test
    fun missingStatusWinsOverPermission() {
        assertEquals(ShizukuPrompt.INSTALL, shizukuPrompt(available = false, permitted = true))
    }
}
