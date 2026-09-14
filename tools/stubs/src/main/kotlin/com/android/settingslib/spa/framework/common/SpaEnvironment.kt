package com.android.settingslib.spa.framework.common

import android.content.Context

abstract class SpaEnvironment(val context: Context) {
    open val pageProviderRepository: Lazy<SettingsPageProviderRepository> = lazy {
        SettingsPageProviderRepository(emptyList())
    }
    open val isSpaExpressiveEnabled: Boolean = false
}

class SettingsPageProviderRepository(val list: List<Any> = emptyList())

object SpaEnvironmentFactory {
    fun reset(env: SpaEnvironment) {}
}
