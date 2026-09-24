/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import me.timschneeberger.shizustore.data.model.CategoryTag
import me.timschneeberger.shizustore.data.model.ResolvedApp
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySectionsTest {

    @Test
    fun membershipIncludesDescendants() {
        val sections = CategorySections.build(
            categories = listOf(
                tag("tools", "Tools", children = listOf(tag("shell", "Shell")))
            ),
            apps = listOf(
                app("a", category = "tools"),
                app("b", category = "shell"),
                app("c", category = "shell"),
                app("d", category = "tools")
            ),
            useInstallCounts = false
        )

        assertEquals(listOf("tools"), sections.map { it.category })
        assertEquals(listOf("a", "b", "c", "d"), sections.single().apps.map { it.slug })
        assertEquals("Tools", sections.single().title)
    }

    @Test
    fun categoriesBelowMinAppsAreSkipped() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools"), tag("games", "Games")),
            apps = listOf(
                app("a", category = "tools"),
                app("b", category = "tools"),
                app("c", category = "tools"),
                app("d", category = "games"),
                app("e", category = "games"),
                app("f", category = "games"),
                app("g", category = "games")
            ),
            useInstallCounts = false
        )

        assertEquals(listOf("games"), sections.map { it.category })
    }

    @Test
    fun sectionsOrderByMemberCountThenName() {
        val sections = CategorySections.build(
            categories = listOf(
                tag("small", "Zeta"),
                tag("large", "Beta"),
                tag("medium", "Alpha")
            ),
            apps = apps("small", 4) + apps("large", 6) + apps("medium", 4),
            useInstallCounts = false
        )

        assertEquals(listOf("large", "medium", "small"), sections.map { it.category })
    }

    @Test
    fun sectionsAreCappedPerSection() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = apps("tools", 25),
            useInstallCounts = false
        )

        assertEquals(20, sections.single().apps.size)
    }

    @Test
    fun installCountRanksSectionsWhenFlagIsOn() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = listOf(
                app("a", category = "tools", installCount = 10),
                app("b", category = "tools", installCount = 40),
                app("c", category = "tools", installCount = 30),
                app("d", category = "tools", installCount = 20)
            ),
            useInstallCounts = true
        )

        assertEquals(listOf("b", "c", "d", "a"), sections.single().apps.map { it.slug })
    }

    @Test
    fun downloadsRankSectionsWithNullsLast() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = listOf(
                app("a", "Alpha", category = "tools", downloadTotal = null),
                app("b", "Beta", category = "tools", downloadTotal = 20),
                app("c", "Gamma", category = "tools", downloadTotal = null),
                app("d", "Delta", category = "tools", downloadTotal = 10)
            ),
            useInstallCounts = false
        )

        assertEquals(listOf("b", "d", "a", "c"), sections.single().apps.map { it.slug })
    }

    @Test
    fun appsWithoutCategoryAreIgnored() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = apps("tools", 4) + app("orphan", category = null),
            useInstallCounts = false
        )

        assertEquals(4, sections.single().apps.size)
    }

    private fun apps(category: String, count: Int): List<ResolvedApp> =
        (1..count).map { app("$category-$it", category = category) }

    private fun tag(
        slug: String,
        name: String,
        children: List<CategoryTag> = emptyList()
    ) = CategoryTag(slug = slug, name = name, appCount = 0, children = children)

    private fun app(
        slug: String,
        name: String = slug,
        category: String? = null,
        installCount: Long = 0,
        downloadTotal: Long? = null
    ) = ResolvedApp(
        packageName = "pkg.$slug",
        repoName = "GitHub",
        name = name,
        summary = "",
        iconUrl = null,
        versionCode = 1L,
        versionName = "1.0",
        signer = null,
        size = 0L,
        minSdk = 24,
        installedVersionCode = null,
        installedSigner = null,
        slug = slug,
        categorySlug = category,
        installCount = installCount,
        downloadTotal = downloadTotal
    )
}
