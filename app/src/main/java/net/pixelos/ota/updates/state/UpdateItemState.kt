/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package net.pixelos.ota.updates.state

import androidx.annotation.StringRes
import net.pixelos.ota.updates.action.UpdateActions

data class UpdateItemState(
    val downloadId: String,
    val isLocal: Boolean,

    val buildDate: String,
    val buildVersion: String,
    val status: String,

    val fileSize: String,
    val androidUpdateInfo: String,
    val securityUpdate: String,
    @param:StringRes val installNote: Int,

    val progress: ProgressState?,
    val actions: UpdateActions,

    val maintainer: String? = null,
    val githubUrl: String? = null,
    val forumUrl: String? = null,
    val donationUrl: String? = null,
    val device: String? = null,
    val buildType: String? = null,
    val phase: UpdateOperationPhase = UpdateOperationPhase.IDLE,
)

sealed interface ProgressState {
    data class Determinate(
        val percent: Float,
        val downloadedSize: String,
        val eta: String,
    ) : ProgressState

    data object Indeterminate : ProgressState
}
