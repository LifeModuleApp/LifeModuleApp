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

package de.lifemodule.app.ui.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.lifemodule.app.R
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import java.util.Locale

/**
 * Displays the app's own open-source license, a link to the GitHub repo,
 * and all third-party libraries with their licenses.
 */
@Composable
fun LicenseScreen(navController: NavController) {
    val context = LocalContext.current
    val isGerman = Locale.getDefault().language == "de"
    val accent = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.app_license_title),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            // ── App license header ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "LifeModule",
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GNU General Public License v3.0 (GPL-3.0)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "© 2026 Paul Bernhard Colin Witzke",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LifeModuleApp/LifeModule"))
                            context.startActivity(intent)
                        }) {
                            Text(
                                text = if (isGerman) "GitHub-Repository öffnen" else "Open GitHub Repository",
                                color = accent,
                                fontSize = 14.sp
                            )
                        }
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))
                            context.startActivity(intent)
                        }) {
                            Text(
                                text = if (isGerman) "Vollständigen Lizenztext lesen (gnu.org)" else "Read full license text (gnu.org)",
                                color = accent,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // ── Section title ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isGerman) "Verwendete Open-Source-Bibliotheken" else "Third-Party Open Source Libraries",
                    style = MaterialTheme.typography.titleSmall,
                    color = Secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Library cards ──
            items(ossLibraries) { lib ->
                LicenseCard(lib)
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun LicenseCard(lib: OssLibrary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = lib.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = lib.artifact,
                style = MaterialTheme.typography.bodySmall,
                color = Secondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lib.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }
    }
}

private data class OssLibrary(
    val name: String,
    val artifact: String,
    val license: String
)

/* ── Static list of all third-party dependencies ────────────────────────── */
private val ossLibraries = listOf(
    OssLibrary(
        name = "Dagger Hilt",
        artifact = "com.google.dagger:hilt-android:2.51.1",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Hilt Navigation Compose",
        artifact = "androidx.hilt:hilt-navigation-compose:1.2.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Room (Runtime, KTX, Compiler)",
        artifact = "androidx.room:room-*:2.7.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Coil Compose",
        artifact = "io.coil-kt:coil-compose:2.6.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "CameraX",
        artifact = "androidx.camera:camera-*:1.3.4",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "ML Kit Barcode Scanning",
        artifact = "com.google.mlkit:barcode-scanning:17.3.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "ML Kit Text Recognition",
        artifact = "com.google.mlkit:text-recognition:16.0.1",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Accompanist Permissions",
        artifact = "com.google.accompanist:accompanist-permissions:0.36.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Vico Charts",
        artifact = "com.patrykandpatrick.vico:compose-m3:1.15.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "DataStore Preferences",
        artifact = "androidx.datastore:datastore-preferences:1.1.1",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Kotlinx Serialization JSON",
        artifact = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Health Connect Client",
        artifact = "androidx.health.connect:connect-client:1.1.0-alpha11",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "SQLCipher for Android",
        artifact = "net.zetetic:android-database-sqlcipher:4.5.4",
        license = "BSD 3-Clause License"
    ),
    OssLibrary(
        name = "AndroidX SQLite KTX",
        artifact = "androidx.sqlite:sqlite-ktx:2.4.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Guava",
        artifact = "com.google.guava:guava:33.4.8-android",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Timber",
        artifact = "com.jakewharton.timber:timber:5.0.1",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "AndroidX AppCompat",
        artifact = "androidx.appcompat:appcompat:1.7.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "AndroidX Core KTX",
        artifact = "androidx.core:core-ktx:1.12.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "AndroidX Activity Compose",
        artifact = "androidx.activity:activity-compose:1.9.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "AndroidX Lifecycle",
        artifact = "androidx.lifecycle:lifecycle-runtime-*:2.7.0",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "Jetpack Compose (BOM 2024.12.01)",
        artifact = "androidx.compose:compose-bom",
        license = "Apache License 2.0"
    ),
    OssLibrary(
        name = "AndroidX Navigation Compose",
        artifact = "androidx.navigation:navigation-compose:2.8.4",
        license = "Apache License 2.0"
    )
)
