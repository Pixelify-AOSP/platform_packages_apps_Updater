package com.android.settingslib.spa.widget.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

class AlertDialogButton(val text: String, val onClick: () -> Unit = {})

@Composable
fun SettingsAlertDialogWithIcon(
    onDismissRequest: () -> Unit,
    icon: ImageVector,
    confirmButton: AlertDialogButton,
    dismissButton: AlertDialogButton? = null,
    title: String,
    text: @Composable () -> Unit,
) {}
