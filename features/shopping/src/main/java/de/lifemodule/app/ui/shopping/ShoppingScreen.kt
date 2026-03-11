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

package de.lifemodule.app.ui.shopping

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMEmptyState
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.shopping.R
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource

@Composable
fun ShoppingScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    var newItemName by remember { mutableStateOf("") }

    val uncheckedItems = items.filter { !it.checked }
    val checkedItems = items.filter { it.checked }

    // Delete confirmation
    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showClearCheckedDialog by remember { mutableStateOf(false) }

    pendingDelete?.let { onConfirm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.shopping_shopping_artikel_loeschen)) },
            text = { Text(stringResource(R.string.shopping_shopping_artikel_loeschen_text)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(); pendingDelete = null }) {
                    Text(stringResource(R.string.shopping_shopping_loeschen), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.shopping_shopping_abbrechen))
                }
            }
        )
    }

    if (showClearCheckedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCheckedDialog = false },
            title = { Text(stringResource(R.string.shopping_shopping_erledigte_loeschen_titel)) },
            text = { Text(stringResource(R.string.shopping_shopping_erledigte_loeschen_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearChecked(); showClearCheckedDialog = false }) {
                    Text(stringResource(R.string.shopping_shopping_loeschen), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCheckedDialog = false }) {
                    Text(stringResource(R.string.shopping_shopping_abbrechen))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.shopping_shopping_einkaufsliste),
                onBackClick = { navController.popBackStack() }
            )
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
            // Add item row
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LMInput(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = stringResource(R.string.shopping_shopping_neuer_artikel),
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newItemName.isNotBlank()) {
                                    viewModel.addItem(newItemName.trim())
                                    newItemName = ""
                                }
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newItemName.isNotBlank()) {
                                viewModel.addItem(newItemName.trim())
                                newItemName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, stringResource(R.string.shopping_shopping_hinzufuegen), tint = accent)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Empty state
            if (uncheckedItems.isEmpty() && checkedItems.isEmpty()) {
                item {
                    LMEmptyState(
                        emoji = "🛒",
                        title = stringResource(R.string.shopping_shopping_leer_titel),
                        subtitle = stringResource(R.string.shopping_shopping_leer_subtitel)
                    )
                }
            }

            // Unchecked items
            items(uncheckedItems, key = { it.uuid }) { item ->
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { viewModel.toggleChecked(item) },
                            colors = CheckboxDefaults.colors(checkedColor = accent)
                        )
                        Text(
                            text = if (item.quantity.isNotBlank()) "${item.name} (${item.quantity})" else item.name,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { pendingDelete = { viewModel.deleteItem(item) } }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.shopping_shopping_loeschen), tint = Secondary)
                        }
                    }
                }
            }

            // Checked items section
            if (checkedItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_shopping_erledigt, checkedItems.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showClearCheckedDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, stringResource(R.string.shopping_shopping_erledigte_loeschen), tint = Secondary)
                        }
                    }
                }

                items(checkedItems, key = { it.uuid }) { item ->
                    LMCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = true,
                                onCheckedChange = { viewModel.toggleChecked(item) },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Text(
                                text = item.name,
                                modifier = Modifier.weight(1f),
                                color = Secondary,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
