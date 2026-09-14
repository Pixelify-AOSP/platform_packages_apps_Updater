package com.android.settingslib.spa.widget.ui

import androidx.compose.runtime.Composable

@Composable
fun Category(title: String, content: @Composable () -> Unit) {
    content()
}
