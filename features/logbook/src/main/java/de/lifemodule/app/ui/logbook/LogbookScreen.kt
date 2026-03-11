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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.data.logbook.JourneyPurpose
import de.lifemodule.app.data.logbook.LogbookEntryEntity
import androidx.compose.ui.res.stringResource
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Accent
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Surface
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.feature.logbook.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LogbookScreen(
    navController: NavController,
    viewModel: LogbookViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.logbook_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(AppRoute.LogbookAdd) },
                containerColor = Accent,
                contentColor = Black
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.logbook_add_journey))
            }
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Vehicles header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.logbook_vehicles_count, vehicles.size),
                        color = Secondary,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { navController.navigate(AppRoute.LogbookVehicles) }) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.logbook_manage), color = Accent, fontSize = 14.sp)
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.logbook_empty),
                            color = Secondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(entries, key = { it.uuid }) { entry ->
                    LogbookEntryCard(entry, vehicles.firstOrNull { it.uuid == entry.vehicleId }?.name)
                }
            }
        }
    }
}

@Composable
private fun LogbookEntryCard(entry: LogbookEntryEntity, vehicleName: String?) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm") }
    val formattedDate = remember(entry.journeyDate) {
        Instant.ofEpochMilli(entry.journeyDate)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    val purposeColor = when (entry.purposeCode) {
        JourneyPurpose.BUSINESS -> Color(0xFF4CAF50)
        JourneyPurpose.COMMUTE -> Color(0xFFFFA726)
        JourneyPurpose.PRIVATE -> Secondary
    }
    val purposeLabel = when (entry.purposeCode) {
        JourneyPurpose.BUSINESS -> "Dienstlich"
        JourneyPurpose.COMMUTE -> "Arbeitsweg"
        JourneyPurpose.PRIVATE -> "Privat"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formattedDate, color = Secondary, fontSize = 12.sp)
                Text(
                    purposeLabel,
                    color = purposeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${entry.startLocation}  ->  ${entry.endLocation}",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${"%.1f".format(entry.distanceKm)} km",
                    color = Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                vehicleName?.let {
                    Text(it, color = Secondary, fontSize = 12.sp)
                }
            }
            if (entry.correctionOfUuid != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    " Korrektur",
                    color = Color(0xFFFF9800),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            entry.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Secondary, fontSize = 12.sp, maxLines = 2)
            }
        }
    }
}
