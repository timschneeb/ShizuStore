/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.model.AppPrice
import me.timschneeberger.shizustore.data.model.AppSort

/**
 * One raw query keeps every filter combination on a single Room path; values are always
 * bound, so user input cannot alter the statement.
 */
object AppListQueryBuilder {

    fun build(
        args: AppListArgs,
        useInstallCountsForPopularity: Boolean = false,
        excludePackage: String? = null
    ): SupportSQLiteQuery {
        val binds = mutableListOf<Any?>()
        val where = mutableListOf<String>()
        val head = StringBuilder()

        val categorySlug = args.categorySlug?.takeIf { it.isNotBlank() }
        if (categorySlug != null) {
            // Subtree selection: a category's apps include all of its descendants.
            head.append(
                "WITH RECURSIVE tree(slug) AS (" +
                    " SELECT ?" +
                    " UNION ALL" +
                    " SELECT c.slug FROM category c JOIN tree t ON c.parentSlug = t.slug)" +
                    " "
            )
            binds += categorySlug
        }

        val query = args.query.trim()
        if (query.isNotEmpty()) {
            val pattern = escapeLikePattern(query)
            where += "(name LIKE '%' || ? || '%' ESCAPE '\\'" +
                " OR packageName LIKE '%' || ? || '%' ESCAPE '\\'" +
                " OR description LIKE '%' || ? || '%' ESCAPE '\\')"
            repeat(SEARCH_COLUMNS) { binds += pattern }
        }

        if (categorySlug != null) {
            where += "categorySlug IN (SELECT slug FROM tree)"
        }

        when (args.price) {
            AppPrice.FREE -> where += "hasPaid = 0 AND hasIap = 0"
            AppPrice.IAP -> where += "hasPaid = 0 AND hasIap = 1"
            AppPrice.IAP_OR_PAID -> where += "(hasPaid = 1 OR hasIap = 1)"
            null -> Unit
        }

        if (args.recommended) {
            where += "isRecommended = 1"
        }

        if (excludePackage != null) {
            // The store's own catalog row is never browsable; it surfaces only via
            // the installed and updates lists. NULL stays because most rows have a package.
            where += "(packageName IS NULL OR packageName != ?)"
            binds += excludePackage
        }

        val sql = buildString {
            append(head)
            append("SELECT * FROM app")
            if (where.isNotEmpty()) {
                append(" WHERE ")
                append(where.joinToString(" AND "))
            }
            append(" ORDER BY ")
            append(orderBy(args.sort, useInstallCountsForPopularity))
        }

        return SimpleSQLiteQuery(sql, binds.toTypedArray())
    }

    private fun orderBy(sort: AppSort, useInstallCountsForPopularity: Boolean): String =
        when (sort) {
            AppSort.NAME -> "name COLLATE NOCASE ASC"
            AppSort.RECENTLY_ADDED ->
                "listUpdatedAt IS NULL, listUpdatedAt DESC," +
                    " name COLLATE NOCASE ASC"
            AppSort.RECENTLY_UPDATED ->
                "versionUpdatedAt IS NULL, versionUpdatedAt DESC," +
                    " name COLLATE NOCASE ASC"
            // `IS NULL` sorts first so unknown popularity lands last in the DESC list.
            AppSort.STARS -> "stars IS NULL, stars DESC, name COLLATE NOCASE ASC"
            // Server flag: rank by installCount; NOT NULL, so plain DESC keeps only the name tiebreak.
            AppSort.DOWNLOADS -> if (useInstallCountsForPopularity) {
                "installCount DESC, name COLLATE NOCASE ASC"
            } else {
                "downloadTotal IS NULL, downloadTotal DESC, name COLLATE NOCASE ASC"
            }
            AppSort.SIZE_DESC -> "size IS NULL, size DESC, name COLLATE NOCASE ASC"
        }

    internal fun escapeLikePattern(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    private const val SEARCH_COLUMNS = 3
}
