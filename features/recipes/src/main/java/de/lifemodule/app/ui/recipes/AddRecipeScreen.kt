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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.data.recipes.RecipeCategory
import de.lifemodule.app.feature.recipes.R
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Border
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    navController: NavController,
    viewModel: RecipesViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("4") }
    var prepTime by remember { mutableStateOf("") }
    var cookTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RecipeCategory.COOKING) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Dynamic ingredient list
    var ingredientRows by remember {
        mutableStateOf(listOf(IngredientRowState()))
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess == true) {
            viewModel.clearSaveState()
            navController.popBackStack()
        }
    }

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
                title = stringResource(R.string.recipes_add_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            LMInput(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.recipes_add_recipe_name),
                modifier = Modifier.fillMaxWidth()
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = categoryLabels[selectedCategory] ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.recipes_add_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Border,
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Secondary,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    RecipeCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(categoryLabels[cat] ?: cat.name) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LMInput(
                    value = servings,
                    onValueChange = { servings = it },
                    label = stringResource(R.string.recipes_add_servings),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                LMInput(
                    value = prepTime,
                    onValueChange = { prepTime = it },
                    label = stringResource(R.string.recipes_add_prep_time),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                LMInput(
                    value = cookTime,
                    onValueChange = { cookTime = it },
                    label = stringResource(R.string.recipes_add_cook_time),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }

            LMInput(
                value = instructions,
                onValueChange = { instructions = it },
                label = stringResource(R.string.recipes_add_instructions),
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                singleLine = false
            )

            // Ingredients section
            Text(stringResource(R.string.recipes_add_ingredients_title), color = accent, fontSize = 14.sp)

            ingredientRows.forEachIndexed { index, row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LMInput(
                                value = row.name,
                                onValueChange = { v ->
                                    ingredientRows = ingredientRows.toMutableList().also {
                                        it[index] = it[index].copy(name = v)
                                    }
                                },
                                label = stringResource(R.string.recipes_add_ingredient),
                                modifier = Modifier.weight(1f)
                            )
                            if (ingredientRows.size > 1) {
                                IconButton(onClick = {
                                    ingredientRows = ingredientRows.toMutableList().also {
                                        it.removeAt(index)
                                    }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.recipes_add_remove), tint = Destructive)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LMInput(
                                value = row.quantity,
                                onValueChange = { v ->
                                    ingredientRows = ingredientRows.toMutableList().also {
                                        it[index] = it[index].copy(quantity = v)
                                    }
                                },
                                label = stringResource(R.string.recipes_add_quantity),
                                modifier = Modifier.weight(1f),
                                keyboardType = KeyboardType.Decimal
                            )
                            LMInput(
                                value = row.unit,
                                onValueChange = { v ->
                                    ingredientRows = ingredientRows.toMutableList().also {
                                        it[index] = it[index].copy(unit = v)
                                    }
                                },
                                label = stringResource(R.string.recipes_add_unit),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    ingredientRows = ingredientRows + IngredientRowState()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.recipes_add_ingredient_add), color = accent)
            }

            LMInput(
                value = notes,
                onValueChange = { notes = it },
                label = stringResource(R.string.recipes_add_notes_optional),
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Button(
                onClick = {
                    val srv = servings.toIntOrNull()
                    val prep = prepTime.toIntOrNull()
                    val cook = cookTime.toIntOrNull()
                    val validIngredients = ingredientRows.filter { it.name.isNotBlank() }

                    if (title.isNotBlank() && instructions.isNotBlank() && srv != null && srv > 0) {
                        viewModel.addRecipe(
                            title = title.trim(),
                            instructions = instructions.trim(),
                            category = selectedCategory,
                            servings = srv,
                            prepTimeMinutes = prep,
                            cookTimeMinutes = cook,
                            notes = notes.takeIf { it.isNotBlank() },
                            ingredients = validIngredients.map {
                                IngredientInput(
                                    name = it.name.trim(),
                                    quantity = it.quantity.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    unit = it.unit.trim()
                                )
                            }
                        )
                    } else {
                        Timber.w("[Recipes] Add recipe validation failed: title=%s, servings=%s, ingredients=%d",
                            title, servings, validIngredients.size)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
                        && instructions.isNotBlank()
                        && (servings.toIntOrNull() ?: 0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.recipes_add_save))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Local UI state for a single ingredient row. */
private data class IngredientRowState(
    val name: String = "",
    val quantity: String = "",
    val unit: String = ""
)
