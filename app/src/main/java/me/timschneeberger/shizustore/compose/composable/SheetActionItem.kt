/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import me.timschneeberger.shizustore.R

/** Shares [AuroraListItem]'s metrics so a sheet's actions match the rows behind them. */
@Composable
fun SheetActionItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    AuroraListItem(
        modifier = modifier,
        minHeight = R.dimen.min_touch_target,
        headline = label,
        headlineStyle = MaterialTheme.typography.bodyLarge.copy(color = color),
        onClick = onClick,
        leading = icon?.let {
            {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = color
                )
            }
        }
    )
}
