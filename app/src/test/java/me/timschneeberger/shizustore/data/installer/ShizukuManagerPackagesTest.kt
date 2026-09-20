/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

import android.content.Context
import android.content.pm.PermissionInfo
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rikka.shizuku.ShizukuProvider

/** Detection must not depend on the stock package name: forks declare the same permissions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class ShizukuManagerPackagesTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    // PermissionInfo has no non-deprecated constructor taking an owner package; the copy
    // constructor would still need this instance first.
    @Suppress("DEPRECATION")
    private fun declarePermission(permission: String, ownerPackage: String) {
        shadowOf(context.packageManager).addPermissionInfo(
            PermissionInfo().apply {
                name = permission
                packageName = ownerPackage
            }
        )
    }

    @Test
    fun stockPermissionOwnerIsDetected() {
        declarePermission(ShizukuProvider.PERMISSION, ShizukuProvider.MANAGER_APPLICATION_ID)

        assertEquals(
            listOf(ShizukuProvider.MANAGER_APPLICATION_ID),
            ShizukuInstaller.managerPackages(context)
        )
        assertTrue(ShizukuInstaller.isAvailable(context))
    }

    @Test
    fun forkPermissionOwnerIsDetected() {
        declarePermission(ShizukuInstaller.SHIZUKU_PLUS_PERMISSION, "af.shizuku.plus.api")

        assertEquals(listOf("af.shizuku.plus.api"), ShizukuInstaller.managerPackages(context))
        assertTrue(ShizukuInstaller.isAvailable(context))
    }

    @Test
    fun renamedForkDeclaringTheStockPermissionIsDetected() {
        declarePermission(ShizukuProvider.PERMISSION, "com.example.shizuku.fork")

        assertEquals(listOf("com.example.shizuku.fork"), ShizukuInstaller.managerPackages(context))
        assertTrue(ShizukuInstaller.isAvailable(context))
    }

    @Test
    fun bothPermissionsAreListedStockFirst() {
        declarePermission(ShizukuInstaller.SHIZUKU_PLUS_PERMISSION, "af.shizuku.plus.api")
        declarePermission(ShizukuProvider.PERMISSION, ShizukuProvider.MANAGER_APPLICATION_ID)

        assertEquals(
            listOf(ShizukuProvider.MANAGER_APPLICATION_ID, "af.shizuku.plus.api"),
            ShizukuInstaller.managerPackages(context)
        )
    }

    @Test
    fun samePackageDeclaringBothPermissionsIsListedOnce() {
        declarePermission(ShizukuProvider.PERMISSION, ShizukuProvider.MANAGER_APPLICATION_ID)
        declarePermission(
            ShizukuInstaller.SHIZUKU_PLUS_PERMISSION,
            ShizukuProvider.MANAGER_APPLICATION_ID
        )

        assertEquals(
            listOf(ShizukuProvider.MANAGER_APPLICATION_ID),
            ShizukuInstaller.managerPackages(context)
        )
    }

    @Test
    fun unrelatedPermissionOwnerIsIgnored() {
        declarePermission("com.example.unrelated.permission.API_V23", "com.example.unrelated")

        assertTrue(ShizukuInstaller.managerPackages(context).isEmpty())
    }

    @Test
    fun noManagerPermissionMeansUnavailable() {
        assertTrue(ShizukuInstaller.managerPackages(context).isEmpty())
        assertFalse(ShizukuInstaller.isAvailable(context))
    }
}
