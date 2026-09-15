/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.work

import me.timschneeberger.shizustore.util.SHIZU_STORE_PACKAGE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfUpdateGuardTest {

    @Test
    fun skipsWhenInstalledAlreadyMatchesTheRow() {
        assertTrue(isSelfUpdateSatisfied(SHIZU_STORE_PACKAGE, 42L, 42L))
    }

    @Test
    fun dispatchesWhenInstalledIsOlderThanTheRow() {
        assertFalse(isSelfUpdateSatisfied(SHIZU_STORE_PACKAGE, 42L, 41L))
    }

    @Test
    fun ignoresOtherPackages() {
        assertFalse(isSelfUpdateSatisfied("com.example.other", 42L, 42L))
    }

    @Test
    fun ignoresMissingOrUnsetRowVersion() {
        assertFalse(isSelfUpdateSatisfied(SHIZU_STORE_PACKAGE, null, null))
        assertFalse(isSelfUpdateSatisfied(SHIZU_STORE_PACKAGE, 0L, 0L))
    }
}
