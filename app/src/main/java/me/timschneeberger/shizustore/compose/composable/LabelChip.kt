/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import me.timschneeberger.shizustore.R

@Composable
fun LabelChip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val radius = dimensionResource(R.dimen.radius_small)
    Surface(
        modifier = modifier,
        color = container,
        contentColor = content,
        shape = remember(radius) { RoundedCornerShape(radius) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.chip_height))
                .padding(
                    horizontal = dimensionResource(R.dimen.spacing_medium),
                    vertical = dimensionResource(R.dimen.spacing_xsmall)
                )
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}
