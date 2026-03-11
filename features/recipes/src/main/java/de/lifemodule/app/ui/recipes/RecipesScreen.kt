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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
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
import de.lifemodule.app.data.recipes.RecipeWithIngredients
import de.lifemodule.app.feature.recipes.R
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Accent
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Surface
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.components.LMTopBar

@Composable
fun RecipesScreen(
    navController: NavController,
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.recipes_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(AppRoute.RecipesAdd) },
                containerColor = Accent,
                contentColor = Black
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.recipes_fab_add))
            }
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (recipes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.recipes_empty),
                                color = Secondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(recipes, key = { it.recipe.uuid }) { recipeWithIngredients ->
                    RecipeCard(
                        recipeWithIngredients = recipeWithIngredients,
                        onClick = {
                            navController.navigate(AppRoute.RecipeDetail(recipeWithIngredients.recipe.uuid))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeCard(
    recipeWithIngredients: RecipeWithIngredients,
    onClick: () -> Unit
) {
    val recipe = recipeWithIngredients.recipe
    val ingredientCount = recipeWithIngredients.ingredients.size

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    recipe.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    recipe.category.name.lowercase().replaceFirstChar { it.titlecase() },
                    color = Secondary,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.recipes_servings, recipe.servings),
                    color = Secondary,
                    fontSize = 12.sp
                )
                recipe.prepTimeMinutes?.let { prep ->
                    Text(
                        stringResource(R.string.recipes_prep_time, prep),
                        color = Secondary,
                        fontSize = 12.sp
                    )
                }
                recipe.cookTimeMinutes?.let { cook ->
                    Text(
                        stringResource(R.string.recipes_cook_time, cook),
                        color = Secondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.recipes_ingredient_count, ingredientCount),
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
