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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.lifemodule.app.data.scanner.ReceiptCategory
import de.lifemodule.app.data.scanner.ReceiptRecordEntity
import de.lifemodule.app.feature.scanner.R
import de.lifemodule.app.ui.components.LMInput
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Border
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: String,
    navController: NavController,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val scope = rememberCoroutineScope()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val receipt = records.firstOrNull { it.uuid == receiptId }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

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
                title = stringResource(R.string.scanner_detail_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = Black
    ) { padding ->
        if (receipt == null) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.scanner_detail_not_found), color = Secondary, fontSize = 14.sp)
            }
        } else {
            // Editable state (only if not finalized)
            var vendor by remember(receipt.uuid) { mutableStateOf(receipt.vendor) }
            var totalAmount by remember(receipt.uuid) { mutableStateOf("%.2f".format(receipt.totalAmount)) }
            var vatAmount by remember(receipt.uuid) { mutableStateOf(receipt.vatAmount?.let { "%.2f".format(it) } ?: "") }
            var currency by remember(receipt.uuid) { mutableStateOf(receipt.currency) }
            var notes by remember(receipt.uuid) { mutableStateOf(receipt.notes ?: "") }
            var selectedCategory by remember(receipt.uuid) { mutableStateOf(receipt.category) }
            var categoryExpanded by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.scanner_detail_captured_at, dateFormat.format(Date(receipt.capturedAt))), color = Secondary, fontSize = 12.sp)
                        if (receipt.isFinalized) {
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.scanner_detail_finalized), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (receipt.isFinalized) {
                    // Read-only view for finalized receipts
                    DetailRow(stringResource(R.string.scanner_detail_vendor), receipt.vendor)
                    DetailRow(stringResource(R.string.scanner_detail_amount), "%.2f %s".format(receipt.totalAmount, receipt.currency))
                    receipt.vatAmount?.let { DetailRow("MwSt", "%.2f".format(it)) }
                    DetailRow(stringResource(R.string.scanner_detail_category), categoryLabels[receipt.category] ?: receipt.category.name)
                    receipt.notes?.let { DetailRow(stringResource(R.string.scanner_detail_notes), it) }
                } else {
                    // Editable view for unfinalized receipts
                    LMInput(
                        value = vendor,
                        onValueChange = { vendor = it },
                        label = stringResource(R.string.scanner_detail_vendor),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LMInput(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = stringResource(R.string.scanner_detail_amount),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Decimal
                    )

                    LMInput(
                        value = vatAmount,
                        onValueChange = { vatAmount = it },
                        label = stringResource(R.string.scanner_detail_vat_optional),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Decimal
                    )

                    LMInput(
                        value = currency,
                        onValueChange = { currency = it },
                        label = stringResource(R.string.scanner_detail_currency),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = categoryLabels[selectedCategory] ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.scanner_detail_category)) },
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
                        label = stringResource(R.string.scanner_detail_notes_optional),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )

                    // Save edits
                    Button(
                        onClick = {
                            val amt = totalAmount.replace(",", ".").toDoubleOrNull()
                            val vat = vatAmount.replace(",", ".").toDoubleOrNull()
                            if (amt != null && vendor.isNotBlank()) {
                                viewModel.updateReceipt(
                                    receipt.copy(
                                        vendor = vendor.trim(),
                                        totalAmount = amt,
                                        vatAmount = vat,
                                        currency = currency.trim().ifBlank { "EUR" },
                                        category = selectedCategory,
                                        notes = notes.takeIf { it.isNotBlank() }
                                    )
                                )
                            } else {
                                Timber.w("[Scanner] Detail edit validation failed: vendor=%s, amount=%s", vendor, totalAmount)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = vendor.isNotBlank()
                                && totalAmount.replace(",", ".").toDoubleOrNull() != null,
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text(stringResource(R.string.scanner_detail_save_changes))
                    }

                    // Finalize
                    OutlinedButton(
                        onClick = { viewModel.finalizeReceipt(receipt.uuid) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                    ) {
                        Text(stringResource(R.string.scanner_detail_finalize))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Secondary, fontSize = 12.sp)
        Text(value, color = androidx.compose.ui.graphics.Color.White, fontSize = 15.sp)
    }
}
