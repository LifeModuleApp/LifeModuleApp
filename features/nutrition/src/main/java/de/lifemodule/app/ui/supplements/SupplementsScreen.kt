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

import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import de.lifemodule.app.ui.components.CommunityBadge
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.data.ImportSource
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.lifemodule.app.feature.nutrition.R
import androidx.compose.ui.res.stringResource

@Composable
fun SupplementsScreen(
    navController: NavController,
    viewModel: SupplementViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    val activeSupplements by viewModel.activeSupplements.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dailyTotals by viewModel.dailyIngredientTotals.collectAsStateWithLifecycle()

    LaunchedEffect(activeSupplements) {
        if (activeSupplements.isNotEmpty()) {
            viewModel.ensureTodayLogs()
        }
    }

    val takenCount = todayLogs.count { it.log.taken }
    val totalCount = todayLogs.size

    val dateText = selectedDate.format(
        DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(Locale.getDefault())
    )

    // Delete or Archive confirmation
    var pendingActionSupplement by remember { mutableStateOf<de.lifemodule.app.data.supplements.SupplementEntity?>(null) }
    pendingActionSupplement?.let { supplement ->
        AlertDialog(
            onDismissRequest = { pendingActionSupplement = null },
            title = { Text(stringResource(R.string.nutrition_supplements_supplement_loeschen)) },
            text = { Text(stringResource(R.string.nutrition_supplements_supplement_loeschen_text)) },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.archiveSupplement(supplement)
                    pendingActionSupplement = null 
                }) {
                    Text(stringResource(R.string.nutrition_supplements_archivieren), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { pendingActionSupplement = null }) {
                        Text(stringResource(R.string.nutrition_supplements_abbrechen), color = Secondary)
                    }
                    TextButton(onClick = { 
                        viewModel.deleteSupplement(supplement)
                        pendingActionSupplement = null 
                    }) {
                        Text(stringResource(R.string.nutrition_supplements_loeschen_total), color = Destructive)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.nutrition_supplements_supplements),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            LMFAB(onClick = { navController.navigate(AppRoute.SupplementsAdd) })
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
            item {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Progress
            item {
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.nutrition_supplements_genommen_format, takenCount, totalCount),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (takenCount == totalCount && totalCount > 0) accent
                            else MaterialTheme.colorScheme.primary
                        )
                        if (totalCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (takenCount == totalCount) stringResource(R.string.nutrition_supplements_alles_erledigt)
                                else stringResource(R.string.nutrition_supplements_noch_offen, totalCount - takenCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                        }
                    }
                }
            }

            // Daily totals
            if (dailyTotals.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nutrition_supplements_tages_aufnahme),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    LMCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            dailyTotals.forEachIndexed { index, total ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = total.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatAmount(total.totalAmount, total.unit),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = accent
                                    )
                                    if (total.maxRvsPct != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${total.maxRvsPct.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Secondary
                                        )
                                    }
                                }
                                if (index < dailyTotals.size - 1) {
                                    HorizontalDivider(
                                        color = Secondary.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Checklist header
            if (todayLogs.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nutrition_supplements_einnahme_checkliste),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (todayLogs.isEmpty() && activeSupplements.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nutrition_supplements_noch_keine_supplements_eingerichtet_tippe),
                        color = Secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }

            items(todayLogs, key = { it.log.uuid }) { logWithSupplement ->
                val log = logWithSupplement.log
                val supplement = logWithSupplement.supplement

                LMCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate(AppRoute.SupplementDetail(supplement.uuid))
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle
                        IconButton(onClick = { viewModel.toggleTaken(log) }) {
                            Icon(
                                imageVector = if (log.taken) Icons.Filled.CheckCircle
                                else Icons.Outlined.Circle,
                                contentDescription = if (log.taken) stringResource(R.string.nutrition_supplements_genommen) else stringResource(R.string.nutrition_supplements_nicht_genommen),
                                tint = if (log.taken) accent else Secondary
                            )
                        }

                        // Thumbnail
                        if (!supplement.imagePath.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(supplement.imagePath),
                                contentDescription = supplement.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = supplement.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (log.taken) Secondary else MaterialTheme.colorScheme.onSurface
                                )
                                if (supplement.importSource == ImportSource.COMMUNITY_HUB) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CommunityBadge()
                                }
                            }
                            Row {
                                Text(
                                    text = supplement.dosage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = timeOfDayLabel(supplement.timeOfDay),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                                if (supplement.timesPerDay > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "×${supplement.timesPerDay}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = accent
                                    )
                                }
                            }
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.nutrition_supplements_details),
                            tint = Secondary
                        )
                        // Delete / Archive button
                        IconButton(onClick = { pendingActionSupplement = supplement }) {
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
}

@Composable
private fun timeOfDayLabel(timeOfDay: String): String = when (timeOfDay) {
    "morning" -> stringResource(R.string.nutrition_supplements_morgens_1)
    "midday" -> stringResource(R.string.nutrition_supplements_mittags_1)
    "evening" -> stringResource(R.string.nutrition_supplements_abends_1)
    else -> stringResource(R.string.nutrition_supplements_jederzeit_1)
}

private fun formatAmount(amount: Double, unit: String): String {
    return if (amount == amount.toLong().toDouble()) {
        "${amount.toLong()} $unit"
    } else {
        "${"%.1f".format(amount)} $unit"
    }
}
