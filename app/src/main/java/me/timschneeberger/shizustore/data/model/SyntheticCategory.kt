/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

/**
 * Client-side categories that have no row in the server category tree. Their
 * slugs are reserved: the query builder maps them to a flag filter and the UI
 * resolves their localized names.
 */
object SyntheticCategory {

    const val DHIZUKU_SLUG = "dhizuku"

    fun isDhizuku(slug: String?): Boolean = slug == DHIZUKU_SLUG
}
