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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.CommunityBadge
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.nutrition.R
import androidx.compose.ui.res.stringResource

@Composable
fun FoodDatabaseScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val foodItems by viewModel.allFoodItems.collectAsStateWithLifecycle()
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

    // Delete or Archive confirmation
    var pendingActionFood by remember { mutableStateOf<de.lifemodule.app.data.nutrition.FoodItemEntity?>(null) }
    pendingActionFood?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingActionFood = null },
            title = { Text(stringResource(R.string.nutrition_nutrition_lebensmittel_loeschen)) },
            text = { Text(stringResource(R.string.nutrition_nutrition_lebensmittel_loeschen_text)) },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.archiveFoodItem(item)
                    pendingActionFood = null 
                }) {
                    Text(stringResource(R.string.nutrition_nutrition_archivieren), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { pendingActionFood = null }) {
                        Text(stringResource(R.string.nutrition_nutrition_abbrechen), color = Secondary)
                    }
                    TextButton(onClick = { 
                        viewModel.deleteFoodItem(item)
                        pendingActionFood = null 
                    }) {
                        Text(stringResource(R.string.nutrition_nutrition_loeschen_total), color = Destructive)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.nutrition_nutrition_lebensmitteldb),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.NutritionAddFood) })
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
                    text = stringResource(R.string.nutrition_nutrition_lebensmittel_count_total, filteredItems.size, foodItems.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            items(filteredItems, key = { it.uuid }) { item ->
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (item.importSource == ImportSource.COMMUNITY_HUB) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CommunityBadge()
                                }
                            }
                            Text(
                                text = stringResource(R.string.nutrition_nutrition_makros_format, item.proteinPer100g.toInt(), item.carbsPer100g.toInt(), item.fatPer100g.toInt(), item.kcalPer100g.toInt()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                            if (!item.barcode.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.nutrition_nutrition_barcode_display, item.barcode!!),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                            }
                        }
                        IconButton(onClick = { pendingActionFood = item }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.nutrition_nutrition_loeschen),
                                tint = Destructive
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
