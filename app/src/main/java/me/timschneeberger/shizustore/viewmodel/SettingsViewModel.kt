/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.installer.AppInstaller
import me.timschneeberger.shizustore.data.installer.ShizukuInstaller
import me.timschneeberger.shizustore.data.installer.canInstallUnattended
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.work.UpdateWorker
import me.timschneeberger.shizustore.util.Preferences

enum class UnattendedPausedReason {
    UPDATE_CHECKS_OFF,
    INSTALLER_NEEDS_CONFIRMATION,
    SHIZUKU_NOT_READY,
    ROOT_UNAVAILABLE
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val _themeStyle = MutableStateFlow(THEME_STYLE_SYSTEM)
    val themeStyle: StateFlow<Int> = _themeStyle.asStateFlow()

    fun setThemeStyle(style: Int) {
        viewModelScope.launch {
            initialLoad.join()
            _themeStyle.value = style
            Preferences.putInteger(context, Preferences.PREFERENCE_THEME_STYLE, style)
        }
    }

    private val _dynamicColorsEnabled = MutableStateFlow(Preferences.dynamicColorsDefault)
    val dynamicColorsEnabled: StateFlow<Boolean> = _dynamicColorsEnabled.asStateFlow()

    fun setDynamicColorsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            initialLoad.join()
            _dynamicColorsEnabled.value = enabled
            Preferences.putBoolean(context, Preferences.PREFERENCE_DYNAMIC_COLORS, enabled)
        }
    }

    private val _blackNightEnabled = MutableStateFlow(false)
    val blackNightEnabled: StateFlow<Boolean> = _blackNightEnabled.asStateFlow()

    fun setBlackNightEnabled(enabled: Boolean) {
        viewModelScope.launch {
            initialLoad.join()
            _blackNightEnabled.value = enabled
            Preferences.putBoolean(context, Preferences.PREFERENCE_BLACK_NIGHT, enabled)
        }
    }

    private val _updateCheckIntervalHours =
        MutableStateFlow(UpdateWorker.DEFAULT_INTERVAL_HOURS)
    val updateCheckIntervalHours: StateFlow<Int> = _updateCheckIntervalHours.asStateFlow()

    fun setUpdateCheckInterval(hours: Int) {
        viewModelScope.launch {
            initialLoad.join()
            _updateCheckIntervalHours.value = hours
            Preferences.putInteger(context, Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL, hours)
            UpdateWorker.reschedule(context)
            refreshUnattendedPausedReason()
        }
    }

    private val _syncOnWifiOnly = MutableStateFlow(false)
    val syncOnWifiOnly: StateFlow<Boolean> = _syncOnWifiOnly.asStateFlow()

    fun setSyncOnWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            initialLoad.join()
            _syncOnWifiOnly.value = enabled
            Preferences.putBoolean(context, Preferences.PREFERENCE_SYNC_ON_WIFI_ONLY, enabled)
            UpdateWorker.reschedule(context)
        }
    }

    private val _unattendedUpdatesEnabled = MutableStateFlow(false)
    val unattendedUpdatesEnabled: StateFlow<Boolean> = _unattendedUpdatesEnabled.asStateFlow()

    private val _unattendedPausedReason = MutableStateFlow<UnattendedPausedReason?>(null)
    val unattendedPausedReason: StateFlow<UnattendedPausedReason?> =
        _unattendedPausedReason.asStateFlow()

    fun setUnattendedUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            initialLoad.join()
            _unattendedUpdatesEnabled.value = enabled
            Preferences.putBoolean(context, Preferences.PREFERENCE_UPDATES_UNATTENDED, enabled)
            refreshUnattendedPausedReason()
        }
    }

    private val _availableInstallers = MutableStateFlow<List<Installer>>(emptyList())
    val availableInstallers: StateFlow<List<Installer>> = _availableInstallers.asStateFlow()

    private val _selectedInstaller = MutableStateFlow<Installer?>(null)
    val selectedInstaller: StateFlow<Installer?> = _selectedInstaller.asStateFlow()

    private val initialLoad: Job = viewModelScope.launch {
        _themeStyle.value = Preferences.readInteger(
            context,
            Preferences.PREFERENCE_THEME_STYLE,
            THEME_STYLE_SYSTEM
        )
        _dynamicColorsEnabled.value = Preferences.readBoolean(
            context,
            Preferences.PREFERENCE_DYNAMIC_COLORS,
            Preferences.dynamicColorsDefault
        )
        _blackNightEnabled.value = Preferences.readBoolean(
            context,
            Preferences.PREFERENCE_BLACK_NIGHT
        )
        _updateCheckIntervalHours.value = UpdateWorker.intervalHours(context)
        _syncOnWifiOnly.value = Preferences.readBoolean(
            context,
            Preferences.PREFERENCE_SYNC_ON_WIFI_ONLY
        )
        _unattendedUpdatesEnabled.value = Preferences.readBoolean(
            context,
            Preferences.PREFERENCE_UPDATES_UNATTENDED
        )
    }

    init {
        viewModelScope.launch {
            initialLoad.join()
            _availableInstallers.value = AppInstaller.availableInstallers(context)
            _selectedInstaller.value = AppInstaller.getCurrentInstaller(context)
            refreshUnattendedPausedReason()
        }
    }

    fun selectInstaller(installer: Installer) {
        viewModelScope.launch {
            initialLoad.join()
            _selectedInstaller.value = installer

            if (installer == Installer.SHIZUKU) {
                ShizukuInstaller.requestPermissionIfNeeded()
            }

            Preferences.putInteger(context, Preferences.PREFERENCE_INSTALLER_ID, installer.ordinal)
            refreshUnattendedPausedReason()
        }
    }

    fun refreshUnattendedState() {
        viewModelScope.launch {
            initialLoad.join()
            refreshUnattendedPausedReason()
        }
    }

    private suspend fun refreshUnattendedPausedReason() {
        if (!_unattendedUpdatesEnabled.value) {
            _unattendedPausedReason.value = null
            return
        }

        if (_updateCheckIntervalHours.value == UpdateWorker.INTERVAL_NEVER) {
            _unattendedPausedReason.value = UnattendedPausedReason.UPDATE_CHECKS_OFF
            return
        }

        val ready = canInstallUnattended(AppInstaller.getCurrentInstaller(context))
        _unattendedPausedReason.value = if (ready) {
            null
        } else {
            when (storedInstallerChoice()) {
                Installer.SHIZUKU -> UnattendedPausedReason.SHIZUKU_NOT_READY
                Installer.ROOT -> UnattendedPausedReason.ROOT_UNAVAILABLE
                Installer.SESSION, Installer.NATIVE ->
                    UnattendedPausedReason.INSTALLER_NEEDS_CONFIRMATION
            }
        }
    }

    private suspend fun storedInstallerChoice(): Installer {
        val stored = Preferences.readInteger(
            context,
            Preferences.PREFERENCE_INSTALLER_ID,
            Installer.SESSION.ordinal
        )
        return Installer.entries.getOrNull(stored) ?: Installer.SESSION
    }

    private companion object {
        const val THEME_STYLE_SYSTEM = 0
    }
}
