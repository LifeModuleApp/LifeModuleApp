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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import de.lifemodule.app.data.weight.WeightEntryEntity
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.health.R
import androidx.compose.ui.res.stringResource

@Composable
fun AddWeightScreen(
    navController: NavController,
    viewModel: WeightViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    var weight by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var afterWaking by remember { mutableStateOf(false) }
    var timeOfDay by remember { mutableStateOf("MORNING") }

    val timeOptions = listOf("MORNING" to stringResource(R.string.health_weight_morgens), "AFTERNOON" to stringResource(R.string.health_weight_mittags), "EVENING" to stringResource(R.string.health_weight_abends))

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.health_weight_gewicht_erfassen),
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
            Spacer(modifier = Modifier.height(8.dp))

            LMInput(
                value = weight,
                onValueChange = { weight = it },
                label = stringResource(R.string.health_weight_gewicht_kg),
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Decimal
            )

            // Time of day chips
            Text(
                text = stringResource(R.string.health_weight_tageszeit),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeOptions.forEach { (value, label) ->
                    FilterChip(
                        selected = timeOfDay == value,
                        onClick = { timeOfDay = value },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.3f),
                            selectedLabelColor = accent,
                            labelColor = Secondary
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = afterWaking,
                    onCheckedChange = { afterWaking = it },
                    colors = CheckboxDefaults.colors(checkedColor = accent)
                )
                Text(
                    text = stringResource(R.string.health_weight_direkt_nach_aufstehen),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LMInput(
                value = notes,
                onValueChange = { notes = it },
                label = stringResource(R.string.health_weight_notizen_optional),
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Button(
                onClick = {
                    weight.replace(",", ".").toDoubleOrNull()?.let { w ->
                        viewModel.addEntry(
                            WeightEntryEntity(
                                date = viewModel.today().toString(),
                                weightKg = w,
                                timeOfDay = timeOfDay,
                                afterWaking = afterWaking,
                                notes = notes
                            )
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent
                )
            ) {
                Text(stringResource(R.string.health_weight_speichern))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
