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

package de.lifemodule.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.lifemodule.app.data.shopping.ShoppingItemEntity
import de.lifemodule.app.data.shopping.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    val allItems = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(name: String, quantity: String = "", category: String = "Sonstiges") {
        viewModelScope.launch {
            try {
                repository.insert(
                    ShoppingItemEntity(name = name, quantity = quantity, category = category)
                )
            } catch (e: Exception) {
                Timber.e(e, "[Shopping] Failed to add item '%s'", name)
            }
        }
    }

    fun toggleChecked(item: ShoppingItemEntity) {
        viewModelScope.launch {
            try { repository.update(item.copy(checked = !item.checked)) }
            catch (e: Exception) { Timber.e(e, "[Shopping] Failed to toggle item '%s'", item.name) }
        }
    }

    fun deleteItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            try { repository.delete(item) }
            catch (e: Exception) { Timber.e(e, "[Shopping] Failed to delete item '%s'", item.name) }
        }
    }

    fun clearChecked() {
        viewModelScope.launch {
            try { repository.deleteChecked() }
            catch (e: Exception) { Timber.e(e, "[Shopping] Failed to clear checked items") }
        }
    }
}
