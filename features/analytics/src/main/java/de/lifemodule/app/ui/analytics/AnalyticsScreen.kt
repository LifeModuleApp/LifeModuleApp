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

package de.lifemodule.app.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.entryModelOf
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import de.lifemodule.app.feature.analytics.R

@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val todayMood by viewModel.todayMood.collectAsStateWithLifecycle()
    val recentMoods by viewModel.recentMoods.collectAsStateWithLifecycle()
    val moodRange by viewModel.moodRange.collectAsStateWithLifecycle()
    val todayNutrition by viewModel.todayNutrition.collectAsStateWithLifecycle()
    val habitStreaks by viewModel.habitStreaks.collectAsStateWithLifecycle()
    val habitWeekGrid by viewModel.habitWeekGrid.collectAsStateWithLifecycle()
    val supplementStreaks by viewModel.supplementStreaks.collectAsStateWithLifecycle()
    val exerciseNames by viewModel.exerciseNames.collectAsStateWithLifecycle()
    val selectedExercise by viewModel.selectedExercise.collectAsStateWithLifecycle()
    val exerciseProgress by viewModel.exerciseProgress.collectAsStateWithLifecycle()
    val totalWorkouts by viewModel.totalWorkouts.collectAsStateWithLifecycle()
    val recentWorkouts by viewModel.recentWorkouts.collectAsStateWithLifecycle()
    val totalActions by viewModel.totalActions.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    val weightTrend by viewModel.weightTrend.collectAsStateWithLifecycle()
    val weightForecast by viewModel.weightForecast.collectAsStateWithLifecycle()
    val weightRange by viewModel.weightRange.collectAsStateWithLifecycle()
    val workWeekHours by viewModel.workWeekHours.collectAsStateWithLifecycle()

    val todaySteps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val todayCalories by viewModel.todayCalories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.analytics_analytics_titel),
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ═══════════════════════════════════════════
            // 🔥 STREAK OVERVIEW
            // ═══════════════════════════════════════════
            item {
                SectionHeader(emoji = "🔥", title = stringResource(R.string.analytics_analytics_streaks))
            }

            item {
                if (habitStreaks.isEmpty() && supplementStreaks.isEmpty()) {
                    EmptyState(stringResource(R.string.analytics_analytics_noch_keine_streaks_logge_habits))
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(habitStreaks) { info ->
                            StreakChip(
                                emoji = info.emoji,
                                name = info.name,
                                streak = info.streak,
                                color = accent
                            )
                        }
                        items(supplementStreaks) { info ->
                            StreakChip(
                                emoji = "💊",
                                name = info.name,
                                streak = info.streak,
                                color = Color(0xFF64D2FF)
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // 🍎 NUTRITION HEUTE - Donut chart
            // ═══════════════════════════════════════════
            item {
                SectionHeader(emoji = "🍎", title = stringResource(R.string.analytics_analytics_nutrition_heute))
            }

            item {
                LMCard {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NutritionDonutChart(
                            protein = todayNutrition.protein,
                            carbs = todayNutrition.carbs,
                            fat = todayNutrition.fat,
                            kcal = todayNutrition.kcal.toInt(),
                            accent = accent
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MacroLegend(stringResource(R.string.analytics_analytics_protein), todayNutrition.protein, Color(0xFF30D158))
                            MacroLegend(stringResource(R.string.analytics_analytics_carbs), todayNutrition.carbs, Color(0xFFFF9F0A))
                            MacroLegend(stringResource(R.string.analytics_analytics_fett), todayNutrition.fat, Color(0xFFFF453A))
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // ✅ HABITS - weekly completion grid
            // ═══════════════════════════════════════════
            if (habitWeekGrid.isNotEmpty()) {
                item {
                    SectionHeader(emoji = "✅", title = stringResource(R.string.analytics_analytics_habits))
                }

                item {
                    LMCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.analytics_analytics_letzte_7_tage),
                                style = MaterialTheme.typography.labelMedium,
                                color = Secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            habitWeekGrid.forEach { habit ->
                                HabitCompletionRow(
                                    emoji = habit.emoji,
                                    name = habit.name,
                                    days = habit.days,
                                    accent = accent
                                )
                                if (habit != habitWeekGrid.last()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // 🏋️ GYM FORTSCHRITT - styled line chart
            // ═══════════════════════════════════════════
            item {
                SectionHeader(emoji = "🏋️", title = stringResource(R.string.analytics_analytics_gym_fortschritt))
            }

            item {
                LMCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.analytics_analytics_workouts_gesamt, totalWorkouts),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (recentWorkouts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.analytics_analytics_letztes_workout, recentWorkouts.first().session.name, recentWorkouts.first().session.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                        }

                        if (exerciseNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.analytics_analytics_uebung_auswaehlen),
                                style = MaterialTheme.typography.labelMedium,
                                color = Secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(exerciseNames) { name ->
                                    FilterChip(
                                        selected = name == selectedExercise,
                                        onClick = { viewModel.selectExercise(name) },
                                        label = {
                                            Text(
                                                name,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accent.copy(alpha = 0.2f),
                                            selectedLabelColor = accent
                                        )
                                    )
                                }
                            }

                            if (exerciseProgress.isNotEmpty() && selectedExercise != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.analytics_analytics_gewicht_kg, selectedExercise!!),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StyledLineChart(
                                    values = exerciseProgress.map { it.maxWeight.toFloat() },
                                    accentColor = accent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.analytics_analytics_noch_keine_uebungen_geloggt), color = Secondary)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // 🧠 MOOD TREND - styled + 7/30 day toggle
            // ═══════════════════════════════════════════
            item {
                SectionHeader(emoji = "🧠", title = stringResource(R.string.analytics_analytics_mood_trend))
            }

            item {
                LMCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Range selector chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = moodRange == 7,
                                onClick = { viewModel.setMoodRange(7) },
                                label = { Text(stringResource(R.string.analytics_analytics_7_tage)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.2f),
                                    selectedLabelColor = accent
                                )
                            )
                            FilterChip(
                                selected = moodRange == 30,
                                onClick = { viewModel.setMoodRange(30) },
                                label = { Text(stringResource(R.string.analytics_analytics_30_tage)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.2f),
                                    selectedLabelColor = accent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (todayMood != null) {
                            val mood = todayMood!!
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = moodEmoji(mood.moodLevel),
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.analytics_analytics_heute_mood, mood.moodLevel),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                    Text(
                                        text = moodLabel(mood.moodLevel),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Secondary
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.analytics_analytics_heute_noch_kein_moodeintrag), color = Secondary)
                        }

                        if (recentMoods.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val avg = recentMoods.map { it.moodLevel }.average()
                            Text(
                                text = stringResource(R.string.analytics_analytics_avg_format, String.format("%.1f", avg), trendArrow(recentMoods)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            StyledColumnChart(
                                values = recentMoods.reversed().map { it.moodLevel.toFloat() },
                                accentColor = accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // ⚖️ WEIGHT TREND
            // ═══════════════════════════════════════════
            item {
                SectionHeader(emoji = "⚖️", title = stringResource(R.string.analytics_analytics_gewicht_verlauf))
            }

            item {
                LMCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (weightTrend.isEmpty()) {
                            Text(
                                text = stringResource(R.string.analytics_analytics_noch_keine_gewichtseintraege),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Secondary
                            )
                        } else {
                            val latest = weightTrend.last()
                            Text(
                                text = stringResource(R.string.analytics_analytics_aktuell_kg, String.format("%.1f", latest.kg)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                            if (weightTrend.size >= 2) {
                                val diff = latest.kg - weightTrend.first().kg
                                val sign = if (diff >= 0) "+" else ""
                                Text(
                                    text = stringResource(R.string.analytics_analytics_gewicht_diff, "$sign${String.format("%.1f", diff)}"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Range selector chips ──
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    7 to stringResource(R.string.analytics_analytics_woche),
                                    30 to stringResource(R.string.analytics_analytics_monat),
                                    365 to stringResource(R.string.analytics_analytics_jahr)
                                ).forEach { (days, label) ->
                                    FilterChip(
                                        selected = weightRange == days,
                                        onClick = { viewModel.setWeightRange(days) },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accent.copy(alpha = 0.15f),
                                            selectedLabelColor = accent
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Weight chart with forecast ──
                            WeightTrendChart(
                                actual = weightTrend.map { it.kg.toFloat() },
                                forecast = weightForecast.map { it.kg.toFloat() },
                                accentColor = accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // 🕐 WORK TIME - weekly bar chart
            // ═══════════════════════════════════════════
            if (workWeekHours.isNotEmpty() && workWeekHours.any { it.hours > 0 }) {
                item {
                    SectionHeader(emoji = "🕐", title = stringResource(R.string.analytics_analytics_arbeitszeit_woche))
                }

                item {
                    LMCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val totalHours = workWeekHours.sumOf { it.hours }
                            Text(
                                text = stringResource(R.string.analytics_analytics_stunden_diese_woche, String.format("%.1f", totalHours)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            WorkWeekBarChart(
                                days = workWeekHours,
                                accent = accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // 🚶 AKTIVITÄT
            // ═══════════════════════════════════════════
            item {
                SectionHeader(emoji = "🚶", title = stringResource(R.string.analytics_analytics_aktivitaet_heute))
            }

            item {
                LMCard {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "%,d".format(todaySteps),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = stringResource(R.string.analytics_analytics_schritte),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.analytics_analytics_kcal_verbrannt_format, todayCalories),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9F0A)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════
            // 📊 GESAMTAKTIVITÄT
            // ═══════════════════════════════════════════
            item {
                SectionHeader(emoji = "📊", title = stringResource(R.string.analytics_analytics_gesamtaktivitaet))
            }

            item {
                LMCard {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$totalActions",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Text(
                            text = stringResource(R.string.analytics_analytics_aktionen_geloggt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Secondary
                        )
                    }
                }
            }

            // ── Upcoming Events ──
            if (upcomingEvents.isNotEmpty()) {
                item {
                    SectionHeader(emoji = "📅", title = stringResource(R.string.analytics_analytics_anstehend))
                }

                item {
                    LMCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            upcomingEvents.take(3).forEach { event ->
                                Text(
                                    text = "- ${event.date}: ${event.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// COMPOSABLE HELPERS
// ══════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(emoji: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    LMCard {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            color = Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StreakChip(emoji: String, name: String, streak: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.analytics_analytics_streak_chip_format, streak),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

// ── Nutrition Donut Chart ──

@Composable
private fun NutritionDonutChart(
    protein: Double,
    carbs: Double,
    fat: Double,
    kcal: Int,
    accent: Color
) {
    val proteinColor = Color(0xFF30D158)
    val carbsColor = Color(0xFFFF9F0A)
    val fatColor = Color(0xFFFF453A)
    val bgColor = Surface

    val total = (protein + carbs + fat).coerceAtLeast(1.0)
    val proteinAngle = (protein / total * 360f).toFloat()
    val carbsAngle = (carbs / total * 360f).toFloat()
    val fatAngle = (fat / total * 360f).toFloat()

    val chartDesc = "Nutrition: ${String.format("%.0f", protein)}g protein, ${String.format("%.0f", carbs)}g carbs, ${String.format("%.0f", fat)}g fat, $kcal kcal"

    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.semantics { contentDescription = chartDesc }
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val topLeft = Offset(
                (size.width - 2 * radius) / 2f,
                (size.height - 2 * radius) / 2f
            )
            val arcSize = Size(radius * 2, radius * 2)
            val bgStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val fgStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Background ring
            drawArc(
                color = bgColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = bgStroke
            )

            if (total > 1.0) {
                // Protein arc
                var startAngle = -90f
                drawArc(
                    color = proteinColor,
                    startAngle = startAngle,
                    sweepAngle = proteinAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = fgStroke
                )
                startAngle += proteinAngle

                // Carbs arc
                drawArc(
                    color = carbsColor,
                    startAngle = startAngle,
                    sweepAngle = carbsAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = fgStroke
                )
                startAngle += carbsAngle

                // Fat arc
                drawArc(
                    color = fatColor,
                    startAngle = startAngle,
                    sweepAngle = fatAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = fgStroke
                )
            }
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$kcal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = "kcal",
                style = MaterialTheme.typography.labelSmall,
                color = Secondary
            )
        }
    }
}

@Composable
private fun MacroLegend(label: String, grams: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = stringResource(R.string.analytics_analytics_gramm_format, grams.toInt()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Secondary
            )
        }
    }
}

// ── Habit Completion Row (7-day dot grid) ──

@Composable
private fun HabitCompletionRow(
    emoji: String,
    name: String,
    days: List<Boolean>,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { completed ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (completed) accent else Surface
                        )
                )
            }
        }
    }
}

// ── Weight Trend Chart (solid actual + dotted forecast) ──

@Composable
private fun WeightTrendChart(
    actual: List<Float>,
    forecast: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (actual.isEmpty()) return

    val allValues = actual + forecast
    val minVal = allValues.min() - 1f
    val maxVal = allValues.max() + 1f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val chartDesc = buildString {
        append("Weight trend chart. ")
        append("Current: ${String.format("%.1f", actual.last())} kg. ")
        if (actual.size >= 2) {
            val change = actual.last() - actual.first()
            append("Change: ${if (change >= 0) "+" else ""}${String.format("%.1f", change)} kg over ${actual.size} entries. ")
        }
        if (forecast.size >= 2) {
            append("Forecast: ${String.format("%.1f", forecast.last())} kg.")
        }
    }

    val accentArgb = accentColor.toArgb()
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = 0xFF888888.toInt()
            textSize = 28f
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier.semantics { contentDescription = chartDesc }) {
        val leftPad = 48.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartW = size.width - leftPad
        val chartH = size.height - bottomPad
        val totalPoints = actual.size + forecast.size - 1 // forecast[0] == actual[last]

        fun xFor(index: Int) = leftPad + (index.toFloat() / (totalPoints - 1).coerceAtLeast(1)) * chartW
        fun yFor(value: Float) = chartH * (1f - (value - minVal) / range)

        // Y-axis labels (3 ticks)
        for (i in 0..2) {
            val v = minVal + range * i / 2f
            val y = yFor(v)
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.0f", v),
                4.dp.toPx(),
                y + 10f,
                labelPaint
            )
            // grid line
            drawLine(
                color = Color(0xFF333333),
                start = Offset(leftPad, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Solid line: actual data
        val actualLinePaint = accentColor
        for (i in 1 until actual.size) {
            drawLine(
                color = actualLinePaint,
                start = Offset(xFor(i - 1), yFor(actual[i - 1])),
                end = Offset(xFor(i), yFor(actual[i])),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Dots on actual data
        actual.forEachIndexed { i, v ->
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = Offset(xFor(i), yFor(v))
            )
        }

        // Dotted line: forecast
        if (forecast.size >= 2) {
            val forecastStartIdx = actual.size - 1
            val pathEffect = android.graphics.DashPathEffect(
                floatArrayOf(12f, 8f), 0f
            )
            val forecastPaint = androidx.compose.ui.graphics.Paint().apply {
                color = accentColor.copy(alpha = 0.5f)
                strokeWidth = 2.dp.toPx()
                style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                strokeCap = StrokeCap.Round
                asFrameworkPaint().pathEffect = pathEffect
            }
            val path = android.graphics.Path()
            forecast.forEachIndexed { i, v ->
                val x = xFor(forecastStartIdx + i)
                val y = yFor(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawContext.canvas.nativeCanvas.drawPath(path, forecastPaint.asFrameworkPaint())
        }
    }
}

// ── Styled Vico Line Chart (accent-colored) ──

@Composable
private fun StyledLineChart(
    values: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val model = remember(values) {
        entryModelOf(*values.toTypedArray())
    }

    Chart(
        chart = lineChart(
            lines = listOf(
                LineChart.LineSpec(
                    lineColor = accentColor.toArgb(),
                    lineThicknessDp = 2.5f
                )
            )
        ),
        model = model,
        modifier = modifier,
    )
}

// ── Styled Vico Column Chart (accent-colored) ──

@Composable
private fun StyledColumnChart(
    values: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val model = remember(values) {
        entryModelOf(*values.toTypedArray())
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                lineComponent(
                    color = accentColor,
                    thickness = 8.dp,
                    shape = Shapes.roundedCornerShape(allPercent = 40)
                )
            )
        ),
        model = model,
        modifier = modifier,
    )
}

// ── Work Week Bar Chart (Canvas-drawn) ──

@Composable
private fun WorkWeekBarChart(
    days: List<WorkDayHours>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val maxHours = days.maxOfOrNull { it.hours }?.coerceAtLeast(1.0) ?: 1.0
    val surfaceColor = Surface
    val secondaryColor = Secondary
    val totalHoursDesc = days.sumOf { it.hours }
    val chartDesc = "Work week chart. Total: ${String.format("%.1f", totalHoursDesc)} hours across ${days.size} days."
    val labelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    labelPaint.color = secondaryColor.toArgb()

    Canvas(modifier = modifier.semantics { contentDescription = chartDesc }) {
        labelPaint.textSize = 10.sp.toPx()

        val barCount = days.size
        val spacing = 12.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(0f)
        val chartHeight = size.height - 24.dp.toPx() // leave room for labels

        days.forEachIndexed { index, day ->
            val x = index * (barWidth + spacing)
            val barHeight = ((day.hours / maxHours) * chartHeight).toFloat().coerceAtLeast(0f)
            val y = chartHeight - barHeight

            // Background bar
            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, chartHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )

            // Filled bar
            if (barHeight > 0f) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }

            // Day label
            drawContext.canvas.nativeCanvas.drawText(
                day.dayLabel,
                x + barWidth / 2f,
                size.height,
                labelPaint
            )
        }
    }
}

// ── Helpers ──

private fun moodEmoji(level: Int): String = when (level) {
    in 1..2 -> "😞"
    in 3..4 -> "😕"
    5 -> "😐"
    in 6..7 -> "🙂"
    in 8..9 -> "😊"
    10 -> "🤩"
    else -> "😐"
}

@Composable
private fun moodLabel(level: Int): String = when (level) {
    in 1..2 -> stringResource(R.string.analytics_analytics_mood_sehr_schlecht)
    in 3..4 -> stringResource(R.string.analytics_analytics_mood_nicht_gut)
    5 -> stringResource(R.string.analytics_analytics_mood_neutral)
    in 6..7 -> stringResource(R.string.analytics_analytics_mood_gut)
    in 8..9 -> stringResource(R.string.analytics_analytics_mood_sehr_gut)
    10 -> stringResource(R.string.analytics_analytics_mood_fantastisch)
    else -> stringResource(R.string.analytics_analytics_mood_neutral)
}

@Composable
private fun trendArrow(moods: List<de.lifemodule.app.data.mentalhealth.MoodEntryEntity>): String {
    if (moods.size < 2) return "->"
    val firstHalf = moods.takeLast(moods.size / 2).map { it.moodLevel }.average()
    val secondHalf = moods.take(moods.size / 2).map { it.moodLevel }.average()
    return when {
        firstHalf - secondHalf > 0.5 -> stringResource(R.string.analytics_analytics_trend_aufwaerts)
        secondHalf - firstHalf > 0.5 -> stringResource(R.string.analytics_analytics_trend_abwaerts)
        else -> stringResource(R.string.analytics_analytics_trend_stabil)
    }
}
