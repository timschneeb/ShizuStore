/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import me.timschneeberger.shizustore.data.model.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallReconcilerTest {

    @Test
    fun settlesInstalledWhenTheRowVersionHasLanded() {
        assertEquals(
            DownloadStatus.INSTALLED,
            strandedInstallOutcome(
                rowVersionCode = 42L,
                installedVersionCode = 42L,
                dispatchedInThisProcess = true
            )
        )
    }

    @Test
    fun leavesAnInFlightInstallFromThisProcessAlone() {
        assertNull(
            strandedInstallOutcome(
                rowVersionCode = 42L,
                installedVersionCode = 41L,
                dispatchedInThisProcess = true
            )
        )
    }

    @Test
    fun failsAnInstallThatDidNotLandAndWasNotDispatchedHere() {
        assertEquals(
            DownloadStatus.FAILED,
            strandedInstallOutcome(
                rowVersionCode = 42L,
                installedVersionCode = null,
                dispatchedInThisProcess = false
            )
        )
    }
}
