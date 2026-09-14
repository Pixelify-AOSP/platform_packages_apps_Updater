package com.android.settingslib.spa.framework.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object SettingsDimension {
    val itemPaddingAround: Dp = 16.dp
    val itemPaddingEnd: Dp = 16.dp
    val itemPaddingStart: Dp = 16.dp
    val itemIconSize: Dp = 24.dp
    val buttonPadding: Dp = 8.dp
}

object SettingsSpace {
    val extraSmall: Dp = 4.dp
    val extraSmall2: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
}

object SettingsTheme {
    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        content()
    }
}

@Composable
fun SettingsTheme(content: @Composable () -> Unit) {
    content()
}
