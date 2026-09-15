/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkListTest {
    @Test
    fun recognizesForgeHosts() {
        assertTrue(isForgeRepositoryUrl("https://github.com/owner/repo"))
        assertTrue(isForgeRepositoryUrl("https://GitLab.com/owner/repo"))
        assertTrue(isForgeRepositoryUrl("https://codeberg.org/owner/repo"))
    }

    @Test
    fun recognizesForgeSubdomainsAndCredentials() {
        assertTrue(isForgeRepositoryUrl("https://www.github.com/owner/repo"))
        assertTrue(isForgeRepositoryUrl("https://user@gitlab.com/owner/repo"))
    }

    @Test
    fun rejectsNonForgeAndBareHosts() {
        assertFalse(isForgeRepositoryUrl("https://example.com/owner/repo"))
        assertFalse(isForgeRepositoryUrl("https://notgithub.com/owner/repo"))
        assertFalse(isForgeRepositoryUrl("github.com/owner/repo"))
    }

    @Test
    fun recognizesFDroidHosts() {
        assertTrue(isFDroidUrl("https://f-droid.org/packages/org.example.app/"))
        assertTrue(isFDroidUrl("https://f-droid.org/en/packages/org.example.app/"))
        assertTrue(isFDroidUrl("https://staging.f-droid.org/packages/org.example.app/"))
    }

    @Test
    fun rejectsNonFDroidHosts() {
        assertFalse(isFDroidUrl("https://example.com/packages/org.example.app/"))
        assertFalse(isFDroidUrl("https://notf-droid.org/packages/org.example.app/"))
        assertFalse(isFDroidUrl("f-droid.org/packages/org.example.app/"))
    }
}
