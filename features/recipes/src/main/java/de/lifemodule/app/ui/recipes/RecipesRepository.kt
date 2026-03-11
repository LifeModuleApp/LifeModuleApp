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

import de.lifemodule.app.data.recipes.RecipeDao
import de.lifemodule.app.data.recipes.RecipeEntity
import de.lifemodule.app.data.recipes.RecipeIngredientEntity
import de.lifemodule.app.data.recipes.RecipeWithIngredients
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipesRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {
    fun getAllRecipes(): Flow<List<RecipeEntity>> = recipeDao.getAllRecipes()

    fun getAllRecipesWithIngredients(): Flow<List<RecipeWithIngredients>> =
        recipeDao.getAllRecipesWithIngredients()

    fun getRecipesByCategory(category: String): Flow<List<RecipeEntity>> =
        recipeDao.getRecipesByCategory(category)

    fun searchRecipes(query: String): Flow<List<RecipeEntity>> = recipeDao.searchRecipes(query)

    suspend fun getRecipeByUuid(uuid: String): RecipeEntity? = recipeDao.getRecipeByUuid(uuid)

    suspend fun getRecipeWithIngredients(uuid: String): RecipeWithIngredients? =
        recipeDao.getRecipeWithIngredients(uuid)

    suspend fun insertRecipe(recipe: RecipeEntity): Long = recipeDao.insertRecipe(recipe)

    suspend fun updateRecipe(recipe: RecipeEntity) = recipeDao.updateRecipe(recipe)

    suspend fun deleteRecipe(recipe: RecipeEntity) = recipeDao.deleteRecipe(recipe)

    suspend fun insertIngredient(ingredient: RecipeIngredientEntity): Long =
        recipeDao.insertIngredient(ingredient)

    suspend fun updateIngredient(ingredient: RecipeIngredientEntity) =
        recipeDao.updateIngredient(ingredient)

    suspend fun deleteIngredient(ingredient: RecipeIngredientEntity) =
        recipeDao.deleteIngredient(ingredient)

    suspend fun deleteIngredientsForRecipe(recipeId: String) =
        recipeDao.deleteIngredientsForRecipe(recipeId)

    fun getIngredientsForRecipe(recipeId: String): Flow<List<RecipeIngredientEntity>> =
        recipeDao.getIngredientsForRecipe(recipeId)
}
