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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Receipt
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
import de.lifemodule.app.data.scanner.ReceiptRecordEntity
import androidx.compose.ui.res.stringResource
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Accent
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Surface
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.feature.scanner.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScannerScreen(
    navController: NavController,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.scanner_title),
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(AppRoute.ScannerCapture) },
                containerColor = Accent,
                contentColor = Black
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.scanner_scan_receipt))
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
            if (records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.scanner_empty),
                                color = Secondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(records, key = { it.uuid }) { record ->
                    ReceiptCard(record)
                }
            }
        }
    }
}

@Composable
private fun ReceiptCard(record: ReceiptRecordEntity) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val formattedDate = remember(record.receiptDate) {
        Instant.ofEpochMilli(record.receiptDate)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
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
                Text(
                    record.vendor,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (record.isFinalized) {
                    Text(
                        "🔒",
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formattedDate, color = Secondary, fontSize = 12.sp)
                Text(
                    record.category.name.lowercase()
                        .replaceFirstChar { it.titlecase() },
                    color = Secondary,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${"%.2f".format(record.totalAmount)} ${record.currency}",
                color = Accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            record.vatAmount?.let { vat ->
                Text(
                    "MwSt: ${"%.2f".format(vat)} ${record.currency}",
                    color = Secondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
