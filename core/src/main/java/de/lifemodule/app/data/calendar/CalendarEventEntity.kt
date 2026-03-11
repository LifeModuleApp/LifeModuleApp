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

package de.lifemodule.app.data.calendar

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.ImportSource

@Entity(
    tableName = "calendar_events",
    indices = [Index("date"), Index("isRecurring")]
)
data class CalendarEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    override val uuid: String = BaseEntity.generateUuid(),

    @ColumnInfo(name = "created_at")
    override val createdAt: Long = 0L,

    @ColumnInfo(name = "updated_at")
    override val updatedAt: Long = 0L,

    @ColumnInfo(name = "import_source")
    override val importSource: ImportSource = ImportSource.USER,

    @ColumnInfo(name = "imported_from_package_id")
    override val importedFromPackageId: String? = null,

    val title: String,
    val description: String = "",
    val date: String,
    val endDate: String? = null,
    val time: String? = null,
    val endTime: String? = null,
    val category: String = "other",
    val color: String = "#30D158",
    val isHoliday: Boolean = false,
    val isRecurring: Boolean = false,
) : BaseEntity(uuid, createdAt, updatedAt, importSource, importedFromPackageId)
