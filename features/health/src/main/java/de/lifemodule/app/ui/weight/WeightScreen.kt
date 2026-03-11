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

package de.lifemodule.app.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMEmptyState
import de.lifemodule.app.ui.components.LMFAB
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import de.lifemodule.app.feature.health.R
import androidx.compose.ui.res.stringResource

@Composable
fun WeightScreen(
    navController: NavController,
    viewModel: WeightViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val latest by viewModel.latestEntry.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<de.lifemodule.app.data.weight.WeightEntryEntity?>(null) }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.health_weight_gewicht),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.WeightAdd) })
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
            // Current weight card
            item {
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.health_weight_aktuelles_gewicht),
                            style = MaterialTheme.typography.titleSmall,
                            color = Secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (latest != null) stringResource(R.string.health_weight_kg_format, "%.1f".format(latest!!.weightKg)) else "-",
                            fontSize = 48.sp,
                            color = accent
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.health_weight_verlauf),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (entries.isEmpty()) {
                item {
                    LMEmptyState(
                        emoji = "⚖️",
                        title = stringResource(R.string.health_weight_leer_titel),
                        subtitle = stringResource(R.string.health_weight_leer_subtitel)
                    )
                }
            }

            items(entries) { entry ->
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.health_weight_kg_format, "%.1f".format(entry.weightKg)),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = entry.date +
                                        if (entry.afterWaking) stringResource(R.string.health_weight_nach_aufstehen) else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                            if (entry.notes.isNotBlank()) {
                                Text(
                                    text = entry.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        IconButton(onClick = { pendingDelete = entry }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.health_weight_loeschen),
                                tint = Secondary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Delete confirmation dialog
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    stringResource(R.string.health_weight_loeschen_titel),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    stringResource(R.string.health_weight_loeschen_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entry)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.health_weight_loeschen), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.health_weight_abbrechen))
                }
            },
            containerColor = Surface
        )
    }
}
