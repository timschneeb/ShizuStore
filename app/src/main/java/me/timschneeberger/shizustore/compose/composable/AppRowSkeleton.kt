/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import me.timschneeberger.shizustore.R

@Composable
fun AppRowSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    showTrailing: Boolean = false,
    rowCount: Int = DEFAULT_SKELETON_ROWS
) {
    val description = stringResource(R.string.loading)

    ShimmerHost {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag(APP_ROW_SKELETON_TAG)
                .semantics { stateDescription = description },
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            items(rowCount) {
                if (showTrailing) ShimmerUpdateItem() else ShimmerAppRow()
            }
        }
    }
}

const val APP_ROW_SKELETON_TAG: String = "app_row_skeleton"

private const val DEFAULT_SKELETON_ROWS = 10
