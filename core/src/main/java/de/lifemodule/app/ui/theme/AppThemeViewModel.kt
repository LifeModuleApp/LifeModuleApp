/*
 * LifeModule — A modular, privacy-focused life tracking app for Android.
 * Copyright (C) 2026 Paul Bernhard Colin Witzke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.lifemodule.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin ViewModel that exposes the singleton [AppThemePreferences] flows.
 *
 * Because [AppThemePreferences] is a @Singleton, every ViewModel instance
 * (Activity-scoped or NavEntry-scoped) observes the SAME [StateFlow]s.
 * Calling [setGlobalAccent] from SettingsScreen immediately updates the
 * flows read by MainActivity -> real-time recomposition, no restart needed.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    private val prefs: AppThemePreferences
) : ViewModel() {

    /** Reactive global accent - shared singleton flow. */
    val globalAccent: StateFlow<Color> get() = prefs.globalAccentFlow

    /** Per-module color overrides - shared singleton flow. */
    val moduleColors: StateFlow<Map<String, Color>> get() = prefs.moduleColorsFlow

    fun setGlobalAccent(color: Color) = prefs.setGlobalColor(color)

    fun setModuleAccent(moduleId: String, color: Color?) = prefs.setModuleColor(moduleId, color)

    /** Effective accent for a module: module override ?? global. */
    fun effectiveColor(moduleId: String): Color =
        prefs.moduleColorsFlow.value[moduleId] ?: prefs.globalAccentFlow.value
}
