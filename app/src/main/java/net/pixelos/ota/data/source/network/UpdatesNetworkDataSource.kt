/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package net.pixelos.ota.data.source.network

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import net.pixelos.ota.R
import net.pixelos.ota.deviceinfo.DeviceInfoUtils
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdatesNetworkDataSource(private val context: Context) {
    private val serverUrl: String
        get() {
            val base = context.getString(R.string.updater_server_url)
            require(base.startsWith("https://")) {
                "Update server URL must use HTTPS: $base"
            }
            require(DeviceInfoUtils.device.isNotBlank()) {
                "Missing ro.ascp.ota.device or ro.custom.device"
            }
            require(DeviceInfoUtils.otaBranch.isNotBlank()) {
                "Missing net.pixelos.version"
            }
            return base
                .replace("{device}", DeviceInfoUtils.device)
                .replace("{branch}", DeviceInfoUtils.otaBranch)
        }

    private val maintainerUrl: String
        get() {
            val base = context.getString(R.string.maintainer_url)
            return base
                .replace("{device}", DeviceInfoUtils.device)
                .replace("{branch}", DeviceInfoUtils.otaBranch)
        }

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun fetchMaintainerInfo(): MaintainerInfo? {
        var resultMaintainer: MaintainerInfo? = null
        var otaVersion: String? = null

        // 1. Single source of truth for maintainer information: API/devices/{device}.json
        try {
            val req = Request.Builder().url(maintainerUrl).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        resultMaintainer = json.decodeFromString<MaintainerInfo>(body)
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Single source of truth for version: API/updater/{device}.json
        try {
            val req = Request.Builder().url(serverUrl).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val list = json.decodeFromString<List<NetworkUpdate>>(body)
                        otaVersion = list.firstOrNull()?.version?.takeIf { it.isNotBlank() }
                    }
                }
            }
        } catch (_: Exception) {}

        return (resultMaintainer ?: MaintainerInfo()).copy(version = otaVersion)
    }

    fun fetchUpdates(): List<NetworkUpdate> {
        val request = Request.Builder().url(serverUrl).build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP status: ${response.code}")
            }
            val b = response.body ?: throw IOException("Empty response body")
            val contentLength = b.contentLength()
            if (contentLength > MAX_RESPONSE_BYTES) {
                throw IOException("Update response is too large: $contentLength bytes")
            }
            val source = b.source()
            if (source.request(MAX_RESPONSE_BYTES + 1)) {
                throw IOException("Update response exceeds $MAX_RESPONSE_BYTES bytes")
            }
            source.buffer.readByteArray().decodeToString()
        }

        val updates = json.decodeFromString<List<NetworkUpdate>>(body).onEach { it.validate() }

        val maintainerInfo = fetchMaintainerInfo()
        return if (maintainerInfo != null) {
            updates.map { update ->
                update.copy(
                    maintainer = maintainerInfo.maintainer,
                    github = maintainerInfo.github,
                    forum = maintainerInfo.telegram,
                    paypal = maintainerInfo.donationLink,
                    status = maintainerInfo.status ?: "OFFICIAL",
                    device = maintainerInfo.codename ?: update.device,
                )
            }
        } else {
            updates
        }
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 1024L * 1024L
    }
}
