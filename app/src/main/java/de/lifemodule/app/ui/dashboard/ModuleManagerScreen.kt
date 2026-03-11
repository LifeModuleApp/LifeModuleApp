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

package de.lifemodule.app.ui.dashboard

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Surface

@Composable
fun ModuleManagerScreen(
    navController: NavController,
    modulePreferences: ModulePreferences
) {
    val accent = LocalAccentColor.current
    val scope = rememberCoroutineScope()
    
    // Tracks the module that the user just tried to disable, opening the dialog
    var pendingDisableModule by remember { mutableStateOf<AppModule?>(null) }
    
    val moduleStates = remember {
        mutableStateMapOf<AppModule, Boolean>().apply {
            AppModule.entries.forEach { module ->
                this[module] = modulePreferences.isModuleEnabled(module)
            }
        }
    }

    val groupedModules = remember {
        AppModule.entries.groupBy { it.category }
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.app_dashboard_module_verwalten_1),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedModules.forEach { (category, modules) ->
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(category.displayNameRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(modules) { module ->
                    LMCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = module.emoji,
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = module.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(module.displayNameRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(category.displayNameRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                            }
                            Switch(
                                checked = moduleStates[module] ?: module.defaultEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        // User wants to disable -> Show Prompt
                                        pendingDisableModule = module
                                    } else {
                                        // User enables -> Just enable it directly
                                        moduleStates[module] = true
                                        modulePreferences.setModuleEnabled(module, true)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = accent.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Secondary,
                                    uncheckedTrackColor = Secondary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        
        // --- Data Wipe Prompt ---
        pendingDisableModule?.let { moduleToDisable ->
            AlertDialog(
                onDismissRequest = { pendingDisableModule = null },
                title = { Text(text = stringResource(R.string.app_module_disable_title), style = MaterialTheme.typography.titleMedium) },
                text = { Text(text = stringResource(R.string.app_module_disable_text), style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            modulePreferences.wipeModuleData(moduleToDisable)
                            moduleStates[moduleToDisable] = false
                            modulePreferences.setModuleEnabled(moduleToDisable, false)
                            pendingDisableModule = null
                        }
                    }) {
                        Text(stringResource(R.string.app_module_disable_delete), color = Destructive)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        moduleStates[moduleToDisable] = false
                        modulePreferences.setModuleEnabled(moduleToDisable, false)
                        pendingDisableModule = null
                    }) {
                        Text(stringResource(R.string.app_module_disable_hide), color = accent)
                    }
                },
                containerColor = Surface
            )
        }
    }
}
