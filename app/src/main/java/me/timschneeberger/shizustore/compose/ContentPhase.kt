/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

/**
 * One of a screen's three states, so screens crossfade the same way and never flash a placeholder
 * over content that is still loading.
 */
enum class ContentPhase { Loading, Empty, Loaded }
