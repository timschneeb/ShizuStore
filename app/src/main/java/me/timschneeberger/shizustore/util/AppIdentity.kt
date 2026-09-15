/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

/**
 * Package name this store is published under. The catalog entry for it is hidden from
 * browsing and is the only target a build may replace in place. It is the base id, not
 * [BuildConfig.APPLICATION_ID], so debug and nightly builds also hide the published entry.
 */
const val SHIZU_STORE_PACKAGE = "me.timschneeberger.shizustore"
