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

package de.lifemodule.app.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.lifemodule.app.data.nutrition.FoodItemEntity
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.nutrition.R
import androidx.compose.ui.res.stringResource

@Composable
fun AddFoodEntryScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val foodItems by viewModel.allFoodItems.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<FoodItemEntity?>(null) }
    var gramsText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems by remember(foodItems, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) foodItems
            else foodItems.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.barcode?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.nutrition_nutrition_mahlzeit_hinzufuegen),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Scan barcode to find existing food
                LMFAB(
                    onClick = { navController.navigate(AppRoute.NutritionScannerEntry) },
                    icon = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.nutrition_nutrition_barcode_scannen)
                )
                LMFAB(
                    onClick = { navController.navigate(AppRoute.NutritionFoodDb) },
                    icon = Icons.Default.Storage,
                    contentDescription = stringResource(R.string.nutrition_nutrition_lebensmitteldatenbank)
                )
            }
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search bar
            item {
                LMInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.nutrition_nutrition_suchen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.nutrition_nutrition_lebensmittel_anzahl, filteredItems.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            if (filteredItems.isEmpty() && foodItems.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 32.dp)) {
                        Text(
                            text = stringResource(R.string.nutrition_nutrition_keine_lebensmittel_in_der_datenbank),
                            color = Secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { navController.navigate(AppRoute.NutritionAddFood) }
                        ) {
                            Text(stringResource(R.string.nutrition_nutrition_neues_lebensmittel_anlegen))
                        }
                    }
                }
            }

            items(filteredItems, key = { it.uuid }) { item ->
                LMCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedItem = item
                            gramsText = ""     // always clear grams when switching items
                            showDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail
                        if (!item.imagePath.isNullOrBlank()) {
                            AsyncImage(
                                model = item.imagePath,
                                contentDescription = item.name,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.nutrition_nutrition_kcal_pro_100g, item.kcalPer100g.toInt()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Grams input dialog
    if (showDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                gramsText = ""
            },
            title = {
                Text(selectedItem!!.name)
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.nutrition_nutrition_kcal_pro_100g, selectedItem!!.kcalPer100g.toInt()),
                        color = Secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LMInput(
                        value = gramsText,
                        onValueChange = { gramsText = it },
                        label = stringResource(R.string.nutrition_nutrition_gramm),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val grams = gramsText.toDoubleOrNull() ?: 0.0
                    if (grams > 0) {
                        val kcal = selectedItem!!.kcalPer100g * grams / 100.0
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.nutrition_nutrition_kcal_ergebnis, kcal.toInt()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val grams = gramsText.toDoubleOrNull()
                        if (grams != null && grams > 0) {
                            viewModel.addFoodEntry(selectedItem!!.uuid, grams)
                            showDialog = false
                            gramsText = ""
                            navController.popBackStack()
                        }
                    }
                ) { Text(stringResource(R.string.nutrition_nutrition_speichern)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        gramsText = ""
                    }
                ) { Text(stringResource(R.string.nutrition_nutrition_abbrechen)) }
            }
        )
    }
}
