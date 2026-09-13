/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

/**
 * Which of a screen's three states is on show, so every screen crossfades between the same three
 * and none of them can flash a placeholder at content that is merely still loading.
 */
enum class ContentPhase { Loading, Empty, Loaded }
