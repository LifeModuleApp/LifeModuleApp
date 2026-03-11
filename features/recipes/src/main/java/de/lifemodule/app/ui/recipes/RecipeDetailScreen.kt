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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.data.recipes.RecipeCategory
import de.lifemodule.app.feature.recipes.R
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import timber.log.Timber

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    navController: NavController,
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val recipeWithIngredients = recipes.firstOrNull { it.recipe.uuid == recipeId }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val categoryLabels = mapOf(
        RecipeCategory.BAKING to stringResource(R.string.recipes_cat_baking),
        RecipeCategory.COOKING to stringResource(R.string.recipes_cat_cooking),
        RecipeCategory.GRILLING to stringResource(R.string.recipes_cat_grilling),
        RecipeCategory.DRINKS to stringResource(R.string.recipes_cat_drinks),
        RecipeCategory.RAW to stringResource(R.string.recipes_cat_raw),
        RecipeCategory.OTHER to stringResource(R.string.recipes_cat_other)
    )

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.recipes_detail_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        if (recipeWithIngredients == null) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.recipes_detail_not_found), color = Secondary, fontSize = 14.sp)
            }
        } else {
            val recipe = recipeWithIngredients.recipe
            val ingredients = recipeWithIngredients.ingredients.sortedBy { it.orderIndex }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Title + category
                Text(recipe.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    categoryLabels[recipe.category] ?: recipe.category.name,
                    color = accent,
                    fontSize = 13.sp
                )

                // Meta info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetaChip(stringResource(R.string.recipes_detail_servings), "${recipe.servings}")
                        recipe.prepTimeMinutes?.let { MetaChip(stringResource(R.string.recipes_detail_prep_time), "${it} min") }
                        recipe.cookTimeMinutes?.let { MetaChip(stringResource(R.string.recipes_detail_cook_time), "${it} min") }
                    }
                }

                // Instructions
                Text(stringResource(R.string.recipes_detail_instructions), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(recipe.instructions, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)

                // Ingredients
                if (ingredients.isNotEmpty()) {
                    Text(stringResource(R.string.recipes_detail_ingredients), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ingredients.forEach { ing ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ing.name, color = Color.White, fontSize = 14.sp)
                                    Text(
                                        "${"%.1f".format(ing.quantity).trimEnd('0').trimEnd('.')} ${ing.unit}",
                                        color = Secondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Notes
                recipe.notes?.let { n ->
                    Text(stringResource(R.string.recipes_detail_notes), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(n, color = Secondary, fontSize = 14.sp)
                }

                Spacer(Modifier.height(8.dp))

                // Delete button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Destructive),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Destructive)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.recipes_detail_delete))
                }

                Spacer(Modifier.height(16.dp))
            }

            // Delete confirmation dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(stringResource(R.string.recipes_detail_delete_title)) },
                    text = { Text(stringResource(R.string.recipes_detail_delete_text, recipe.title)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            viewModel.deleteRecipe(recipe)
                            Timber.i("[Recipes] Deleted recipe '%s' (%s)", recipe.title, recipe.uuid)
                            navController.popBackStack()
                        }) {
                            Text(stringResource(R.string.recipes_detail_delete_confirm), color = Destructive)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(stringResource(R.string.recipes_detail_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(label, color = Secondary, fontSize = 11.sp)
    }
}
