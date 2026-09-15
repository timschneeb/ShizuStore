/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PermissionGroupsTest {
    @Test
    fun mapsLocationPermissionsToLocationGroup() {
        assertEquals(
            "android.permission-group.LOCATION",
            PermissionGroups.groupOfPlatformPermission("android.permission.ACCESS_FINE_LOCATION")
        )
        assertEquals(
            "android.permission-group.LOCATION",
            PermissionGroups.groupOfPlatformPermission("android.permission.ACCESS_COARSE_LOCATION")
        )
    }

    @Test
    fun mapsCameraAndMicrophonePermissions() {
        assertEquals(
            "android.permission-group.CAMERA",
            PermissionGroups.groupOfPlatformPermission("android.permission.CAMERA")
        )
        assertEquals(
            "android.permission-group.MICROPHONE",
            PermissionGroups.groupOfPlatformPermission("android.permission.RECORD_AUDIO")
        )
    }

    @Test
    fun returnsNullForUnknownPermissions() {
        assertNull(PermissionGroups.groupOfPlatformPermission("com.example.CUSTOM"))
    }
}
