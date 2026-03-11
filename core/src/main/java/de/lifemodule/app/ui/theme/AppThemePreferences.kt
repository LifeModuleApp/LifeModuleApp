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
import androidx.compose.ui.graphics.toArgb
import timber.log.Timber
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import de.lifemodule.app.di.ApplicationScope
import de.lifemodule.app.ui.dashboard.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Persists the global accent colour and optional per-module accent overrides.
 *
 * Backed by Jetpack DataStore - replaces the old SharedPreferences implementation.
 * File: `files/datastore/app_theme_v1.preferences_pb`
 *
 * Exposes [globalAccentFlow] and [moduleColorsFlow] as singleton [StateFlow]s so
 * that ANY ViewModel instance observes the same reactive source - foundation of
 * real-time colour propagation without an app restart.
 */
@Singleton
class AppThemePreferences @Inject constructor(
    @Named("theme") private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope
) {
    companion object {
        private val KEY_GLOBAL = stringPreferencesKey("global_accent")
        private fun moduleKey(id: String) = stringPreferencesKey("module_accent_$id")

        /** Default Electric Lime - RRGGBB, no alpha prefix (always opaque). */
        const val DEFAULT_HEX = "A2FF00"
    }

    // ── Internal raw-prefs StateFlow - shared by all derived flows ──────────
    private val currentPrefs: StateFlow<Preferences> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyPreferences())

    // ── Singleton reactive flows (shared across ALL ViewModel instances) ────

    val globalAccentFlow: StateFlow<Color> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> hexToColor(prefs[KEY_GLOBAL] ?: DEFAULT_HEX) }
        .stateIn(scope, SharingStarted.Eagerly, hexToColor(DEFAULT_HEX))

    val moduleColorsFlow: StateFlow<Map<String, Color>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppModule.entries
                .mapNotNull { m -> prefs[moduleKey(m.id)]?.let { hex -> m.id to hexToColor(hex) } }
                .toMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    // ── Sync convenience wrappers (safe after Eagerly start) ───────────────

    fun getGlobalHex(): String = colorToHex(globalAccentFlow.value)

    fun getGlobalColor(): Color = globalAccentFlow.value

    fun getModuleHex(moduleId: String): String? = currentPrefs.value[moduleKey(moduleId)]

    fun getModuleColor(moduleId: String): Color? =
        getModuleHex(moduleId)?.let { hexToColor(it) }

    fun getAllModuleColors(): Map<String, Color> = moduleColorsFlow.value

    // ── Write operations (fire-and-forget via ApplicationScope) ───────────

    fun setGlobalColor(color: Color) {
        scope.launch {
            dataStore.edit { it[KEY_GLOBAL] = colorToHex(color) }
        }
    }

    fun setModuleColor(moduleId: String, color: Color?) {
        scope.launch {
            dataStore.edit { prefs ->
                if (color == null) prefs.remove(moduleKey(moduleId))
                else prefs[moduleKey(moduleId)] = colorToHex(color)
            }
        }
    }
}

// ── Top-level utilities (accessible anywhere in ui.theme) ─────────────────

fun colorToHex(color: Color): String =
    String.format("%06X", color.toArgb() and 0xFFFFFF)

fun hexToColor(hex: String): Color = try {
    val cleaned = hex.trimStart('#').padStart(6, '0').take(6)
    Color(android.graphics.Color.parseColor("#FF$cleaned"))
} catch (e: Exception) {
    Timber.w(e, "[AppThemePreferences] Failed to parse hex color: %s", hex)
    Color(red = 0.635f, green = 1f, blue = 0f) // #A2FF00 fallback
}