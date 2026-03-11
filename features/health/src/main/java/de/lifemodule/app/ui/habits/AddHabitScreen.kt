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

package de.lifemodule.app.ui.habits

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.lifemodule.app.feature.health.R
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddHabitScreen(
    navController: NavController,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✅") }
    var timeOfDay by remember { mutableStateOf("any") }
    var isPositive by remember { mutableStateOf(true) }
    var intervalDays by remember { mutableFloatStateOf(1f) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val isValid = name.isNotBlank()

    val emojiOptions = listOf(
        "✅", "🚿", "🪥", "📖", "🏋️", "🧘", "💧", "🍎",
        "💤", "📱", "🚫", "🎯", "🧹", "💊", "🏃", "🧠"
    )

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(context.filesDir, "habit_img_$timestamp.jpg")
            inputStream?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            imageUri = Uri.fromFile(file)
        }
    }

    // Camera picker
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            imageUri = tempPhotoUri
        }
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.health_habits_neuer_habit),
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

            item {
                LMInput(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.health_habits_name_zb_zaehne_putzen_duschen),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Emoji picker
            item {
                Text(stringResource(R.string.health_habits_emoji_label), style = MaterialTheme.typography.labelLarge, color = Secondary)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    emojiOptions.take(8).forEach { e ->
                        FilterChip(
                            selected = emoji == e,
                            onClick = { emoji = e },
                            label = { Text(e) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    emojiOptions.drop(8).forEach { e ->
                        FilterChip(
                            selected = emoji == e,
                            onClick = { emoji = e },
                            label = { Text(e) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Type: Positive vs Avoid
            item {
                Text(stringResource(R.string.health_habits_art), style = MaterialTheme.typography.labelLarge, color = Secondary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isPositive,
                        onClick = { isPositive = true },
                        label = { Text(stringResource(R.string.health_habits_machen)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = !isPositive,
                        onClick = { isPositive = false },
                        label = { Text(stringResource(R.string.health_habits_vermeiden)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Time of day
            item {
                Text(stringResource(R.string.health_habits_tageszeit), style = MaterialTheme.typography.labelLarge, color = Secondary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        stringResource(R.string.health_habits_morgens) to "morning",
                        stringResource(R.string.health_habits_abends) to "evening",
                        stringResource(R.string.health_habits_jederzeit) to "any"
                    ).forEach { (label, value) ->
                        FilterChip(
                            selected = timeOfDay == value,
                            onClick = { timeOfDay = value },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Repeat interval slider
            item {
                Text(stringResource(R.string.health_habits_wiederholung), style = MaterialTheme.typography.labelLarge, color = Secondary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                val intervalLabel = when (intervalDays.toInt()) {
                    1 -> stringResource(R.string.health_habits_jeden_tag)
                    2 -> stringResource(R.string.health_habits_alle_2_tage)
                    3 -> stringResource(R.string.health_habits_alle_3_tage)
                    7 -> stringResource(R.string.health_habits_jede_woche)
                    14 -> stringResource(R.string.health_habits_alle_2_wochen)
                    else -> stringResource(R.string.health_habits_alle_x_tage, intervalDays.toInt())
                }
                Text(
                    text = "🔄 $intervalLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = accent
                )
                Slider(
                    value = intervalDays,
                    onValueChange = { intervalDays = it },
                    valueRange = 1f..14f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent
                    )
                )
            }

            // Photo section
            item {
                Text(stringResource(R.string.health_habits_foto_optional), style = MaterialTheme.typography.labelLarge, color = Secondary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.health_habits_galerie))
                    }
                    OutlinedButton(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val file = File(context.filesDir, "habit_img_$timestamp.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                tempPhotoUri = Uri.fromFile(file)
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.health_habits_kamera))
                    }
                }
            }
            if (imageUri != null) {
                item {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = stringResource(R.string.health_habits_habit_foto),
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Save
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addHabit(
                            name = name,
                            emoji = emoji,
                            frequency = if (intervalDays.toInt() == 1) "daily" else "custom",
                            repeatIntervalDays = intervalDays.toInt(),
                            timeOfDay = timeOfDay,
                            isPositive = isPositive,
                            imagePath = imageUri?.toString()
                        )
                        navController.popBackStack()
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.health_habits_speichern), style = MaterialTheme.typography.labelLarge)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
