/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.entity.UseCaseEntity

/**
 * Builds the home USE_CASE rows from the server-curated vocabulary. Sections are
 * ordered by the server's app count, but membership comes from the synced apps
 * themselves so a use case whose apps were filtered away produces no header.
 * Capped so the curated and category rows stay reachable.
 */
object UseCaseSections {

    const val MIN_APPS = 4
    const val MAX_SECTIONS = 4
    const val ITEM_LIMIT = 20

    fun build(
        useCases: List<UseCaseEntity>,
        apps: List<ResolvedApp>,
        useInstallCounts: Boolean,
        minApps: Int = MIN_APPS,
        maxSections: Int = MAX_SECTIONS,
        itemLimit: Int = ITEM_LIMIT
    ): List<AppGroup> {
        val byUseCase = apps
            .filter { it.useCases.isNotEmpty() }
            .flatMap { app -> app.useCases.map { it to app } }
            .groupBy({ it.first }, { it.second })

        return useCases
            .mapNotNull { useCase ->
                byUseCase[useCase.slug]?.takeIf { it.size >= minApps }?.let { members ->
                    AppGroup(
                        kind = AppGroupKind.USE_CASE,
                        apps = members.sortedWith(popularity(useInstallCounts)).take(itemLimit),
                        useCase = useCase.slug,
                        title = useCase.name
                    )
                }
            }
            .take(maxSections)
    }

    private fun popularity(useInstallCounts: Boolean): Comparator<ResolvedApp> {
        val primary = if (useInstallCounts) {
            compareByDescending<ResolvedApp> { it.installCount }
        } else {
            // Nulls last: unknown totals sink below known ones in the DESC list.
            compareByDescending<ResolvedApp> { it.downloadTotal ?: Long.MIN_VALUE }
        }
        return primary.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
