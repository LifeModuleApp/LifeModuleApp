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

package de.lifemodule.app.ui.mentalhealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.feature.health.R
import androidx.compose.ui.res.stringResource

@Composable
fun AddMoodScreen(
    navController: NavController,
    viewModel: MoodViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    var moodLevel by remember { mutableFloatStateOf(5f) }
    var energyLevel by remember { mutableFloatStateOf(5f) }
    var stressLevel by remember { mutableFloatStateOf(5f) }
    var sleepQuality by remember { mutableFloatStateOf(5f) }
    var positiveNotes by remember { mutableStateOf("") }
    var negativeNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.health_mentalhealth_mood_checkin),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Mood Slider ──
            MoodSliderCard(
                emoji = moodEmoji(moodLevel.toInt()),
                label = stringResource(R.string.health_mentalhealth_wie_fuehlst_du_dich),
                value = moodLevel,
                onValueChange = { moodLevel = it },
                dynamicText = moodDynamicQuestion(moodLevel.toInt())
            )

            // ── Energy Slider ──
            MoodSliderCard(
                emoji = "⚡",
                label = stringResource(R.string.health_mentalhealth_energielevel),
                value = energyLevel,
                onValueChange = { energyLevel = it },
                dynamicText = energyDynamicText(energyLevel.toInt())
            )

            // ── Stress Slider (inverted: 1=relaxed/green, 10=stressed/red) ──
            MoodSliderCard(
                emoji = "😤",
                label = stringResource(R.string.health_mentalhealth_stresslevel),
                value = stressLevel,
                onValueChange = { stressLevel = it },
                dynamicText = stressDynamicText(stressLevel.toInt()),
                invertColors = true
            )

            // ── Sleep Quality ──
            MoodSliderCard(
                emoji = "😴",
                label = stringResource(R.string.health_mentalhealth_schlafqualitaet),
                value = sleepQuality,
                onValueChange = { sleepQuality = it },
                dynamicText = sleepDynamicText(sleepQuality.toInt())
            )

            // ── Was war gut heute? ──
            LMCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.health_mentalhealth_was_war_gut_heute),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LMInput(
                        value = positiveNotes,
                        onValueChange = { positiveNotes = it },
                        label = stringResource(R.string.health_mentalhealth_das_hat_mich_gefreut),
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }

            // ── Was war schlecht heute? ──
            LMCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.health_mentalhealth_was_war_schlecht_heute),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LMInput(
                        value = negativeNotes,
                        onValueChange = { negativeNotes = it },
                        label = stringResource(R.string.health_mentalhealth_das_war_nicht_so_gut),
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save Button ──
            Button(
                onClick = {
                    viewModel.saveMoodEntry(
                        moodLevel = moodLevel.toInt(),
                        energyLevel = energyLevel.toInt(),
                        stressLevel = stressLevel.toInt(),
                        sleepQuality = sleepQuality.toInt(),
                        positiveNotes = positiveNotes,
                        negativeNotes = negativeNotes
                    )
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.health_mentalhealth_speichern), color = Black)
            }
        }
    }
}

/**
 * Interpolates between red -> orange -> yellow -> green based on slider value (1-10).
 * If [invertColors] is true, the direction is reversed (1=green, 10=red) - useful for stress.
 */
private fun sliderColor(value: Float, invertColors: Boolean = false): Color {
    val t = ((value - 1f) / 9f).coerceIn(0f, 1f)
    val effective = if (invertColors) 1f - t else t

    return when {
        effective <= 0.33f -> {
            // Red -> Orange
            val local = effective / 0.33f
            Color(
                red = 1f,
                green = 0.3f + 0.35f * local,  // 0.3 -> 0.65
                blue = 0.1f * (1f - local)
            )
        }
        effective <= 0.66f -> {
            // Orange -> Yellow
            val local = (effective - 0.33f) / 0.33f
            Color(
                red = 1f - 0.15f * local,       // 1.0 -> 0.85
                green = 0.65f + 0.2f * local,   // 0.65 -> 0.85
                blue = 0f
            )
        }
        else -> {
            // Yellow -> Green
            val local = (effective - 0.66f) / 0.34f
            Color(
                red = 0.85f - 0.55f * local,    // 0.85 -> 0.30
                green = 0.85f + 0.05f * local,  // 0.85 -> 0.90
                blue = 0.15f * local             // 0 -> 0.15
            )
        }
    }
}

@Composable
private fun MoodSliderCard(
    emoji: String,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    dynamicText: String,
    invertColors: Boolean = false
) {
    val color = sliderColor(value, invertColors)

    LMCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${value.toInt()}/10",
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = Secondary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = dynamicText,
                style = MaterialTheme.typography.bodySmall,
                color = Secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Dynamic texts that change based on slider value ──

private fun moodEmoji(level: Int): String = when (level) {
    1 -> "😭"
    2 -> "😢"
    3 -> "😞"
    4 -> "😕"
    5 -> "😐"
    6 -> "🙂"
    7 -> "😊"
    8 -> "😄"
    9 -> "🤩"
    10 -> "🔥"
    else -> "😐"
}

@Composable
private fun moodDynamicQuestion(level: Int): String = when (level) {
    1 -> stringResource(R.string.health_mentalhealth_mood_1_text)
    2 -> stringResource(R.string.health_mentalhealth_mood_2_text)
    3 -> stringResource(R.string.health_mentalhealth_etwas_down_was_koennte_dir)
    4 -> stringResource(R.string.health_mentalhealth_nicht_perfekt_aber_ok_goenn)
    5 -> stringResource(R.string.health_mentalhealth_mood_5_text)
    6 -> stringResource(R.string.health_mentalhealth_laeuft_ganz_gut_was_hat)
    7 -> stringResource(R.string.health_mentalhealth_mood_7_text)
    8 -> stringResource(R.string.health_mentalhealth_richtig_stark_heute_was_macht)
    9 -> stringResource(R.string.health_mentalhealth_wow_topform_geniess_den_moment)
    10 -> stringResource(R.string.health_mentalhealth_absolute_spitze_was_ein_tag)
    else -> ""
}

@Composable
private fun energyDynamicText(level: Int): String = when (level) {
    in 1..2 -> stringResource(R.string.health_mentalhealth_energy_low)
    in 3..4 -> stringResource(R.string.health_mentalhealth_wenig_energie_vielleicht_ein_kaffee)
    in 5..6 -> stringResource(R.string.health_mentalhealth_normales_level_reicht_fuer_den)
    in 7..8 -> stringResource(R.string.health_mentalhealth_gut_geladen_perfekt_fuer_ein)
    in 9..10 -> stringResource(R.string.health_mentalhealth_energy_high)
    else -> ""
}

@Composable
private fun stressDynamicText(level: Int): String = when (level) {
    in 1..2 -> stringResource(R.string.health_mentalhealth_stress_low)
    in 3..4 -> stringResource(R.string.health_mentalhealth_leichter_stress_alles_im_rahmen)
    in 5..6 -> stringResource(R.string.health_mentalhealth_merkbarer_stress_pausen_einplanen)
    in 7..8 -> stringResource(R.string.health_mentalhealth_hoher_stress_achte_auf_dich)
    in 9..10 -> stringResource(R.string.health_mentalhealth_sehr_hoher_stress_ueberlege_dir)
    else -> ""
}

@Composable
private fun sleepDynamicText(level: Int): String = when (level) {
    in 1..2 -> stringResource(R.string.health_mentalhealth_kaum_geschlafen_das_holt_sich)
    in 3..4 -> stringResource(R.string.health_mentalhealth_wenig_schlaf_heute_abend_frueher)
    in 5..6 -> stringResource(R.string.health_mentalhealth_ok_geschlafen_geht_besser_geht)
    in 7..8 -> stringResource(R.string.health_mentalhealth_gut_geschlafen_gute_basis_fuer)
    in 9..10 -> stringResource(R.string.health_mentalhealth_perfekt_erholt_wie_ein_baby)
    else -> ""
}
