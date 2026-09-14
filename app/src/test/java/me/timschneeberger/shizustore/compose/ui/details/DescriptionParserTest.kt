/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptionParserTest {

    @Test
    fun plainTextStaysASingleTextSegment() {
        val segments = parseDescription("Just a summary")

        assertEquals(listOf(DescriptionSegment.Text("Just a summary")), segments)
    }

    @Test
    fun highlightedCodeBlockIsSplitOutWithItsLanguage() {
        val html = "<p>Intro</p>" +
            "<div class=\"highlight highlight-source-kotlin notranslate position-relative " +
            "overflow-auto\" dir=\"auto\" data-snippet-clipboard-copy-content=\"val x = " +
            "&quot;a&quot;\"><pre class=\"notranslate\"><span class=\"pl-k\">val</span> x = " +
            "<span class=\"pl-s\">&quot;a&quot;</span></pre></div><p>Outro</p>"

        val segments = parseDescription(html)

        assertEquals(3, segments.size)
        assertEquals(DescriptionSegment.Text("<p>Intro</p>"), segments[0])
        assertEquals(DescriptionSegment.Code("kotlin", "val x = \"a\""), segments[1])
        assertEquals(DescriptionSegment.Text("<p>Outro</p>"), segments[2])
    }

    @Test
    fun plainCodeBlockHasNoLanguageAndDecodesEntities() {
        val html = "<div class=\"snippet-clipboard-content notranslate position-relative " +
            "overflow-auto\" data-snippet-clipboard-copy-content=\"a &lt; b\"><pre " +
            "class=\"notranslate\"><code>a &lt; b &amp;&amp; c &gt; d</code></pre></div>"

        val segments = parseDescription(html)

        assertEquals(1, segments.size)
        assertEquals(DescriptionSegment.Code(null, "a < b && c > d"), segments[0])
    }

    @Test
    fun imageIsReplacedByItsAltText() {
        val html = "<p><a href=\"https://f-droid.org\"><img src=\"https://img/badge.png\" " +
            "alt=\"Get it on F-Droid\"></a></p>"

        val text = (parseDescription(html).single() as DescriptionSegment.Text).html

        assertFalse(text.contains('\uFFFC'))
        assertFalse(text.contains("<img"))
        assertTrue(text.contains("Get it on F-Droid"))
    }

    @Test
    fun imageWithoutAltBecomesEmpty() {
        val html = "<p><img src=\"https://img/logo.png\"></p>"

        val segments = parseDescription(html)

        assertTrue(segments.isEmpty())
    }

    @Test
    fun inlineCodeBecomesMonospace() {
        val segments = parseDescription("Use <code>shizuku</code> now")

        assertEquals(DescriptionSegment.Text("Use <tt>shizuku</tt> now"), segments.single())
    }

    @Test
    fun headingAnchorsAndSvgsAreRemoved() {
        val html = "<div class=\"markdown-heading\"><h1>Title</h1><a class=\"anchor\" " +
            "href=\"#title\"><svg viewBox=\"0 0 16 16\"><path d=\"M4 9h1v1H4z\"/></svg></a></div>"

        val text = (parseDescription(html).single() as DescriptionSegment.Text).html

        assertFalse(text.contains("<svg"))
        assertFalse(text.contains("anchor"))
        assertTrue(text.contains("<h1>Title</h1>"))
    }

    @Test
    fun taskListCheckboxesBecomeSymbols() {
        val html = "<ul><li><input type=\"checkbox\" checked disabled> done</li>" +
            "<li><input type=\"checkbox\" disabled> todo</li></ul>"

        val text = (parseDescription(html).single() as DescriptionSegment.Text).html

        assertTrue(text.contains("\u2611"))
        assertTrue(text.contains("\u2610"))
        assertFalse(text.contains("<input"))
    }

    @Test
    fun multipleCodeBlocksAlternateWithText() {
        val html = "<p>One</p><pre>a</pre><p>Two</p><pre>b</pre>"

        val segments = parseDescription(html)

        assertEquals(4, segments.size)
        assertTrue(segments[0] is DescriptionSegment.Text)
        assertEquals(DescriptionSegment.Code(null, "a"), segments[1])
        assertTrue(segments[2] is DescriptionSegment.Text)
        assertEquals(DescriptionSegment.Code(null, "b"), segments[3])
    }

    @Test
    fun decodesNamedAndNumericEntities() {
        assertEquals("A & B < C", decodeHtmlEntities("A &amp; B &lt; C"))
        assertEquals("'", decodeHtmlEntities("&#39;"))
        assertEquals("'", decodeHtmlEntities("&#x27;"))
        assertEquals("A", decodeHtmlEntities("&#65;"))
        assertEquals("&unknown;", decodeHtmlEntities("&unknown;"))
    }

    @Test
    fun isPlainTextOnlyForUntaggedInput() {
        assertTrue(isPlainText("hello"))
        assertTrue(isPlainText("2 < 3 and 5 > 4"))
        assertFalse(isPlainText("<b>hello</b>"))
    }
}
