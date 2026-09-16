/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

internal const val PAGE_SIZE = 30

/** Shared `WhileSubscribed` timeout, long enough to survive a configuration change. */
internal const val STOP_TIMEOUT_MS = 5_000L
