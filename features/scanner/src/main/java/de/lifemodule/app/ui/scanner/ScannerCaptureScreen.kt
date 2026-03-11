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

package de.lifemodule.app.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.data.scanner.ReceiptCategory
import de.lifemodule.app.feature.scanner.R
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Border
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerCaptureScreen(
    navController: NavController,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    var capturedFile by remember { mutableStateOf<File?>(null) }
    var capturedSha256 by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }

    // Form state (pre-filled by OCR)
    var vendor by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var vatAmount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("EUR") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReceiptCategory.OTHER) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess == true) {
            viewModel.clearSaveState()
            navController.popBackStack()
        }
    }

    val categoryLabels = mapOf(
        ReceiptCategory.GROCERIES to stringResource(R.string.scanner_cat_groceries),
        ReceiptCategory.RESTAURANT to stringResource(R.string.scanner_cat_restaurant),
        ReceiptCategory.TRANSPORT to stringResource(R.string.scanner_cat_transport),
        ReceiptCategory.HEALTH to stringResource(R.string.scanner_cat_health),
        ReceiptCategory.OFFICE to stringResource(R.string.scanner_cat_office),
        ReceiptCategory.ENTERTAINMENT to stringResource(R.string.scanner_cat_entertainment),
        ReceiptCategory.CLOTHING to stringResource(R.string.scanner_cat_clothing),
        ReceiptCategory.ELECTRONICS to stringResource(R.string.scanner_cat_electronics),
        ReceiptCategory.SUBSCRIPTION to stringResource(R.string.scanner_cat_subscription),
        ReceiptCategory.INSURANCE to stringResource(R.string.scanner_cat_insurance),
        ReceiptCategory.TAX to stringResource(R.string.scanner_cat_tax),
        ReceiptCategory.OTHER to stringResource(R.string.scanner_cat_other)
    )

    Scaffold(
        topBar = {
            LMTopBar(
                title = if (showForm) stringResource(R.string.scanner_capture_title) else stringResource(R.string.scanner_capture_take_photo),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        if (!hasCameraPermission) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.scanner_camera_permission_required), color = Secondary, fontSize = 14.sp)
            }
        } else if (!showForm) {
            // Camera phase
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                CameraCaptureView(
                    onImageCaptured = { file ->
                        capturedFile = file
                        isProcessing = true
                        // Compute SHA-256
                        val sha = try {
                            val digest = MessageDigest.getInstance("SHA-256")
                            file.inputStream().use { input ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    digest.update(buffer, 0, bytesRead)
                                }
                            }
                            digest.digest().joinToString("") { "%02x".format(it) }
                        } catch (e: Exception) {
                            Timber.e(e, "[Scanner] SHA-256 computation failed")
                            ""
                        }
                        capturedSha256 = sha

                        // Run OCR
                        scope.launch {
                            val result = ReceiptOcrEngine.recognizeReceipt(context, file)
                            if (result != null) {
                                vendor = result.vendor ?: ""
                                totalAmount = result.totalAmount?.let { "%.2f".format(it) } ?: ""
                                vatAmount = result.vatAmount?.let { "%.2f".format(it) } ?: ""
                            } else {
                                Timber.w("[Scanner] OCR returned null for %s", file.name)
                            }
                            isProcessing = false
                            showForm = true
                        }
                    },
                    onError = { msg ->
                        cameraError = msg
                        Timber.e("[Scanner] Camera error: %s", msg)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }

                cameraError?.let { err ->
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }
        } else {
            // Form phase - edit OCR results and save
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
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = stringResource(R.string.scanner_capture_vendor),
                    modifier = Modifier.fillMaxWidth()
                )

                LMInput(
                    value = totalAmount,
                    onValueChange = { totalAmount = it },
                    label = stringResource(R.string.scanner_capture_amount),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Decimal
                )

                LMInput(
                    value = vatAmount,
                    onValueChange = { vatAmount = it },
                    label = stringResource(R.string.scanner_capture_vat_optional),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Decimal
                )

                LMInput(
                    value = currency,
                    onValueChange = { currency = it },
                    label = stringResource(R.string.scanner_capture_currency),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categoryLabels[selectedCategory] ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.scanner_capture_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = Border,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = Secondary,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ReceiptCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(categoryLabels[cat] ?: cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                LMInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.scanner_capture_notes_optional),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )

                Button(
                    onClick = {
                        val amount = totalAmount.replace(",", ".").toDoubleOrNull()
                        val vat = vatAmount.replace(",", ".").toDoubleOrNull()
                        val file = capturedFile
                        if (amount != null && amount > 0 && vendor.isNotBlank() && file != null) {
                            viewModel.createReceipt(
                                vendor = vendor.trim(),
                                totalAmount = amount,
                                vatAmount = vat,
                                currency = currency.trim().ifBlank { "EUR" },
                                category = selectedCategory,
                                notes = notes.takeIf { it.isNotBlank() },
                                imagePath = file.absolutePath,
                                imageSha256 = capturedSha256
                            )
                        } else {
                            Timber.w("[Scanner] Capture validation failed: vendor=%s, amount=%s, file=%s",
                                vendor, totalAmount, file?.name)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = vendor.isNotBlank()
                            && totalAmount.replace(",", ".").toDoubleOrNull() != null
                            && capturedFile != null,
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(stringResource(R.string.scanner_capture_save))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
