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

package de.lifemodule.app.ui.supplements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.nutrition.R
import androidx.compose.ui.res.stringResource

@Composable
fun SupplementDetailScreen(
    navController: NavController,
    supplementId: String,
    viewModel: SupplementViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val allSupplements by viewModel.allSupplements.collectAsStateWithLifecycle()
    val supplement = allSupplements.find { it.uuid == supplementId }
    val ingredients by viewModel.getIngredientsForSupplement(supplementId).collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    // Delete confirmation
    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    pendingDelete?.let { onConfirm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.nutrition_supplements_inhaltsstoff_loeschen)) },
            text = { Text(stringResource(R.string.nutrition_supplements_inhaltsstoff_loeschen_text)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(); pendingDelete = null }) {
                    Text(stringResource(R.string.nutrition_supplements_loeschen), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.nutrition_supplements_abbrechen))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = supplement?.name ?: stringResource(R.string.nutrition_supplements_inhaltsstoffe),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { showAddDialog = true })
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
            // Supplement info
            if (supplement != null) {
                item {
                    LMCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = supplement.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.nutrition_supplements_dosierung_format, supplement.dosage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                            if (!supplement.notes.isNullOrBlank()) {
                                Text(
                                    text = supplement.notes!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.nutrition_supplements_inhaltsstoffe_count, ingredients.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = Secondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            if (ingredients.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nutrition_supplements_noch_keine_inhaltsstoffe_eingetragen_tippe),
                        color = Secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            items(ingredients, key = { it.uuid }) { ingredient ->
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                Text(
                                    text = formatAmount(ingredient.amount, ingredient.unit),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = accent
                                )
                                if (ingredient.rvsPct != null) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.nutrition_supplements_nrv_format, ingredient.rvsPct!!.toInt()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Secondary
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { pendingDelete = { viewModel.deleteIngredient(ingredient) } }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.nutrition_supplements_loeschen),
                                tint = Destructive
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Add ingredient dialog
    if (showAddDialog) {
        AddIngredientDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, unit, rvsPct ->
                viewModel.addIngredient(
                    supplementId = supplementId,
                    name = name,
                    amount = amount,
                    unit = unit,
                    rvsPct = rvsPct
                )
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, unit: String, rvsPct: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("mg") }
    var rvsPct by remember { mutableStateOf("") }
    var unitExpanded by remember { mutableStateOf(false) }

    val units = listOf("mg", "µg", "g", "IU", "ml")
    val isValid = name.isNotBlank() && amount.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nutrition_supplements_inhaltsstoff_hinzufuegen)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LMInput(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.nutrition_supplements_name_vitamin),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LMInput(
                        value = amount,
                        onValueChange = { amount = it },
                        label = stringResource(R.string.nutrition_supplements_menge),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(0.8f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.nutrition_supplements_einheit)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Secondary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                LMInput(
                    value = rvsPct,
                    onValueChange = { rvsPct = it },
                    label = stringResource(R.string.nutrition_supplements_nrv_optional),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            name,
                            amount.toDouble(),
                            unit,
                            rvsPct.toDoubleOrNull()
                        )
                    }
                },
                enabled = isValid
            ) { Text(stringResource(R.string.nutrition_supplements_hinzufuegen)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.nutrition_supplements_abbrechen)) }
        }
    )
}

private fun formatAmount(amount: Double, unit: String): String {
    return if (amount == amount.toLong().toDouble()) {
        "${amount.toLong()} $unit"
    } else {
        "${"%.1f".format(amount)} $unit"
    }
}
