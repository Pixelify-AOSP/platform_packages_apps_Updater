/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalSerializationApi::class)

package net.pixelos.ota.data.source.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import net.pixelos.ota.data.Update
import net.pixelos.ota.deviceinfo.DeviceInfoUtils
import net.pixelos.ota.misc.Constants
import java.net.URI

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class MaintainerInfo(
    @SerialName("maintainer") val maintainer: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("oem") val oem: String? = null,
    @SerialName("device") val device: String? = null,
    @SerialName("codename") val codename: String? = null,
    @SerialName("github") val github: String? = null,
    @SerialName("telegram") val telegram: String? = null,
    @SerialName("donation_link") val donationLink: String? = null,
    @SerialName("version") val version: String? = null,
) {
    val supportUrl: String? get() = telegram
    val donationUrl: String? get() = donationLink
    val officialStatus: String get() = status?.uppercase() ?: "OFFICIAL"
}

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkUpdate(
    @SerialName("datetime") val datetime: Long = 0,
    @SerialName("files") val files: List<NetworkUpdateFile> = emptyList(),
    @SerialName("incremental") val incremental: List<NetworkUpdateFile>? = null,
    @SerialName("version") val version: String = "",
    @SerialName("device") val device: String? = null,
    val maintainer: String? = null,
    val github: String? = null,
    val forum: String? = null,
    val paypal: String? = null,
    val status: String? = null,
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@JsonIgnoreUnknownKeys
data class NetworkUpdateFile(
    // @SerialName("date") val date: String? = null,
    // @SerialName("datetime") val datetime: Long? = null,
    @SerialName("filename") val filename: String,
    // @SerialName("filepath") val filepath: String,
    @SerialName("os_patch_level") val osPatchLevel: String? = null,
    @SerialName("os_sdk_level") val osSdkLevel: Int = 0,
    @SerialName("ota_property_files") val otaPropertyFiles: String? = null,
    // @SerialName("sha1") val sha1: String,
    @SerialName("sha256") val sha256: String,
    @SerialName("size") val size: Long,
    // @SerialName("type") val type: String? = null,
    @SerialName("url") val url: String,
)

private data class PackageFileRange(
    val offset: Long,
    val size: Long,
)

private fun String.parsePackageFileRanges(packageSize: Long) =
    split(",").associate { token ->
        val parts = token.trim().split(":", limit = 3)
        require(parts.size == 3) { "Malformed ota_property_files entry: $token" }
        val name = parts[0].trim()
        val offset = parts[1].trim().toLong()
        val size = parts[2].trim().toLong()
        require(name.isNotEmpty()) { "Empty ota_property_files name" }
        require(offset >= 0 && size > 0 && offset <= Long.MAX_VALUE - size) {
            "Invalid range for $name"
        }
        require(offset + size <= packageSize) { "Range for $name exceeds package size" }
        name to PackageFileRange(offset = offset, size = size)
    }

fun NetworkUpdateFile.validate(label: String) {
    require(filename.isNotBlank()) { "$label.filename must not be blank" }
    require(sha256.matches(Regex("[0-9a-f]{64}"))) { "$label.sha256 must be lowercase hex" }
    require(size > 0) { "$label.size must be positive" }
    require(URI(url).scheme.equals("https", ignoreCase = true)) {
        "$label URL must use HTTPS"
    }
    otaPropertyFiles?.parsePackageFileRanges(size)?.let { ranges ->
        require(Constants.AB_PAYLOAD_METADATA_PATH in ranges) {
            "ota_property_files is missing ${Constants.AB_PAYLOAD_METADATA_PATH}"
        }
        require(Constants.AB_PAYLOAD_BIN_PATH in ranges) {
            "ota_property_files is missing ${Constants.AB_PAYLOAD_BIN_PATH}"
        }
        require(Constants.AB_PAYLOAD_PROPERTIES_PATH in ranges) {
            "ota_property_files is missing ${Constants.AB_PAYLOAD_PROPERTIES_PATH}"
        }
    }
}

fun NetworkUpdate.validate() {
    require(datetime > 0) { "datetime must be positive" }
    require(files.size == 1) { "Each update must contain exactly one file" }
    require(version.isNotBlank()) { "version must not be blank" }

    files.single().validate("file")
    incremental?.let {
        require(it.size == 1) { "Each update must contain exactly one incremental file" }
        it.single().validate("incremental file")
    }
}

private fun NetworkUpdate.toUpdate(file: NetworkUpdateFile): Update {
    val packageFileRanges = file.otaPropertyFiles?.parsePackageFileRanges(file.size).orEmpty()
    val payloadMetadataRange = packageFileRanges[Constants.AB_PAYLOAD_METADATA_PATH]
    val payloadRange = packageFileRanges[Constants.AB_PAYLOAD_BIN_PATH]
    val payloadPropertiesRange = packageFileRanges[Constants.AB_PAYLOAD_PROPERTIES_PATH]

    return Update(
        downloadId = file.sha256,
        name = file.filename,
        timestamp = datetime,
        fileSize = file.size,
        downloadUrl = file.url,
        version = version,
        osPatchLevel = file.osPatchLevel ?: DeviceInfoUtils.buildSecurityPatch,
        osSdkLevel = if (file.osSdkLevel > 0) file.osSdkLevel else DeviceInfoUtils.sdkLevel,
        payloadMetadataOffset = payloadMetadataRange?.offset,
        payloadMetadataSize = payloadMetadataRange?.size,
        payloadOffset = payloadRange?.offset,
        payloadSize = payloadRange?.size,
        payloadPropertiesOffset = payloadPropertiesRange?.offset,
        payloadPropertiesSize = payloadPropertiesRange?.size,
        isAvailableOnline = true,
        type = status?.uppercase() ?: "OFFICIAL",
        maintainer = maintainer,
        githubUrl = github,
        forumUrl = forum,
        donationUrl = paypal?.takeIf { it.isNotBlank() },
        device = device?.removePrefix("custom_"),
    )
}

fun NetworkUpdate.toUpdate(): Update = toUpdate(files[0])

fun NetworkUpdate.toIncrementalUpdate(): Update? {
    val file = incremental?.firstOrNull() ?: return null
    return toUpdate(file)
}
