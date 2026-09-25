/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalizedLabelTest {

    @Test
    fun normalizesAapt2Qualifiers() {
        assertEquals("de-de", normalizeLocaleQualifier("de-rDE"))
        assertEquals("zh-hans", normalizeLocaleQualifier("b+zh+Hans"))
        assertEquals("en-us", normalizeLocaleQualifier("en_US"))
        assertEquals("pt-br", normalizeLocaleQualifier(" pt-BR "))
        assertNull(normalizeLocaleQualifier("   "))
    }

    @Test
    fun exactTagWinsOverLanguage() {
        val labels = mapOf("de" to "Deutsch", "en-US" to "English")

        assertEquals("English", pickLocalizedLabel(labels, listOf("en-US")))
    }

    @Test
    fun scriptAndRegionFallBackToLanguage() {
        assertEquals("简体", pickLocalizedLabel(mapOf("zh-Hans" to "简体"), listOf("zh-Hans-CN")))
        assertEquals("Português", pickLocalizedLabel(mapOf("pt" to "Português"), listOf("pt-BR")))
        assertNull(pickLocalizedLabel(mapOf("pt-BR" to "Português"), listOf("pt-PT")))
    }

    @Test
    fun deviceLocaleOrderDecides() {
        val labels = mapOf("de" to "Deutsch", "en" to "English")

        assertEquals("Deutsch", pickLocalizedLabel(labels, listOf("de-AT", "en-US")))
    }

    @Test
    fun keyReportedMatchesPickedLabel() {
        val labels = mapOf("b+zh+Hans" to "简体")

        assertEquals("zh-hans", pickLocalizedLabelKey(labels, listOf("zh-Hans-CN")))
        assertEquals("简体", pickLocalizedLabel(labels, listOf("zh-Hans-CN")))
    }

    @Test
    fun blankEntriesAndUnknownLocalesYieldNothing() {
        assertNull(pickLocalizedLabel(emptyMap(), listOf("en")))
        assertNull(pickLocalizedLabel(mapOf("fr" to " "), listOf("fr")))
        assertNull(pickLocalizedLabel(mapOf("fr" to "Français"), listOf("ja")))
    }
}
