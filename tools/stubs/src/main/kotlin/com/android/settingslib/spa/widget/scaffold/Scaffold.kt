package com.android.settingslib.spa.widget.scaffold

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

abstract class MoreOptionsScope {
    abstract fun dismiss()

    @Composable
    open fun MenuItem(text: String, enabled: Boolean = true, onClick: () -> Unit) {}
}

@Composable
fun MoreOptionsAction(
    content: @Composable MoreOptionsScope.() -> Unit,
) {}

@Composable
fun RegularScaffold(
    title: String,
    isFirstLayerPageWhenEmbedded: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    content()
}
