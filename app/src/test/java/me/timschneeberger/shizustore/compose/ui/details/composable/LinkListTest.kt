/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import java.net.URLEncoder
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.model.AppDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun forgeLinksAreNotSourceCodeForClosedSourceApps() {
        assertFalse(isSourceCodeLink(Listing.CLOSED_SOURCE, "https://github.com/owner/repo"))
        assertFalse(isSourceCodeLink(Listing.CLOSED_SOURCE, "https://gitlab.com/owner/repo"))
        assertTrue(isSourceCodeLink(Listing.CLOSED_SOURCE, "https://example.com/download"))
        assertTrue(isSourceCodeLink(Listing.MAIN, "https://github.com/owner/repo"))
    }

    @Test
    fun obtainiumIsHiddenForLinkOnlyEntries() {
        assertNull(
            obtainiumRepoUrl(
                details(
                    availability = Availability.LINK_ONLY,
                    url = "https://github.com/owner/repo",
                    sourceUrl = "https://github.com/owner/repo"
                )
            )
        )
    }

    @Test
    fun obtainiumPrefersTheSourceUrlOverTheWebsite() {
        assertEquals(
            "https://github.com/owner/repo",
            obtainiumRepoUrl(
                details(
                    url = "https://example.com",
                    sourceUrl = "https://github.com/owner/repo"
                )
            )
        )
    }

    @Test
    fun obtainiumAcceptsForgeAndFDroidUrls() {
        assertEquals(
            "https://gitlab.com/owner/repo",
            obtainiumRepoUrl(details(url = "https://gitlab.com/owner/repo"))
        )
        assertEquals(
            "https://f-droid.org/packages/org.example.app/",
            obtainiumRepoUrl(details(url = "https://f-droid.org/packages/org.example.app/"))
        )
    }

    @Test
    fun obtainiumIgnoresPlainWebsites() {
        assertNull(obtainiumRepoUrl(details(url = "https://example.com")))
        assertNull(
            obtainiumRepoUrl(
                details(url = "https://play.google.com/store/apps/details?id=org.example.app")
            )
        )
        assertNull(obtainiumRepoUrl(details()))
    }

    @Test
    fun obtainiumRedirectEncodesTheDeepLink() {
        val repo = "https://github.com/owner/repo"
        val expected = "https://apps.obtainium.imranr.dev/redirect?r=" +
            URLEncoder.encode("obtainium://add/" + URLEncoder.encode(repo, "UTF-8"), "UTF-8")
        val redirect = obtainiumRedirectUrl(repo)
        assertEquals(expected, redirect)
        assertTrue(redirect.contains("obtainium%3A%2F%2Fadd%2F"))
        assertTrue(redirect.contains("https%253A%252F%252Fgithub.com"))
    }

    private fun details(
        availability: Availability = Availability.DIRECT_APK,
        url: String? = null,
        sourceUrl: String? = null
    ): AppDetails = AppDetails(
        packageName = "org.example.app",
        repoName = "GitHub",
        name = "Example",
        description = "",
        iconUrl = null,
        license = "GPL-3.0",
        authorName = null,
        changelog = null,
        categories = emptyList(),
        screenshots = emptyList(),
        lastUpdated = 0L,
        versionName = "1.0",
        size = 0L,
        minSdk = 24,
        permissions = emptyList(),
        installedVersionCode = null,
        availability = availability,
        url = url,
        sourceUrl = sourceUrl
    )
}
