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

package de.lifemodule.app.data.packages

/**
 * Describes what kind of data a ZIP package contains.
 *
 * Used in `manifest.json` to tell the import pipeline which entity types
 * to expect inside the `entries` object.
 */
enum class PackageContentType {
    /** A workout plan (templates + exercises). */
    WORKOUT_PLAN,

    /** A curated food / nutritional database. */
    FOOD_DATABASE,

    /** A set of supplements with optional ingredients. */
    SUPPLEMENT_LIST,

    /** A collection of habits. */
    HABIT_SET,

    /** A recipe book (future module). */
    RECIPE_BOOK,

    /** Package contains entries from multiple modules. */
    MIXED
}
