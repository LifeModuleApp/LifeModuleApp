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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = Black,
    surface = Surface,
    primary = Primary,
    secondary = Secondary,
    onBackground = Primary,
    onSurface = Primary,
    onPrimary = Black,
    surfaceVariant = Elevated,
    outline = Border
)

@Composable
fun LifeModuleTheme(
    accentColor: Color = Accent,
    moduleColors: Map<String, Color> = emptyMap(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAccentColor provides accentColor,
        LocalModuleColors provides moduleColors
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
