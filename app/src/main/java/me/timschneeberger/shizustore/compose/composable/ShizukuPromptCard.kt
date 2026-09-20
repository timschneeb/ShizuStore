/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.installer.ShizukuInstaller
import me.timschneeberger.shizustore.data.installer.ShizukuPrompt
import me.timschneeberger.shizustore.data.installer.probeShizukuPrompt
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.extensions.viewExternal
import me.timschneeberger.shizustore.util.Preferences
import rikka.shizuku.ShizukuProvider

/** Walks the user through installing, starting or granting the Shizuku installer; re-probes on
 * resume so returning from Play, Shizuku or the permission dialog refreshes the card. */
@Composable
fun ShizukuPromptCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prompt by produceState<ShizukuPrompt?>(initialValue = null, context, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            value = withContext(Dispatchers.IO) { probeShizukuPrompt(context) }
        }
    }

    val current = prompt
    val scope = rememberCoroutineScope()

    // Only store Shizuku as the installer once the re-probe reports READY: a present package
    // or a running service does not imply the grant and installs would fail without it.
    var setupRequested by remember { mutableStateOf(false) }
    LaunchedEffect(current) {
        if (setupRequested && current == ShizukuPrompt.READY) {
            setupRequested = false
            scope.launch {
                Preferences.putInteger(
                    context,
                    Preferences.PREFERENCE_INSTALLER_ID,
                    Installer.SHIZUKU.ordinal
                )
                onDismiss()
            }
        }
    }

    current ?: return
    if (current == ShizukuPrompt.READY) return

    val isInstall = current == ShizukuPrompt.INSTALL
    val isStart = current == ShizukuPrompt.START

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large))) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info_outlined),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.shizuku_card_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            when {
                                isInstall -> R.string.shizuku_card_install_body
                                isStart -> R.string.shizuku_card_start_body
                                else -> R.string.shizuku_card_grant_body
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clear),
                        contentDescription = stringResource(R.string.shizuku_card_dismiss)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        when {
                            isInstall -> openShizukuOnPlay(context)
                            isStart -> {
                                setupRequested = true
                                openShizukuApp(context)
                            }
                            else -> {
                                setupRequested = true
                                ShizukuInstaller.requestPermissionIfNeeded()
                            }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(
                            when {
                                isInstall -> R.string.shizuku_card_install_action
                                isStart -> R.string.shizuku_card_start_action
                                else -> R.string.shizuku_card_grant_action
                            }
                        )
                    )
                }
            }
        }
    }
}

/** Opening the manager is the only way to start its service; fall back to the stock listing
 * when no manager app is launchable. */
private fun openShizukuApp(context: Context) {
    val started = ShizukuInstaller.managerPackages(context).any { managerPackage ->
        val intent = context.packageManager.getLaunchIntentForPackage(managerPackage)
        intent != null &&
            runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
    }
    if (!started) {
        openShizukuOnPlay(context)
    }
}

/** Play first, browser fallback; devices without Play still reach the listing. */
private fun openShizukuOnPlay(context: Context) {
    val packageName = ShizukuProvider.MANAGER_APPLICATION_ID
    if (!context.viewExternal("market://details?id=$packageName")) {
        context.viewExternal("https://play.google.com/store/apps/details?id=$packageName")
    }
}
