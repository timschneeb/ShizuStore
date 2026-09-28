/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryStoreTest {

    @Test
    fun liveTypingCollapsesToTheLongestQuery() {
        val history = listOf("lin", "link", "linksheet")
            .fold(emptyList<String>(), SearchHistoryStore.Companion::merge)

        assertEquals(listOf("linksheet"), history)
    }

    @Test
    fun backspacingToAPrefixKeepsTheRememberedQuery() {
        assertEquals(listOf("linksheet"), SearchHistoryStore.merge(listOf("linksheet"), "links"))
        assertEquals(listOf("linksheet"), SearchHistoryStore.merge(listOf("linksheet"), "link"))
    }

    @Test
    fun unrelatedSearchesAreBothKeptNewestFirst() {
        assertEquals(
            listOf("linksheet", "monica"),
            SearchHistoryStore.merge(listOf("monica"), "linksheet")
        )
    }

    @Test
    fun repeatingASearchMovesItToTheFront() {
        assertEquals(
            listOf("linksheet", "monica"),
            SearchHistoryStore.merge(listOf("monica", "linksheet"), "linksheet")
        )
    }

    @Test
    fun olderUnrelatedEntriesFallOffTheCap() {
        val history = listOf("three", "two", "one", "zero")
            .fold(emptyList<String>(), SearchHistoryStore.Companion::merge)

        assertEquals(SearchHistoryStore.MAX_ENTRIES, history.size)
        assertEquals(listOf("zero", "one", "two"), history)
    }

    @Test
    fun decodePrunesLegacyPrefixChains() {
        assertEquals(
            listOf("linksheet", "monica"),
            SearchHistoryStore.prune(listOf("lin", "link", "linksheet", "monica"))
        )
    }

    @Test
    fun prefixChecksIgnoreCase() {
        assertEquals(listOf("LinkSheet"), SearchHistoryStore.merge(listOf("LinkSheet"), "link"))
        assertEquals(listOf("linksheet"), SearchHistoryStore.merge(listOf("Link"), "linksheet"))
    }
}
