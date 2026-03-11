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

package de.lifemodule.app.data.shopping

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(private val dao: ShoppingDao) {
    fun getAllItems() = dao.getAllItems()
    suspend fun insert(item: ShoppingItemEntity) = dao.insert(item)
    suspend fun update(item: ShoppingItemEntity) = dao.update(item)
    suspend fun delete(item: ShoppingItemEntity) = dao.delete(item)
    suspend fun deleteChecked() = dao.deleteChecked()
}
