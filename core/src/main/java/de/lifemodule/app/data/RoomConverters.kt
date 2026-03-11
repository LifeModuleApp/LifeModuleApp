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

package de.lifemodule.app.data

import androidx.room.TypeConverter
import de.lifemodule.app.data.logbook.JourneyPurpose
import de.lifemodule.app.data.recipes.RecipeCategory
import de.lifemodule.app.data.scanner.ReceiptCategory

class RoomConverters {

    @TypeConverter
    fun fromImportSource(source: ImportSource): String = source.name

    @TypeConverter
    fun toImportSource(value: String): ImportSource =
        ImportSource.entries.firstOrNull { it.name == value } ?: ImportSource.USER

    @TypeConverter
    fun fromJourneyPurpose(purpose: JourneyPurpose): String = purpose.name

    @TypeConverter
    fun toJourneyPurpose(value: String): JourneyPurpose =
        JourneyPurpose.entries.firstOrNull { it.name == value } ?: JourneyPurpose.PRIVATE

    @TypeConverter
    fun fromReceiptCategory(category: ReceiptCategory): String = category.name

    @TypeConverter
    fun toReceiptCategory(value: String): ReceiptCategory =
        ReceiptCategory.entries.firstOrNull { it.name == value } ?: ReceiptCategory.OTHER

    @TypeConverter
    fun fromRecipeCategory(category: RecipeCategory): String = category.name

    @TypeConverter
    fun toRecipeCategory(value: String): RecipeCategory =
        RecipeCategory.entries.firstOrNull { it.name == value } ?: RecipeCategory.OTHER
}
