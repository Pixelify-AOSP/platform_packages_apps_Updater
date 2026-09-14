package com.android.settingslib.spa.framework.compose

import androidx.compose.runtime.compositionLocalOf

interface NavControllerWrapper {
    fun navigate(route: String, popUpCurrent: Boolean = false)
    fun navigateBack()
}

val LocalNavController = compositionLocalOf<NavControllerWrapper> {
    object : NavControllerWrapper {
        override fun navigate(route: String, popUpCurrent: Boolean) {}
        override fun navigateBack() {}
    }
}
