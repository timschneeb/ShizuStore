/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SyntheticCategoryTest {

    @Test
    fun insertDhizukuAddsLeafAtAlphabeticalPosition() {
        val tree = listOf(
            CategoryTag(slug = "audio", name = "Audio", appCount = 3),
            CategoryTag(slug = "display", name = "Display", appCount = 2),
            CategoryTag(slug = "network", name = "Network", appCount = 1)
        )

        val merged = SyntheticCategory.insertDhizuku(tree, "Dhizuku-compatible")

        assertEquals(listOf("audio", "dhizuku", "display", "network"), merged.map { it.slug })
        val dhizuku = merged[1]
        assertEquals("Dhizuku-compatible", dhizuku.name)
        assertFalse(dhizuku.isContainer)
        assertEquals(0, dhizuku.appCount)
        assertEquals(merged, merged.flatten())
    }

    @Test
    fun insertDhizukuAppendsWhenTitleSortsLast() {
        val tree = listOf(
            CategoryTag(slug = "audio", name = "Audio", appCount = 3),
            CategoryTag(slug = "network", name = "Network", appCount = 1)
        )

        val merged = SyntheticCategory.insertDhizuku(tree, "Zeta")

        assertEquals(listOf("audio", "network", "dhizuku"), merged.map { it.slug })
    }

    @Test
    fun insertDhizukuHandlesAnEmptyTree() {
        val merged = SyntheticCategory.insertDhizuku(emptyList(), "Dhizuku-compatible")

        assertEquals(listOf("dhizuku"), merged.map { it.slug })
    }
}
