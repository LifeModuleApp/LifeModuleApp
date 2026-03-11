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

package de.lifemodule.app.ui.settings

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.lifemodule.app.R
import de.lifemodule.app.data.packages.ImportResult
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.Secondary

/**
 * Full-screen import progress & result display.
 *
 * Receives a package [Uri] via navigation arguments. Immediately starts
 * the import and shows a progress indicator, then transitions to a result
 * summary with inserted/skipped/failed counts.
 */
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    packageUri: Uri?,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val accent = LocalAccentColor.current

    // Kick off import when the screen enters composition
    LaunchedEffect(packageUri) {
        if (packageUri != null && state is ImportState.Idle) {
            viewModel.importPackage(packageUri)
        }
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.app_import_title),
                onBackClick = {
                    viewModel.reset()
                    onBack()
                }
            )
        },
        containerColor = Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            // ── Loading state ────────────────────────────────────────────
            AnimatedVisibility(
                visible = state is ImportState.Importing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = accent,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = stringResource(R.string.app_import_importing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Result state ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = state is ImportState.Done,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val result = (state as? ImportState.Done)?.result
                if (result != null) {
                    ImportResultContent(result = result, onDone = {
                        viewModel.reset()
                        onBack()
                    })
                }
            }
        }
    }
}

@Composable
private fun ImportResultContent(
    result: ImportResult,
    onDone: () -> Unit
) {
    val accent = LocalAccentColor.current
    val isSuccess = result.isSuccess

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Status icon ──────────────────────────────────────────────
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isSuccess) accent else Destructive,
            modifier = Modifier.size(72.dp)
        )

        Text(
            text = stringResource(
                if (isSuccess) R.string.app_import_success_title
                else R.string.app_import_error_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // ── Stats card ───────────────────────────────────────────────
        if (result.totalProcessed > 0) {
            LMCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(
                        label = stringResource(R.string.app_import_inserted),
                        value = result.inserted,
                        color = accent
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Secondary.copy(alpha = 0.2f)
                    )
                    StatRow(
                        label = stringResource(R.string.app_import_skipped),
                        value = result.skipped,
                        color = Secondary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Secondary.copy(alpha = 0.2f)
                    )
                    StatRow(
                        label = stringResource(R.string.app_import_failed),
                        value = result.failed,
                        color = if (result.failed > 0) Destructive else Secondary
                    )
                }
            }
        }

        // ── Errors list ──────────────────────────────────────────────
        if (result.errors.isNotEmpty()) {
            LMCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.app_import_errors_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Destructive,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    result.errors.forEach { error ->
                        Text(
                            text = "- $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Done button ──────────────────────────────────────────────
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Black
            )
        ) {
            Text(
                text = stringResource(R.string.app_import_done),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatRow(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
