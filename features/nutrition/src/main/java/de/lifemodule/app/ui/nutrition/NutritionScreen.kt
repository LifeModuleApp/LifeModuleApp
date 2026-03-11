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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.lifemodule.app.feature.nutrition.R
import androidx.compose.ui.res.stringResource

@Composable
fun NutritionScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val entries by viewModel.dailyEntries.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val accent = LocalAccentColor.current

    val totalKcal = entries.sumOf { it.foodItem.kcalPer100g * it.entry.gramsConsumed / 100.0 }
    val totalProtein = entries.sumOf { it.foodItem.proteinPer100g * it.entry.gramsConsumed / 100.0 }
    val totalCarbs = entries.sumOf { it.foodItem.carbsPer100g * it.entry.gramsConsumed / 100.0 }
    val totalFat = entries.sumOf { it.foodItem.fatPer100g * it.entry.gramsConsumed / 100.0 }

    val dateText = selectedDate.format(
        DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(Locale.getDefault())
    )

    // Delete confirmation
    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    pendingDelete?.let { onConfirm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.nutrition_nutrition_eintrag_loeschen)) },
            text = { Text(stringResource(R.string.nutrition_nutrition_eintrag_loeschen_text)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(); pendingDelete = null }) {
                    Text(stringResource(R.string.nutrition_nutrition_loeschen), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.nutrition_nutrition_abbrechen))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.nutrition_nutrition_titel),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.NutritionAddEntry) })
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Header
            item {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Summary Card
            item {
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.nutrition_nutrition_kcal_format, totalKcal.toInt()),
                            style = MaterialTheme.typography.headlineMedium,
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            MacroLabel(stringResource(R.string.nutrition_nutrition_eiweiss), totalProtein)
                            Spacer(modifier = Modifier.width(16.dp))
                            MacroLabel(stringResource(R.string.nutrition_nutrition_kohlenhydrate), totalCarbs)
                            Spacer(modifier = Modifier.width(16.dp))
                            MacroLabel(stringResource(R.string.nutrition_nutrition_fett), totalFat)
                        }
                    }
                }
            }

            // Entry List
            if (entries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nutrition_nutrition_noch_keine_eintraege_heute_tippe),
                        color = Secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }

            items(entries, key = { it.entry.uuid }) { entryWithFood ->
                val kcal = entryWithFood.foodItem.kcalPer100g * entryWithFood.entry.gramsConsumed / 100.0

                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail
                        if (!entryWithFood.foodItem.imagePath.isNullOrBlank()) {
                            AsyncImage(
                                model = entryWithFood.foodItem.imagePath,
                                contentDescription = entryWithFood.foodItem.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entryWithFood.foodItem.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.nutrition_nutrition_gramm_kcal_format, entryWithFood.entry.gramsConsumed.toInt(), kcal.toInt()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                        }
                        IconButton(onClick = { pendingDelete = { viewModel.deleteFoodEntry(entryWithFood.entry) } }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.nutrition_nutrition_loeschen_1),
                                tint = Destructive
                            )
                        }
                    }
                }
            }

            // Bottom spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun MacroLabel(label: String, grams: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${grams.toInt()}g",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Secondary
        )
    }
}
