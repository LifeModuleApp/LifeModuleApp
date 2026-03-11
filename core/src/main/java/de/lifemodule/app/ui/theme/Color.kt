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

// AMOLED Dark Palette
val Black = Color(0xFF000000)
val Surface = Color(0xFF1C1C1E)
val Elevated = Color(0xFF2C2C2E)
val Primary = Color(0xFFFFFFFF)
val Secondary = Color(0xFF8E8E93)
val Border = Color(0xFF2C2C2E)
val Accent = Color(0xFFA2FF00)       // Electric Lime (default; overridden via LocalAccentColor)
val ElectricLime = Accent            // named alias
val Destructive = Color(0xFFFF453A)  // Red for delete/warnings
