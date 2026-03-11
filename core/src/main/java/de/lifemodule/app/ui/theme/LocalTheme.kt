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

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** 
 * Current effective accent colour.  
 * Default = Electric Lime #A2FF00. 
 * Provided by [LifeModuleTheme] from [AppThemeViewModel].
 */
val LocalAccentColor = compositionLocalOf { Color(red = 0.635f, green = 1f, blue = 0f) }

/**
 * Per-module accent overrides.
 * Keys are [de.lifemodule.app.ui.dashboard.AppModule.id].
 * Provided by [LifeModuleTheme] from [AppThemeViewModel].
 */
val LocalModuleColors = compositionLocalOf<Map<String, Color>> { emptyMap() }
