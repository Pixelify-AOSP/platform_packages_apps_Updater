/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package net.pixelos.ota.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.pixelos.ota.data.source.network.MaintainerInfo
import net.pixelos.ota.deviceinfo.DeviceInfoUtils
import kotlin.time.Duration.Companion.seconds

private const val APP_STATE_DATASTORE_NAME = "app_state"

private val Context.appStatePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = APP_STATE_DATASTORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler {
        emptyPreferences()
    },
)

private object AppStatePreferencesKeys {
    val LAST_CHECKED_TIMESTAMP = longPreferencesKey("last_checked_timestamp")
    val DEVICE_STATUS = stringPreferencesKey("device_status")
    val MAINTAINER_NAME = stringPreferencesKey("maintainer_name")
    val GITHUB_URL = stringPreferencesKey("github_url")
    val FORUM_URL = stringPreferencesKey("forum_url")
    val DONATION_URL = stringPreferencesKey("donation_url")
    val VERSION = stringPreferencesKey("version")
}

class AppStateRepository(context: Context) {
    private val appState = context.applicationContext.appStatePreferencesDataStore

    val lastCheckedTimestampFlow: Flow<Long> = appState.data.map { preferences ->
        preferences[AppStatePreferencesKeys.LAST_CHECKED_TIMESTAMP]
            ?: DeviceInfoUtils.buildDateTimestamp.seconds.inWholeMilliseconds
    }

    val deviceStatusFlow: Flow<String?> = appState.data.map { preferences ->
        preferences[AppStatePreferencesKeys.DEVICE_STATUS]
    }

    val maintainerInfoFlow: Flow<MaintainerInfo?> = appState.data.map { preferences ->
        val status = preferences[AppStatePreferencesKeys.DEVICE_STATUS]
        val maintainer = preferences[AppStatePreferencesKeys.MAINTAINER_NAME]
        val version = preferences[AppStatePreferencesKeys.VERSION]
        if (status != null || maintainer != null || version != null) {
            MaintainerInfo(
                maintainer = maintainer,
                status = status,
                github = preferences[AppStatePreferencesKeys.GITHUB_URL],
                telegram = preferences[AppStatePreferencesKeys.FORUM_URL],
                donationLink = preferences[AppStatePreferencesKeys.DONATION_URL],
                version = version,
            )
        } else {
            null
        }
    }

    suspend fun setLastCheckedTimestamp(timestamp: Long) {
        appState.edit { preferences ->
            preferences[AppStatePreferencesKeys.LAST_CHECKED_TIMESTAMP] = timestamp
        }
    }

    suspend fun setDeviceStatus(status: String) {
        appState.edit { preferences ->
            preferences[AppStatePreferencesKeys.DEVICE_STATUS] = status
        }
    }

    suspend fun setMaintainerInfo(info: MaintainerInfo) {
        appState.edit { preferences ->
            preferences[AppStatePreferencesKeys.DEVICE_STATUS] = info.officialStatus
            info.maintainer?.let { preferences[AppStatePreferencesKeys.MAINTAINER_NAME] = it }
            info.github?.let { preferences[AppStatePreferencesKeys.GITHUB_URL] = it }
            info.supportUrl?.let { preferences[AppStatePreferencesKeys.FORUM_URL] = it }
            info.donationUrl?.let { preferences[AppStatePreferencesKeys.DONATION_URL] = it }
            info.version?.let { preferences[AppStatePreferencesKeys.VERSION] = it }
        }
    }
}
