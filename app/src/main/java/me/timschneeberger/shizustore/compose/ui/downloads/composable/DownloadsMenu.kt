/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's DownloadsMenu.
 */

package me.timschneeberger.shizustore.compose.ui.downloads.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R

enum class DownloadsMenuItem { CANCEL_ALL, CLEAR_FINISHED, CLEAR_ALL }

@Composable
fun DownloadsMenu(
    modifier: Modifier = Modifier,
    onMenuItemClicked: (item: DownloadsMenuItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    fun onClick(item: DownloadsMenuItem) {
        onMenuItemClicked(item)
        expanded = false
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.action_more)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download_cancel_all)) },
                onClick = { onClick(DownloadsMenuItem.CANCEL_ALL) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download_clear_finished)) },
                onClick = { onClick(DownloadsMenuItem.CLEAR_FINISHED) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download_clear_all)) },
                onClick = { onClick(DownloadsMenuItem.CLEAR_ALL) }
            )
        }
    }
}
