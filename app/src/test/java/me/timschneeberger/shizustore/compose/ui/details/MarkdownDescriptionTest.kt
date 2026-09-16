/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import me.timschneeberger.shizustore.data.api.SourceKind

private const val BASE = "https://raw.githubusercontent.com/o/r/HEAD/"

class MarkdownDescriptionTest {
    @Test
    fun detectsLegacyRenderedReadme() {
        val legacy = "<div id=\"readme\" class=\"md\" data-path=\"README.md\"><h1>x</h1></div>"
        assertTrue(isRenderedHtml(legacy))
    }

    @Test
    fun rawMarkdownIsNotHtml() {
        val markdown = "<p align=\"center\">\n<img src=\"https://x/y.png\"/>\n</p>\n\n# Title"
        assertFalse(isRenderedHtml(markdown))
    }

    @Test
    fun plainPlayDescriptionIsNotHtml() {
        assertFalse(isRenderedHtml("Just a plain description."))
    }

    @Test
    fun convertsHtmlImageInsideParagraph() {
        val html = """
            <p align="center">
            <img width="512px" alt="Morphe" src="https://x.test/morphe.svg" />
            </p>
        """.trimIndent()

        val normalized = normalizeReadmeImages(html, null)

        assertTrue(normalized.contains("![Morphe](https://x.test/morphe.svg)"))
        assertFalse(normalized.contains("<img"))
    }

    @Test
    fun htmlImageWithoutAltStillConverts() {
        val normalized = normalizeReadmeImages("<img src=\"https://x.test/a.png\"/>", null)
        assertTrue(normalized.contains("![](https://x.test/a.png)"))
    }

    @Test
    fun resolvesRelativeMarkdownImage() {
        val normalized = normalizeReadmeImages("![icon](assets/icon.png)", BASE)
        assertEquals("![icon](${BASE}assets/icon.png)", normalized)
    }

    @Test
    fun resolvesRelativeHtmlImageAfterConversion() {
        val normalized = normalizeReadmeImages("<img src=\"./docs/a.png\" alt=\"a\">", BASE)
        assertEquals("![a](${BASE}docs/a.png)", normalized.trim())
    }

    @Test
    fun leavesAbsoluteAndDataImagesUntouched() {
        val markdown = "![a](https://cdn.example/a.png) ![b](data:image/png;base64,AAAA)"
        assertEquals(markdown, normalizeReadmeImages(markdown, BASE))
    }

    @Test
    fun derivesGithubRawBase() {
        assertEquals(BASE, githubRawBase("https://github.com/o/r"))
        assertEquals(BASE, githubRawBase("https://github.com/o/r.git"))
        assertNull(githubRawBase("https://morphe.software/"))
        assertNull(githubRawBase(null))
    }

    @Test
    fun indexRepoChangelogsRenderAsHtml() {
        assertTrue(changelogRendersAsHtml(SourceKind.FDROID))
        assertTrue(changelogRendersAsHtml(SourceKind.IZZY))
        assertFalse(changelogRendersAsHtml(SourceKind.GITHUB))
        assertFalse(changelogRendersAsHtml(SourceKind.GITLAB))
        assertFalse(changelogRendersAsHtml(null))
    }
}
