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
import androidx.compose.ui.text.input.KeyboardType
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
import de.lifemodule.app.feature.nutrition.R
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddSupplementScreen(
    navController: NavController,
    viewModel: SupplementViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("daily") }
    var timesPerDay by remember { mutableFloatStateOf(1f) }
    var timeOfDay by remember { mutableStateOf("morning") }
    var durationInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val isValid = name.isNotBlank() && dosage.isNotBlank()

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(context.filesDir, "supp_img_$timestamp.jpg")
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
                title = stringResource(R.string.nutrition_supplements_neues_supplement),
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
                    label = stringResource(R.string.nutrition_supplements_name_pflicht),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                LMInput(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = stringResource(R.string.nutrition_supplements_dosierung_zb_1000mg_2_kapseln),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Frequency
            item {
                Text(stringResource(R.string.nutrition_supplements_haeufigkeit), style = MaterialTheme.typography.labelLarge, color = Secondary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LMSelectChip(stringResource(R.string.nutrition_supplements_taeglich), "daily", frequency) { frequency = it }
                    LMSelectChip(stringResource(R.string.nutrition_supplements_woechentlich), "weekly", frequency) { frequency = it }
                }
            }

            // Times per day slider
            item {
                val timesLabel = when (timesPerDay.toInt()) {
                    1 -> stringResource(R.string.nutrition_supplements_1_taeglich)
                    2 -> stringResource(R.string.nutrition_supplements_2_taeglich)
                    3 -> stringResource(R.string.nutrition_supplements_3_taeglich)
                    else -> stringResource(R.string.nutrition_supplements_x_taeglich, timesPerDay.toInt())
                }
                Text(
                    text = "💊 $timesLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = accent
                )
                Slider(
                    value = timesPerDay,
                    onValueChange = { timesPerDay = it },
                    valueRange = 1f..4f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent
                    )
                )
            }

            // Time of day
            item {
                Text(stringResource(R.string.nutrition_supplements_tageszeit), style = MaterialTheme.typography.labelLarge, color = Secondary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LMSelectChip(stringResource(R.string.nutrition_supplements_morgens), "morning", timeOfDay) { timeOfDay = it }
                    LMSelectChip(stringResource(R.string.nutrition_supplements_mittags), "midday", timeOfDay) { timeOfDay = it }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LMSelectChip(stringResource(R.string.nutrition_supplements_abends), "evening", timeOfDay) { timeOfDay = it }
                    LMSelectChip(stringResource(R.string.nutrition_supplements_jederzeit), "any", timeOfDay) { timeOfDay = it }
                }
            }

            // Duration limit (for medications)
            item {
                LMInput(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = stringResource(R.string.nutrition_supplements_dauer_tage),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
                if (durationInput.isNotBlank()) {
                    val days = durationInput.toIntOrNull()
                    if (days != null) {
                        Text(
                            text = stringResource(R.string.nutrition_supplements_auto_deaktiviert, days),
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                LMInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.nutrition_supplements_notizen_optional),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Photo section
            item {
                Text(stringResource(R.string.nutrition_supplements_foto_optional), style = MaterialTheme.typography.labelLarge, color = Secondary,
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
                        Text(stringResource(R.string.nutrition_supplements_galerie))
                    }
                    OutlinedButton(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val file = File(context.filesDir, "supp_img_$timestamp.jpg")
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
                        Text(stringResource(R.string.nutrition_supplements_kamera))
                    }
                }
            }
            if (imageUri != null) {
                item {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = stringResource(R.string.nutrition_supplements_supplement_foto),
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
                        viewModel.addSupplement(
                            name = name,
                            dosage = dosage,
                            frequency = frequency,
                            timesPerDay = timesPerDay.toInt(),
                            timeOfDay = timeOfDay,
                            durationDays = durationInput.toIntOrNull(),
                            notes = notes,
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
                    Text(stringResource(R.string.nutrition_supplements_speichern), style = MaterialTheme.typography.labelLarge)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun LMSelectChip(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
