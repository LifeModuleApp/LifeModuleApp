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

package de.lifemodule.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.recipes.RecipeCategory
import de.lifemodule.app.data.recipes.RecipeEntity
import de.lifemodule.app.data.recipes.RecipeIngredientEntity
import de.lifemodule.app.data.recipes.RecipeWithIngredients
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val repository: RecipesRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val recipes: StateFlow<List<RecipeWithIngredients>> =
        repository.getAllRecipesWithIngredients()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()

    fun clearSaveState() { _saveSuccess.value = null }

    fun addRecipe(
        title: String,
        instructions: String,
        category: RecipeCategory,
        servings: Int,
        prepTimeMinutes: Int?,
        cookTimeMinutes: Int?,
        notes: String?,
        ingredients: List<IngredientInput>
    ) {
        viewModelScope.launch {
            try {
                val now = timeProvider.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                val recipeUuid = BaseEntity.generateUuid()

                repository.insertRecipe(
                    RecipeEntity(
                        uuid = recipeUuid,
                        createdAt = now,
                        updatedAt = now,
                        title = title,
                        instructions = instructions,
                        category = category,
                        servings = servings,
                        prepTimeMinutes = prepTimeMinutes,
                        cookTimeMinutes = cookTimeMinutes,
                        notes = notes
                    )
                )

                ingredients.forEachIndexed { index, input ->
                    try {
                        repository.insertIngredient(
                            RecipeIngredientEntity(
                                createdAt = now,
                                updatedAt = now,
                                recipeId = recipeUuid,
                                name = input.name,
                                quantity = input.quantity,
                                unit = input.unit,
                                orderIndex = index,
                                foodItemUuid = input.foodItemUuid
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "[Recipes] Failed to insert ingredient '%s'", input.name)
                    }
                }

                _saveSuccess.value = true
                Timber.i("[Recipes] Recipe '%s' (%s) added with %d ingredients",
                    title, recipeUuid, ingredients.size)
            } catch (e: Exception) {
                Timber.e(e, "[Recipes] Failed to insert recipe")
                _saveSuccess.value = false
            }
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            try {
                repository.deleteIngredientsForRecipe(recipe.uuid)
                repository.deleteRecipe(recipe)
                Timber.i("[Recipes] Recipe '%s' deleted", recipe.title)
            } catch (e: Exception) {
                Timber.e(e, "[Recipes] Failed to delete recipe '%s'", recipe.uuid)
            }
        }
    }
}

/**
 * UI input model for a single ingredient (not yet persisted).
 */
data class IngredientInput(
    val name: String,
    val quantity: Double,
    val unit: String,
    val foodItemUuid: String? = null
)
