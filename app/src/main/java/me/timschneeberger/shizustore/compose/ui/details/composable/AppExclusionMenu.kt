/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

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

@Composable
fun AppExclusionMenu(
    isBlacklisted: Boolean,
    isIgnored: Boolean,
    ignoresEveryVersion: Boolean,
    updateVersionName: String?,
    canIgnoreUpdates: Boolean,
    onToggleBlacklist: () -> Unit,
    onIgnoreAllUpdates: () -> Unit,
    onIgnoreThisVersion: () -> Unit,
    onStopIgnoring: () -> Unit,
    onAppInfo: () -> Unit,
    onAddToHome: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    fun choose(action: () -> Unit) {
        action()
        expanded = false
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.action_app_menu)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canIgnoreUpdates) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_app_info)) },
                    onClick = { choose(onAppInfo) }
                )
            }
            onAddToHome?.let { addToHome ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_add_to_home)) },
                    onClick = { choose(addToHome) }
                )
            }

            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isBlacklisted) {
                                R.string.action_unblacklist
                            } else {
                                R.string.action_blacklist
                            }
                        )
                    )
                },
                onClick = { choose(onToggleBlacklist) }
            )

            if (isIgnored) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (ignoresEveryVersion) {
                                    R.string.action_stop_ignoring_all
                                } else {
                                    R.string.action_stop_ignoring_version
                                }
                            )
                        )
                    },
                    onClick = { choose(onStopIgnoring) }
                )
            } else {
                if (updateVersionName != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    R.string.action_ignore_version,
                                    updateVersionName
                                )
                            )
                        },
                        onClick = { choose(onIgnoreThisVersion) }
                    )
                }
                if (canIgnoreUpdates) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_ignore_all)) },
                        onClick = { choose(onIgnoreAllUpdates) }
                    )
                }
            }
        }
    }
}
