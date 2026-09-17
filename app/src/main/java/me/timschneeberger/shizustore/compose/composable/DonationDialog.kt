/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.helper.SourceLauncher

/** Donation options shared by the About screen row and the Settings footer card. */
@Composable
fun DonationDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.donation_dialog_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.donation_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                DonationOption(
                    iconRes = R.drawable.ic_paypal_symbol,
                    title = stringResource(R.string.donation_paypal_title),
                    subtitle = stringResource(R.string.donation_paypal_value),
                    outline = true,
                    onClick = {
                        SourceLauncher.open(context, PAYPAL_URL)
                        onDismiss()
                    }
                )
                DonationOption(
                    iconRes = R.drawable.ic_kofi_symbol,
                    title = stringResource(R.string.donation_kofi_title),
                    subtitle = stringResource(R.string.donation_kofi_value),
                    onClick = {
                        SourceLauncher.open(context, KOFI_URL)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

/** Brand icons keep their own colours, so the painter is left untinted. */
@Composable
private fun DonationOption(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    outline: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(R.dimen.spacing_small)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(dimensionResource(R.dimen.icon_size_default))) {
            // White copies offset around the logo act as an outline so the dark
            // brand colours stay legible on the dark theme's dialog surface.
            if (outline) {
                OUTLINE_OFFSETS.forEach { (dx, dy) ->
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = OUTLINE_WIDTH * dx, y = OUTLINE_WIDTH * dy),
                        tint = Color.White
                    )
                }
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = Color.Unspecified
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val PAYPAL_URL = "https://paypal.me/timschneeberger"
private const val KOFI_URL = "https://ko-fi.com/thepbone"
private val OUTLINE_WIDTH = 1.dp
private val OUTLINE_OFFSETS = listOf(
    -1 to -1, 0 to -1, 1 to -1,
    -1 to 0, 1 to 0,
    -1 to 1, 0 to 1, 1 to 1
)
