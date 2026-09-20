/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuInstallerSourceTest {

    @Test
    fun disabledCustomSourceKeepsAppPackage() {
        assertEquals(
            APP_PACKAGE,
            effectiveInstallerSourcePackage(
                fallbackPackage = APP_PACKAGE,
                customSourceEnabled = false,
                customSourcePackage = "com.android.vending"
            )
        )
    }

    @Test
    fun enabledCustomSourceOverridesAppPackage() {
        assertEquals(
            "com.android.vending",
            effectiveInstallerSourcePackage(
                fallbackPackage = APP_PACKAGE,
                customSourceEnabled = true,
                customSourcePackage = "com.android.vending"
            )
        )
    }

    @Test
    fun blankCustomSourceFallsBackToAppPackage() {
        assertEquals(
            APP_PACKAGE,
            effectiveInstallerSourcePackage(
                fallbackPackage = APP_PACKAGE,
                customSourceEnabled = true,
                customSourcePackage = "  "
            )
        )
    }

    private companion object {
        const val APP_PACKAGE = "me.timschneeberger.shizustore"
    }
}
