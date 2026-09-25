/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import me.timschneeberger.shizustore.data.model.CategoryTag
import me.timschneeberger.shizustore.data.model.ResolvedApp
import org.junit.Assert.assertEquals
import org.junit.Test

private const val DHIZUKU_TITLE = "Dhizuku-compatible"

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
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
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
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
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
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
        )

        assertEquals(listOf("large", "medium", "small"), sections.map { it.category })
    }

    @Test
    fun sectionsAreCappedPerSection() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = apps("tools", 25),
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
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
            useInstallCounts = true,
            dhizukuTitle = DHIZUKU_TITLE
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
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
        )

        assertEquals(listOf("b", "d", "a", "c"), sections.single().apps.map { it.slug })
    }

    @Test
    fun appsWithoutCategoryAreIgnored() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = apps("tools", 4) + app("orphan", category = null),
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
        )

        assertEquals(4, sections.single().apps.size)
    }

    @Test
    fun dhizukuSectionCollectsDeclaredAppsAcrossCategories() {
        val sections = CategorySections.build(
            categories = emptyList(),
            apps = listOf(
                app("a", category = "tools", dhizukuDeclared = true),
                app("b", category = "games", dhizukuDeclared = true),
                app("c", category = "tools")
            ),
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
        )

        val section = sections.single()
        assertEquals("dhizuku", section.category)
        assertEquals(DHIZUKU_TITLE, section.title)
        assertEquals(listOf("a", "b"), section.apps.map { it.slug })
    }

    @Test
    fun dhizukuSectionIsOmittedWithoutDeclaredApps() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = apps("tools", 4),
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
        )

        assertEquals(listOf("tools"), sections.map { it.category })
    }

    @Test
    fun dhizukuSectionRanksWithCatalogSectionsByMemberCount() {
        val sections = CategorySections.build(
            categories = listOf(tag("tools", "Tools")),
            apps = apps("tools", 6) + listOf(
                app("d1", dhizukuDeclared = true),
                app("d2", dhizukuDeclared = true),
                app("d3", dhizukuDeclared = true)
            ),
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
        )

        assertEquals(listOf("tools", "dhizuku"), sections.map { it.category })
    }

    @Test
    fun dhizukuSectionIsCappedPerSection() {
        val sections = CategorySections.build(
            categories = emptyList(),
            apps = (1..25).map { app("d$it", dhizukuDeclared = true) },
            useInstallCounts = false,
            dhizukuTitle = DHIZUKU_TITLE
        )

        assertEquals(20, sections.single().apps.size)
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
        downloadTotal: Long? = null,
        dhizukuDeclared: Boolean = false
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
        downloadTotal = downloadTotal,
        dhizukuDeclared = dhizukuDeclared
    )
}
