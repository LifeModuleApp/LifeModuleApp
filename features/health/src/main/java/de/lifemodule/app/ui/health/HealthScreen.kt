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

package de.lifemodule.app.ui.health

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.data.health.DailySteps
import de.lifemodule.app.data.health.HcSdkStatus
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import java.time.Duration
import java.time.format.DateTimeFormatter
import de.lifemodule.app.feature.health.R
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun HealthScreen(
    navController: NavController,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current

    val permissionsGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()
    val isLoading          by viewModel.isLoading.collectAsStateWithLifecycle()
    val todaySteps         by viewModel.todaySteps.collectAsStateWithLifecycle()
    val weeklySteps        by viewModel.weeklySteps.collectAsStateWithLifecycle()
    val todayDistanceKm    by viewModel.todayDistanceKm.collectAsStateWithLifecycle()
    val avgHeartRate       by viewModel.avgHeartRate.collectAsStateWithLifecycle()
    val lastNightSleep     by viewModel.lastNightSleep.collectAsStateWithLifecycle()
    val bmr                by viewModel.bmr.collectAsStateWithLifecycle()
    val activeCalories     by viewModel.activeCalories.collectAsStateWithLifecycle()

    // ── Permission launcher ───────────────────────────────────────────────────
    // Always registered unconditionally (Compose rule). Uses a no-op contract
    // when Health Connect is unavailable so the launcher is safely inert.
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = remember { viewModel.manager.createPermissionContract() }
    ) { granted -> viewModel.onPermissionsResult(granted) }

    // Re-check permissions when screen resumes (e.g. after granting in system settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title      = stringResource(R.string.health_health_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── SDK not available / needs update ──────────────────────────────
            when (viewModel.sdkStatus) {
                HcSdkStatus.NOT_SUPPORTED -> item {
                    SdkUnavailableCard(
                        message = stringResource(R.string.health_health_health_connect_wird_auf_diesem),
                        accent  = accent
                    )
                }
                HcSdkStatus.NEEDS_UPDATE -> item {
                    SdkUnavailableCard(
                        message = stringResource(R.string.health_health_bitte_aktualisiere_die_health_connect),
                        accent  = accent
                    )
                }
                HcSdkStatus.AVAILABLE -> {
                    // ── Permission banner ─────────────────────────────────────
                    if (!permissionsGranted) {
                        item {
                            PermissionBannerCard(
                                accent        = accent,
                                onGrantClick  = {
                                    permissionsLauncher.launch(viewModel.manager.permissions)
                                }
                            )
                        }
                    }

                    // ── Loading indicator ─────────────────────────────────────
                    if (isLoading) {
                        item {
                            Box(
                                modifier            = Modifier.fillMaxWidth().height(80.dp),
                                contentAlignment    = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = accent)
                            }
                        }
                    } else if (permissionsGranted) {
                        // ── Today summary ─────────────────────────────────────
                        item {
                            Text(
                                text  = stringResource(R.string.health_health_heute),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            Row(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    modifier    = Modifier.weight(1f),
                                    icon        = Icons.Default.DirectionsRun,
                                    label       = stringResource(R.string.health_health_schritte),
                                    value       = "%,d".format(todaySteps),
                                    accent      = accent
                                )
                                StatCard(
                                    modifier    = Modifier.weight(1f),
                                    icon        = Icons.Default.DirectionsRun,
                                    label       = stringResource(R.string.health_health_distanz),
                                    value       = stringResource(R.string.health_health_distanz_wert, "%.2f".format(todayDistanceKm)),
                                    accent      = accent
                                )
                            }
                        }
                        item {
                            Row(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    modifier    = Modifier.weight(1f),
                                    icon        = Icons.Default.LocalFireDepartment,
                                    label       = stringResource(R.string.health_bmr),
                                    value       = "%,d".format(bmr),
                                    accent      = Color(0xFFFF9500)
                                )
                                StatCard(
                                    modifier    = Modifier.weight(1f),
                                    icon        = Icons.Default.LocalFireDepartment,
                                    label       = stringResource(R.string.health_active_calories),
                                    value       = "%,d".format(activeCalories),
                                    accent      = Color(0xFFFF9500)
                                )
                            }
                        }
                        item {
                            Row(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    modifier    = Modifier.weight(1f),
                                    icon        = Icons.Default.Favorite,
                                    label       = stringResource(R.string.health_health_herzrate),
                                    value       = if (avgHeartRate > 0) stringResource(R.string.health_health_herzrate_wert, avgHeartRate) else "-",
                                    accent      = Color(0xFFFF453A)
                                )
                                StatCard(
                                    modifier    = Modifier.weight(1f),
                                    icon        = Icons.Default.Hotel,
                                    label       = stringResource(R.string.health_health_schlaf),
                                    value       = lastNightSleep.formatSleep(),
                                    accent      = Color(0xFF5E5CE6)
                                )
                            }
                        }

                        // ── 7-day steps chart ─────────────────────────────────
                        if (weeklySteps.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text  = stringResource(R.string.health_health_schritte_letzte_7_tage),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                WeeklyStepsChart(
                                    days   = weeklySteps,
                                    accent = accent,
                                    modifier = Modifier.clickable {
                                        navController.navigate(AppRoute.HealthStepsDetail)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SdkUnavailableCard(message: String, accent: Color) {
    LMCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier        = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint   = accent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text  = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PermissionBannerCard(accent: Color, onGrantClick: () -> Unit) {
    LMCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = stringResource(R.string.health_health_title),
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text  = stringResource(R.string.health_health_erlaube_lifemodule_deine_schritte_distanz),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onGrantClick,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(
                    text  = stringResource(R.string.health_health_berechtigung_erteilen),
                    color = Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier : Modifier,
    icon     : ImageVector,
    label    : String,
    value    : String,
    accent   : Color
) {
    LMCard(modifier = modifier) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = accent,
                modifier           = Modifier.size(22.dp)
            )
            Text(
                text  = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 22.sp
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun WeeklyStepsChart(
    days: List<DailySteps>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val dayFormatter = DateTimeFormatter.ofPattern("E")

    LMCard(modifier = Modifier.fillMaxWidth().then(modifier)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (days.all { it.steps == 0L }) {
                Text(
                    text      = stringResource(R.string.health_health_noch_keine_schrittdaten_fuer_diese),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = Color.White.copy(alpha = 0.5f),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                val model = entryModelOf(
                    days.mapIndexed { i, d ->
                        entryOf(i.toFloat(), d.steps.toFloat())
                    }
                )
                Chart(
                    chart    = columnChart(),
                    model    = model,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Spacer(Modifier.height(8.dp))
                // Day labels below chart
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    days.forEach { d ->
                        Text(
                            text  = d.date.format(dayFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

// ── Duration helper ───────────────────────────────────────────────────────────

@Composable
private fun Duration.formatSleep(): String {
    if (isZero || isNegative) return "-"
    val h = toHours()
    val m = (toMinutes() % 60)
    return stringResource(R.string.health_health_schlaf_format, h, m)
}
