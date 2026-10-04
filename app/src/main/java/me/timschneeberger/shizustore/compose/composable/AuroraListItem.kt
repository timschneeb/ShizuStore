/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.annotation.DimenRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import me.timschneeberger.shizustore.R

/**
 * Passing one moves the control's state and role onto the row itself, so the row is a single
 * focusable target rather than a plain click next to a second control.
 */
sealed interface ItemSelection {
    val active: Boolean

    data class Radio(override val active: Boolean) : ItemSelection

    data class Switch(override val active: Boolean) : ItemSelection

    data class Checkbox(override val active: Boolean) : ItemSelection
}

@Composable
fun AuroraListItem(
    modifier: Modifier = Modifier,
    headline: String,
    supporting: String? = null,
    tertiary: AnnotatedString? = null,
    tertiaryInlineContent: Map<String, InlineTextContent> = emptyMap(),
    onClick: (() -> Unit)? = null,
    selection: ItemSelection? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    badges: (@Composable () -> Unit)? = null,
    headlineStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    headlineMaxLines: Int = 1,
    supportingMaxLines: Int = 1,
    tertiaryMaxLines: Int = 1,
    @DimenRes minHeight: Int? = null
) {
    val hasSupporting = !supporting.isNullOrBlank()
    val hasTertiary = !tertiary.isNullOrEmpty()

    val height = minHeight ?: when {
        hasSupporting && hasTertiary -> R.dimen.list_item_height_three_line
        hasSupporting -> R.dimen.list_item_height_two_line
        else -> R.dimen.list_item_height_one_line
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(Modifier.interaction(onClick, selection, enabled))
            .defaultMinSize(minHeight = dimensionResource(height))
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = headlineStyle,
                maxLines = headlineMaxLines,
                overflow = TextOverflow.Ellipsis
            )
            if (hasSupporting) {
                Text(
                    text = supporting.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = supportingMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasTertiary) {
                Text(
                    text = tertiary,
                    inlineContent = tertiaryInlineContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = tertiaryMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (badges != null) badges()
        if (trailing != null) trailing()
    }
}

private fun Modifier.interaction(
    onClick: (() -> Unit)?,
    selection: ItemSelection?,
    enabled: Boolean
): Modifier {
    if (onClick == null) return this

    return when (selection) {
        null -> clickable(enabled = enabled, onClick = onClick)

        is ItemSelection.Radio -> selectable(
            selected = selection.active,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick
        )

        is ItemSelection.Switch -> toggleable(
            value = selection.active,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = { onClick() }
        )

        is ItemSelection.Checkbox -> toggleable(
            value = selection.active,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = { onClick() }
        )
    }
}
