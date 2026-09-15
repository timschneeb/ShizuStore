/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Adapted from PermissionController's PermissionMapping: the platform
 * permission to permission group table (getGroupOfPlatformPermission).
 */

package me.timschneeberger.shizustore.compose.ui.details

import android.os.Build

internal object PermissionGroups {
    private val platformPermissions: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()

        map["android.permission.READ_CONTACTS"] = "android.permission-group.CONTACTS"
        map["android.permission.WRITE_CONTACTS"] = "android.permission-group.CONTACTS"
        map["android.permission.GET_ACCOUNTS"] = "android.permission-group.CONTACTS"

        map["android.permission.READ_CALENDAR"] = "android.permission-group.CALENDAR"
        map["android.permission.WRITE_CALENDAR"] = "android.permission-group.CALENDAR"

        map["android.permission.SEND_SMS"] = "android.permission-group.SMS"
        map["android.permission.RECEIVE_SMS"] = "android.permission-group.SMS"
        map["android.permission.READ_SMS"] = "android.permission-group.SMS"
        map["android.permission.RECEIVE_MMS"] = "android.permission-group.SMS"
        map["android.permission.RECEIVE_WAP_PUSH"] = "android.permission-group.SMS"
        map["android.permission.READ_CELL_BROADCASTS"] = "android.permission-group.SMS"

        map["android.permission.READ_EXTERNAL_STORAGE"] = "android.permission-group.STORAGE"
        map["android.permission.WRITE_EXTERNAL_STORAGE"] = "android.permission-group.STORAGE"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            map["android.permission.ACCESS_MEDIA_LOCATION"] = "android.permission-group.STORAGE"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            map["android.permission.READ_MEDIA_AUDIO"] = "android.permission-group.READ_MEDIA_AURAL"
            map["android.permission.READ_MEDIA_IMAGES"] =
                "android.permission-group.READ_MEDIA_VISUAL"
            map["android.permission.READ_MEDIA_VIDEO"] =
                "android.permission-group.READ_MEDIA_VISUAL"
            map["android.permission.ACCESS_MEDIA_LOCATION"] =
                "android.permission-group.READ_MEDIA_VISUAL"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            map["android.permission.READ_MEDIA_VISUAL_USER_SELECTED"] =
                "android.permission-group.READ_MEDIA_VISUAL"
        }

        map["android.permission.ACCESS_FINE_LOCATION"] = "android.permission-group.LOCATION"
        map["android.permission.ACCESS_COARSE_LOCATION"] = "android.permission-group.LOCATION"
        map["android.permission.ACCESS_BACKGROUND_LOCATION"] = "android.permission-group.LOCATION"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            map["android.permission.BLUETOOTH_ADVERTISE"] =
                "android.permission-group.NEARBY_DEVICES"
            map["android.permission.BLUETOOTH_CONNECT"] = "android.permission-group.NEARBY_DEVICES"
            map["android.permission.BLUETOOTH_SCAN"] = "android.permission-group.NEARBY_DEVICES"
            map["android.permission.UWB_RANGING"] = "android.permission-group.NEARBY_DEVICES"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            map["android.permission.NEARBY_WIFI_DEVICES"] =
                "android.permission-group.NEARBY_DEVICES"
        }

        map["android.permission.READ_CALL_LOG"] = "android.permission-group.CALL_LOG"
        map["android.permission.WRITE_CALL_LOG"] = "android.permission-group.CALL_LOG"
        map["android.permission.PROCESS_OUTGOING_CALLS"] = "android.permission-group.CALL_LOG"

        map["android.permission.READ_PHONE_STATE"] = "android.permission-group.PHONE"
        map["android.permission.READ_PHONE_NUMBERS"] = "android.permission-group.PHONE"
        map["android.permission.CALL_PHONE"] = "android.permission-group.PHONE"
        map["android.permission.ADD_VOICEMAIL"] = "android.permission-group.PHONE"
        map["android.permission.USE_SIP"] = "android.permission-group.PHONE"
        map["android.permission.ANSWER_PHONE_CALLS"] = "android.permission-group.PHONE"
        map["android.permission.ACCEPT_HANDOVER"] = "android.permission-group.PHONE"

        map["android.permission.RECORD_AUDIO"] = "android.permission-group.MICROPHONE"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            map["android.permission.RECORD_BACKGROUND_AUDIO"] =
                "android.permission-group.MICROPHONE"
        }

        map["android.permission.ACTIVITY_RECOGNITION"] =
            "android.permission-group.ACTIVITY_RECOGNITION"

        map["android.permission.CAMERA"] = "android.permission-group.CAMERA"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            map["android.permission.BACKGROUND_CAMERA"] = "android.permission-group.CAMERA"
        }

        map["android.permission.BODY_SENSORS"] = "android.permission-group.SENSORS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            map["android.permission.POST_NOTIFICATIONS"] =
                "android.permission-group.NOTIFICATIONS"
            map["android.permission.BODY_SENSORS_BACKGROUND"] =
                "android.permission-group.SENSORS"
        }

        map
    }

    fun groupOfPlatformPermission(permission: String): String? = platformPermissions[permission]
}
