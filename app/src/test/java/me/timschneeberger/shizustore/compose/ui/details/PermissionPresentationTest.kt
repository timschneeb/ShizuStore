/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import android.content.pm.PermissionInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPresentationTest {
    @Test
    fun detectsDangerousProtection() {
        assertTrue(isDangerousProtection(PermissionInfo.PROTECTION_DANGEROUS))
        assertTrue(
            isDangerousProtection(
                PermissionInfo.PROTECTION_DANGEROUS or PermissionInfo.PROTECTION_FLAG_APPOP
            )
        )
    }

    @Test
    fun ignoresNonDangerousProtection() {
        assertFalse(isDangerousProtection(PermissionInfo.PROTECTION_NORMAL))
        assertFalse(isDangerousProtection(PermissionInfo.PROTECTION_SIGNATURE))
        assertFalse(isDangerousProtection(PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM))
    }

    @Test
    fun formatsCustomPermissionName() {
        assertEquals(
            "Read External Storage",
            customPermissionName("com.example.READ_EXTERNAL_STORAGE")
        )
    }
}
