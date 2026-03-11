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

package de.lifemodule.app.ui.logbook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.data.logbook.JourneyPurpose
import de.lifemodule.app.feature.logbook.R
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLogbookEntryScreen(
    navController: NavController,
    viewModel: LogbookViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    var startLocation by remember { mutableStateOf("") }
    var endLocation by remember { mutableStateOf("") }
    var distanceKm by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPurpose by remember { mutableStateOf(JourneyPurpose.BUSINESS) }
    var selectedVehicleUuid by remember { mutableStateOf<String?>(null) }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var purposeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess == true) {
            viewModel.clearSaveState()
            navController.popBackStack()
        }
    }

    // Auto-select first vehicle if only one exists
    LaunchedEffect(vehicles) {
        if (selectedVehicleUuid == null && vehicles.size == 1) {
            selectedVehicleUuid = vehicles.first().uuid
        }
    }

    val purposeLabels = mapOf(
        JourneyPurpose.BUSINESS to stringResource(R.string.logbook_add_purpose_business),
        JourneyPurpose.COMMUTE to stringResource(R.string.logbook_add_purpose_commute),
        JourneyPurpose.PRIVATE to stringResource(R.string.logbook_add_purpose_private)
    )

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.logbook_add_title),
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
                value = startLocation,
                onValueChange = { startLocation = it },
                label = stringResource(R.string.logbook_add_start_location),
                modifier = Modifier.fillMaxWidth()
            )

            LMInput(
                value = endLocation,
                onValueChange = { endLocation = it },
                label = stringResource(R.string.logbook_add_end_location),
                modifier = Modifier.fillMaxWidth()
            )

            LMInput(
                value = distanceKm,
                onValueChange = { distanceKm = it },
                label = stringResource(R.string.logbook_add_distance_km),
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Decimal
            )

            // Purpose dropdown
            ExposedDropdownMenuBox(
                expanded = purposeDropdownExpanded,
                onExpandedChange = { purposeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = purposeLabels[selectedPurpose] ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.logbook_add_purpose)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(purposeDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = de.lifemodule.app.ui.theme.Border,
                        focusedLabelColor = accent,
                        unfocusedLabelColor = Secondary,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = purposeDropdownExpanded,
                    onDismissRequest = { purposeDropdownExpanded = false }
                ) {
                    JourneyPurpose.entries.forEach { purpose ->
                        DropdownMenuItem(
                            text = { Text(purposeLabels[purpose] ?: purpose.name) },
                            onClick = {
                                selectedPurpose = purpose
                                purposeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Vehicle dropdown
            if (vehicles.isEmpty()) {
                Text(
                    stringResource(R.string.logbook_add_no_vehicle),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = vehicleDropdownExpanded,
                    onExpandedChange = { vehicleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = vehicles.firstOrNull { it.uuid == selectedVehicleUuid }?.name ?: stringResource(R.string.logbook_add_select_vehicle),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.logbook_add_vehicle_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vehicleDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = de.lifemodule.app.ui.theme.Border,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = Secondary,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleDropdownExpanded,
                        onDismissRequest = { vehicleDropdownExpanded = false }
                    ) {
                        vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.name}${v.licensePlate?.let { " ($it)" } ?: ""}") },
                                onClick = {
                                    selectedVehicleUuid = v.uuid
                                    vehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            LMInput(
                value = notes,
                onValueChange = { notes = it },
                label = stringResource(R.string.logbook_add_notes_optional),
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            Button(
                onClick = {
                    val km = distanceKm.replace(",", ".").toDoubleOrNull()
                    val vId = selectedVehicleUuid
                    if (km != null && km > 0 && startLocation.isNotBlank() && endLocation.isNotBlank() && vId != null) {
                        viewModel.addEntry(
                            startLocation = startLocation.trim(),
                            endLocation = endLocation.trim(),
                            distanceKm = km,
                            purpose = selectedPurpose,
                            vehicleId = vId,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    } else {
                        Timber.w("[Logbook] Add entry validation failed: km=%s, start=%s, end=%s, vehicle=%s",
                            distanceKm, startLocation, endLocation, vId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = startLocation.isNotBlank() && endLocation.isNotBlank()
                        && distanceKm.replace(",", ".").toDoubleOrNull() != null
                        && selectedVehicleUuid != null,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.logbook_add_save))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
