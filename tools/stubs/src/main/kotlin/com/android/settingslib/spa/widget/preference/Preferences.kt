package com.android.settingslib.spa.widget.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState

interface PreferenceModel {
    val title: String
    val summary: (() -> String)? get() = null
    val enabled: (() -> Boolean)? get() = null
    val onClick: (() -> Unit)? get() = null
}

@Composable
fun Preference(model: PreferenceModel) {}

interface SwitchPreferenceModel {
    val title: String
    val summary: (() -> String)? get() = null
    val changeable: (() -> Boolean)? get() = null
    val checked: () -> Boolean
    val onCheckedChange: (Boolean) -> Unit
}

@Composable
fun SwitchPreference(model: SwitchPreferenceModel) {}

class ListPreferenceOption(val id: Int, val text: String)

interface ListPreferenceModel {
    val title: String
    val enabled: (() -> Boolean)? get() = null
    val options: List<ListPreferenceOption>
    val selectedId: IntState
    val onIdSelected: (Int) -> Unit
}

@Composable
fun ListPreference(model: ListPreferenceModel) {}
