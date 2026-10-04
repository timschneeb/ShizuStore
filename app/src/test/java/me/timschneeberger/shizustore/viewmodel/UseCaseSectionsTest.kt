/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.entity.UseCaseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCaseSectionsTest {

    @Test
    fun membershipComesFromTheAppsOwnTagLists() {
        val sections = UseCaseSections.build(
            useCases = listOf(
                useCase("install-apps", "Install apps"),
                useCase("file-access", "Files")
            ),
            apps = listOf(
                app("a", useCases = listOf("install-apps")),
                app("b", useCases = listOf("install-apps", "file-access")),
                app("c", useCases = listOf("file-access")),
                app("d", useCases = listOf("install-apps")),
                app("e", useCases = listOf("install-apps"))
            ),
            useInstallCounts = false
        )

        assertEquals(listOf("install-apps"), sections.map { it.useCase })
        assertEquals(listOf("a", "b", "d", "e"), sections.single().apps.map { it.slug })
    }

    @Test
    fun sectionsFollowTheServerOrderAndAreCapped() {
        val useCases = (1..6).map { useCase("uc-$it", "Use case $it") }
        val apps = useCases.flatMap { useCase ->
            (1..4).map { app("${useCase.slug}-$it", useCases = listOf(useCase.slug)) }
        }

        val sections = UseCaseSections.build(useCases, apps, useInstallCounts = false)

        assertEquals(4, sections.size)
        assertEquals(listOf("uc-1", "uc-2", "uc-3", "uc-4"), sections.map { it.useCase })
        assertTrue(sections.all { it.kind == AppGroupKind.USE_CASE })
    }

    @Test
    fun useCasesBelowMinAppsAreSkipped() {
        val sections = UseCaseSections.build(
            useCases = listOf(useCase("big", "Big"), useCase("small", "Small")),
            apps = (1..4).map { app("big-$it", useCases = listOf("big")) } +
                (1..2).map { app("small-$it", useCases = listOf("small")) },
            useInstallCounts = false
        )

        assertEquals(listOf("big"), sections.map { it.useCase })
    }

    @Test
    fun appsWithoutUseCasesAreIgnored() {
        val sections = UseCaseSections.build(
            useCases = listOf(useCase("install-apps", "Install apps")),
            apps = (1..4).map { app("tagged-$it", useCases = listOf("install-apps")) } +
                listOf(app("untagged"), app("unknown", useCases = listOf("missing-slug"))),
            useInstallCounts = false
        )

        assertEquals(4, sections.single().apps.size)
    }

    @Test
    fun sectionAppsAreCappedPerSection() {
        val sections = UseCaseSections.build(
            useCases = listOf(useCase("install-apps", "Install apps")),
            apps = (1..25).map { app("a$it", useCases = listOf("install-apps")) },
            useInstallCounts = false
        )

        assertEquals(20, sections.single().apps.size)
    }

    @Test
    fun installCountRanksAppsWhenFlagIsOn() {
        val sections = UseCaseSections.build(
            useCases = listOf(useCase("install-apps", "Install apps")),
            apps = listOf(
                app("a", useCases = listOf("install-apps"), installCount = 10),
                app("b", useCases = listOf("install-apps"), installCount = 40),
                app("c", useCases = listOf("install-apps"), installCount = 30),
                app("d", useCases = listOf("install-apps"), installCount = 20)
            ),
            useInstallCounts = true
        )

        assertEquals(listOf("b", "c", "d", "a"), sections.single().apps.map { it.slug })
    }

    @Test
    fun downloadsRankAppsWithNullsLast() {
        val sections = UseCaseSections.build(
            useCases = listOf(useCase("install-apps", "Install apps")),
            apps = listOf(
                app("a", "Alpha", useCases = listOf("install-apps"), downloadTotal = null),
                app("b", "Beta", useCases = listOf("install-apps"), downloadTotal = 20),
                app("c", "Gamma", useCases = listOf("install-apps"), downloadTotal = null),
                app("d", "Delta", useCases = listOf("install-apps"), downloadTotal = 10)
            ),
            useInstallCounts = false
        )

        assertEquals(listOf("b", "d", "a", "c"), sections.single().apps.map { it.slug })
    }

    @Test
    fun emptyVocabularyProducesNoSections() {
        val sections = UseCaseSections.build(
            useCases = emptyList(),
            apps = (1..4).map { app("a$it", useCases = listOf("install-apps")) },
            useInstallCounts = false
        )

        assertTrue(sections.isEmpty())
    }

    private fun useCase(slug: String, name: String) =
        UseCaseEntity(slug = slug, name = name, appCount = 0, sortOrder = 0)

    private fun app(
        slug: String,
        name: String = slug,
        useCases: List<String> = emptyList(),
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
        useCases = useCases,
        installCount = installCount,
        downloadTotal = downloadTotal
    )
}
