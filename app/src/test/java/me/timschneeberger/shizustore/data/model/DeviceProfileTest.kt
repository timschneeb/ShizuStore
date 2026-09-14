/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceProfileTest {

    @Test
    fun unknownMinSdkIsCompatible() {
        assertFalse(DeviceProfile.isIncompatible(minSdk = 0, deviceSdk = 33))
    }

    @Test
    fun minSdkAboveDeviceIsIncompatible() {
        assertTrue(DeviceProfile.isIncompatible(minSdk = 34, deviceSdk = 33))
    }

    @Test
    fun minSdkAtOrBelowDeviceIsCompatible() {
        assertFalse(DeviceProfile.isIncompatible(minSdk = 33, deviceSdk = 33))
        assertFalse(DeviceProfile.isIncompatible(minSdk = 21, deviceSdk = 33))
    }
}
