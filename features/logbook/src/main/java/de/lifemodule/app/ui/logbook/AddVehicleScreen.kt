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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.feature.logbook.R
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import timber.log.Timber

@Composable
fun AddVehicleScreen(
    navController: NavController,
    viewModel: LogbookViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current

    var name by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Navigate back after adding (vehicles list will auto-update via Flow)
    LaunchedEffect(saved) {
        if (saved) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.logbook_vehicle_title),
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
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.logbook_vehicle_name),
                modifier = Modifier.fillMaxWidth()
            )

            LMInput(
                value = licensePlate,
                onValueChange = { licensePlate = it },
                label = stringResource(R.string.logbook_vehicle_plate_optional),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addVehicle(
                            name = name.trim(),
                            licensePlate = licensePlate.takeIf { it.isNotBlank() }?.trim()
                        )
                        saved = true
                    } else {
                        Timber.w("[Logbook] Add vehicle validation failed: name is blank")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.logbook_vehicle_save))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
