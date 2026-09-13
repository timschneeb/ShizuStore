/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.api.CategorySection
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTagTreeTest {

    @Test
    fun containersKeepTheirChildrenAndLeavesDoNot() {
        val tree = CategoryTagTree.build(
            listOf(
                category("audio", "Audio", count = 3),
                category("vendor-specific", "Vendor-specific", count = 9),
                category("vendor-miui", "MIUI", parent = "vendor-specific", count = 4),
                category("vendor-pixel", "Google Pixel", parent = "vendor-specific", count = 5)
            )
        )

        assertEquals(listOf("audio", "vendor-specific"), tree.map { it.slug })
        assertFalse(tree.first { it.slug == "audio" }.isContainer)

        val container = tree.first { it.slug == "vendor-specific" }
        assertTrue(container.isContainer)
        assertEquals(listOf("vendor-pixel", "vendor-miui"), container.children.map { it.slug })
        assertEquals(5, container.children.first().appCount)
    }

    @Test
    fun orderIsSortOrderThenName() {
        val tree = CategoryTagTree.build(
            listOf(
                category("b", "Beta", order = 2),
                category("a", "Alpha", order = 1),
                category("z", "Zeta", order = 1)
            )
        )

        assertEquals(listOf("a", "z", "b"), tree.map { it.slug })
    }

    private fun category(
        slug: String,
        name: String = slug,
        parent: String? = null,
        count: Int = 0,
        order: Int = 0
    ) = CategoryEntity(
        slug = slug,
        name = name,
        section = CategorySection.APPS,
        parentSlug = parent,
        appCount = count,
        sortOrder = order
    )
}
