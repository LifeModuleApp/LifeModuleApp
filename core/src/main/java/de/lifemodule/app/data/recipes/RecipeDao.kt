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

package de.lifemodule.app.data.recipes

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the Recipes module.
 *
 * Completely isolated from the Nutrition DAO - separate tables, no foreign keys
 * across module boundaries.
 */
@Dao
interface RecipeDao {

    // ── Recipes ────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    /** Ignore-variant for import pipeline (UUID collision -> skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecipeIgnore(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes ORDER BY title ASC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE category = :category ORDER BY title ASC")
    fun getRecipesByCategory(category: String): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE uuid = :uuid")
    suspend fun getRecipeByUuid(uuid: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchRecipes(query: String): Flow<List<RecipeEntity>>

    // ── Ingredients ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIngredient(ingredient: RecipeIngredientEntity): Long

    /** Ignore-variant for import pipeline (UUID collision -> skip). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngredientIgnore(ingredient: RecipeIngredientEntity): Long

    @Update
    suspend fun updateIngredient(ingredient: RecipeIngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: RecipeIngredientEntity)

    @Query("SELECT * FROM recipe_ingredients WHERE recipe_id = :recipeId ORDER BY order_index ASC")
    fun getIngredientsForRecipe(recipeId: String): Flow<List<RecipeIngredientEntity>>

    @Query("DELETE FROM recipe_ingredients WHERE recipe_id = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: String)

    // ── Recipe with ingredients (transaction) ─────────────────────────────

    @Transaction
    @Query("SELECT * FROM recipes WHERE uuid = :uuid")
    suspend fun getRecipeWithIngredients(uuid: String): RecipeWithIngredients?

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY title ASC")
    fun getAllRecipesWithIngredients(): Flow<List<RecipeWithIngredients>>
}
