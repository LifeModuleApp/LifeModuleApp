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

package de.lifemodule.app

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import de.lifemodule.app.ui.dashboard.ModulePreferences
import de.lifemodule.app.ui.navigation.AppNavHost
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.AppThemeViewModel
import de.lifemodule.app.ui.theme.LifeModuleTheme
import de.lifemodule.app.util.time.TimeProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var modulePreferences: ModulePreferences

    @Inject
    lateinit var timeProvider: TimeProvider

    /** Launcher for POST_NOTIFICATIONS runtime permission (Android 13+). */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted / denied - channels are already created in Application.onCreate() */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge display (API 35+ default; backported to API 21+ via Activity 1.9.0).
        // Scaffold in Compose already handles inset padding automatically.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // On Android 13+ the POST_NOTIFICATIONS permission must be requested at runtime.
        requestNotificationPermissionIfNeeded()

        // Apply screenshot/screen-recording protection based on user preference
        applyScreenshotProtection()

        setContent {
            // Activity-scoped: survives navigation, drives entire theme
            val themeViewModel: AppThemeViewModel = hiltViewModel()
            val globalAccent by themeViewModel.globalAccent.collectAsState()
            val moduleColors by themeViewModel.moduleColors.collectAsState()

            LifeModuleTheme(
                accentColor = globalAccent,
                moduleColors = moduleColors
            ) {
                val navController = rememberNavController()

                // Check if DB was lost on this startup (e.g. encryption key lost after device transfer)
                val prefs = remember {
                    applicationContext.getSharedPreferences("lifemodule_prefs", MODE_PRIVATE)
                }
                var showDbLostDialog by remember {
                    mutableStateOf(prefs.getBoolean("db_lost_on_startup", false))
                }
                if (showDbLostDialog) {
                    val isGerman = java.util.Locale.getDefault().language == "de"
                    AlertDialog(
                        onDismissRequest = { },
                        title = { Text(if (isGerman) "Datenbank zurückgesetzt" else "Database Reset") },
                        text = {
                            Text(
                                if (isGerman)
                                    "Der Verschlüsselungsschlüssel deiner Datenbank konnte nicht wiederhergestellt werden (z. B. nach einem Gerätewechsel oder Werksreset). Deine bisherigen Daten sind leider nicht mehr zugänglich und die App wurde zurückgesetzt.\n\nWenn du ein Backup hast, kannst du es über Einstellungen → Backup wiederherstellen."
                                else
                                    "Your database encryption key could not be recovered (e.g. after a device transfer or factory reset). Your previous data is no longer accessible and the app has been reset.\n\nIf you have a backup, you can restore it via Settings → Backup."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                prefs.edit().remove("db_lost_on_startup").apply()
                                showDbLostDialog = false
                            }) {
                                Text("OK")
                            }
                        }
                    )
                }

                AppNavHost(
                    navController = navController,
                    modulePreferences = modulePreferences,
                    timeProvider = timeProvider
                )

                // Handle incoming share/view intent for package import
                val importUri = remember { getImportUri(intent) }
                LaunchedEffect(importUri) {
                    importUri?.let { uri ->
                        navController.navigate(
                            AppRoute.Import(packageUri = uri.toString())
                        )
                    }
                }
            }
        }
    }

    /**
     * Applies or removes FLAG_SECURE based on the user's "screenshot_protection_enabled" preference.
     * When active, the system prevents screenshots, screen recordings, and the recent-apps thumbnail.
     */
    fun applyScreenshotProtection() {
        val prefs = getSharedPreferences("notification_settings", MODE_PRIVATE)
        val enabled = prefs.getBoolean("screenshot_protection_enabled", true)
        if (enabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(perm)
            }
        }
    }

    /**
     * Extracts a package URI from an incoming ACTION_VIEW or ACTION_SEND intent.
     * Returns null if the intent is not a package import.
     */
    private fun getImportUri(intent: Intent?): android.net.Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val mime = intent.type ?: ""
                val data = intent.data
                if (isSupportedImportMime(mime) && isSupportedImportUri(data)) {
                    data
                } else null
            }
            Intent.ACTION_SEND -> {
                val mime = intent.type ?: ""
                val stream = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? android.net.Uri
                if (isSupportedImportMime(mime) && isSupportedImportUri(stream)) {
                    stream
                } else null
            }
            else -> null
        }
    }

    private fun isSupportedImportMime(mime: String): Boolean {
        val normalized = mime.lowercase()
        return normalized.contains("zip") || normalized.contains("octet-stream")
    }

    private fun isSupportedImportUri(uri: android.net.Uri?): Boolean {
        if (uri == null) return false
        return when (uri.scheme?.lowercase()) {
            "content", "file" -> true
            else -> false
        }
    }
}
