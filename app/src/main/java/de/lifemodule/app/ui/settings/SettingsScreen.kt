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

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.dashboard.AppModule
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.AppThemeViewModel
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Destructive
import de.lifemodule.app.ui.theme.Secondary
import de.lifemodule.app.ui.theme.Surface
import de.lifemodule.app.ui.theme.colorToHex
import de.lifemodule.app.ui.theme.hexToColor
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.BuildConfig
import de.lifemodule.app.MainActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import de.lifemodule.app.R
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
private fun bundeslaender() = listOf(
    "" to stringResource(R.string.app_settings_alle_bundesweit),
    "DE-NW" to stringResource(R.string.app_settings_nordrheinwestfalen),
    "DE-BY" to stringResource(R.string.app_settings_bayern),
    "DE-BW" to stringResource(R.string.app_settings_badenwuerttemberg),
    "DE-NI" to stringResource(R.string.app_settings_niedersachsen),
    "DE-HE" to stringResource(R.string.app_settings_hessen),
    "DE-SN" to stringResource(R.string.app_settings_sachsen),
    "DE-RP" to stringResource(R.string.app_settings_rheinlandpfalz),
    "DE-SH" to stringResource(R.string.app_settings_schleswigholstein),
    "DE-BE" to stringResource(R.string.app_settings_berlin),
    "DE-TH" to stringResource(R.string.app_settings_thueringen),
    "DE-BB" to stringResource(R.string.app_settings_brandenburg),
    "DE-ST" to stringResource(R.string.app_settings_sachsenanhalt),
    "DE-MV" to stringResource(R.string.app_settings_mecklenburgvorpommern),
    "DE-HH" to stringResource(R.string.app_settings_hamburg),
    "DE-HB" to stringResource(R.string.app_settings_bremen),
    "DE-SL" to stringResource(R.string.app_settings_saarland)
)

/** Supported app languages. */
private val appLanguages = listOf(
    "" to "System",
    "en" to "English",
    "de" to "Deutsch",
    "es" to "Espa\u00f1ol",
    "fr" to "Fran\u00e7ais",
    "pt" to "Portugu\u00eas",
    "it" to "Italiano",
    "nl" to "Nederlands",
    "pl" to "Polski",
    "ru" to "\u0420\u0443\u0441\u0441\u043a\u0438\u0439",
    "uk" to "\u0423\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430",
    "tr" to "T\u00fcrk\u00e7e",
    "ar" to "\u0627\u0644\u0639\u0631\u0628\u064a\u0629",
    "hi" to "\u0939\u093f\u0928\u094d\u0926\u0940",
    "zh" to "\u4e2d\u6587",
    "ja" to "\u65e5\u672c\u8a9e",
    "ko" to "\ud55c\uad6d\uc5b4"
)

/** Countries with bundled holiday data (most relevant). */
private val holidayCountries = listOf(
    "" to "\uD83C\uDF10 Auto (System)",
    "DE" to "\uD83C\uDDE9\uD83C\uDDEA Germany",
    "US" to "\uD83C\uDDFA\uD83C\uDDF8 United States",
    "GB" to "\uD83C\uDDEC\uD83C\uDDE7 United Kingdom",
    "FR" to "\uD83C\uDDEB\uD83C\uDDF7 France",
    "ES" to "\uD83C\uDDEA\uD83C\uDDF8 Spain",
    "IT" to "\uD83C\uDDEE\uD83C\uDDF9 Italy",
    "PT" to "\uD83C\uDDF5\uD83C\uDDF9 Portugal",
    "NL" to "\uD83C\uDDF3\uD83C\uDDF1 Netherlands",
    "BE" to "\uD83C\uDDE7\uD83C\uDDEA Belgium",
    "AT" to "\uD83C\uDDE6\uD83C\uDDF9 Austria",
    "CH" to "\uD83C\uDDE8\uD83C\uDDED Switzerland",
    "PL" to "\uD83C\uDDF5\uD83C\uDDF1 Poland",
    "CZ" to "\uD83C\uDDE8\uD83C\uDDFF Czech Republic",
    "SE" to "\uD83C\uDDF8\uD83C\uDDEA Sweden",
    "NO" to "\uD83C\uDDF3\uD83C\uDDF4 Norway",
    "DK" to "\uD83C\uDDE9\uD83C\uDDF0 Denmark",
    "FI" to "\uD83C\uDDEB\uD83C\uDDEE Finland",
    "RU" to "\uD83C\uDDF7\uD83C\uDDFA Russia",
    "UA" to "\uD83C\uDDFA\uD83C\uDDE6 Ukraine",
    "TR" to "\uD83C\uDDF9\uD83C\uDDF7 Turkey",
    "BR" to "\uD83C\uDDE7\uD83C\uDDF7 Brazil",
    "MX" to "\uD83C\uDDF2\uD83C\uDDFD Mexico",
    "AR" to "\uD83C\uDDE6\uD83C\uDDF7 Argentina",
    "CA" to "\uD83C\uDDE8\uD83C\uDDE6 Canada",
    "AU" to "\uD83C\uDDE6\uD83C\uDDFA Australia",
    "JP" to "\uD83C\uDDEF\uD83C\uDDF5 Japan",
    "KR" to "\uD83C\uDDF0\uD83C\uDDF7 South Korea",
    "CN" to "\uD83C\uDDE8\uD83C\uDDF3 China",
    "IN" to "\uD83C\uDDEE\uD83C\uDDF3 India",
    "SA" to "\uD83C\uDDF8\uD83C\uDDE6 Saudi Arabia",
    "AE" to "\uD83C\uDDE6\uD83C\uDDEA UAE",
    "EG" to "\uD83C\uDDEA\uD83C\uDDEC Egypt",
    "ZA" to "\uD83C\uDDFF\uD83C\uDDE6 South Africa",
    "NG" to "\uD83C\uDDF3\uD83C\uDDEC Nigeria",
    "GR" to "\uD83C\uDDEC\uD83C\uDDF7 Greece",
    "RO" to "\uD83C\uDDF7\uD83C\uDDF4 Romania",
    "HU" to "\uD83C\uDDED\uD83C\uDDFA Hungary",
    "IE" to "\uD83C\uDDEE\uD83C\uDDEA Ireland",
    "IL" to "\uD83C\uDDEE\uD83C\uDDF1 Israel",
    "TH" to "\uD83C\uDDF9\uD83C\uDDED Thailand",
    "ID" to "\uD83C\uDDEE\uD83C\uDDE9 Indonesia",
    "PH" to "\uD83C\uDDF5\uD83C\uDDED Philippines",
    "VN" to "\uD83C\uDDFB\uD83C\uDDF3 Vietnam",
    "CO" to "\uD83C\uDDE8\uD83C\uDDF4 Colombia",
    "CL" to "\uD83C\uDDE8\uD83C\uDDF1 Chile",
    "PE" to "\uD83C\uDDF5\uD83C\uDDEA Peru",
    "NZ" to "\uD83C\uDDF3\uD83C\uDDFF New Zealand",
    "SG" to "\uD83C\uDDF8\uD83C\uDDEC Singapore",
    "MY" to "\uD83C\uDDF2\uD83C\uDDFE Malaysia"
)

private val timeOptions = listOf(
    "06:00", "07:00", "08:00", "09:00", "10:00",
    "11:00", "12:00", "13:00", "14:00", "15:00",
    "16:00", "17:00", "18:00", "19:00", "20:00",
    "21:00", "22:00"
)

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: AppThemeViewModel = hiltViewModel()
) {
    val accent = LocalAccentColor.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    val selectedRegion by viewModel.selectedRegion.collectAsStateWithLifecycle()
    val selectedHolidayCountry by viewModel.selectedHolidayCountry.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val totalDataRows by viewModel.totalDataRows.collectAsStateWithLifecycle()

    val calendarReminder1h by viewModel.calendarReminder1h.collectAsStateWithLifecycle()
    val calendarReminder1d by viewModel.calendarReminder1d.collectAsStateWithLifecycle()
    val calendarReminder1w by viewModel.calendarReminder1w.collectAsStateWithLifecycle()

    val scheduleEnabled by viewModel.scheduleEnabled.collectAsStateWithLifecycle()
    val scheduleMinutes by viewModel.scheduleMinutesBefore.collectAsStateWithLifecycle()
    val scheduleStartHour by viewModel.scheduleStartHour.collectAsStateWithLifecycle()
    val scheduleEndHour by viewModel.scheduleEndHour.collectAsStateWithLifecycle()

    val supplementEnabled by viewModel.supplementEnabled.collectAsStateWithLifecycle()
    val supplementTime by viewModel.supplementTime.collectAsStateWithLifecycle()

    val habitEnabled by viewModel.habitEnabled.collectAsStateWithLifecycle()
    val habitTime by viewModel.habitTime.collectAsStateWithLifecycle()
    val googleBackupEnabled by viewModel.googleBackupEnabled.collectAsStateWithLifecycle()
    val screenshotProtectionEnabled by viewModel.screenshotProtectionEnabled.collectAsStateWithLifecycle()

    var showRegionDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSuppTimeDialog by remember { mutableStateOf(false) }
    var showHabitTimeDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // ── Theme state ────────────────────────────────────────────────────────
    val globalAccent by themeViewModel.globalAccent.collectAsStateWithLifecycle()
    val moduleColorMap by themeViewModel.moduleColors.collectAsStateWithLifecycle()
    var showGlobalColorPicker by remember { mutableStateOf(false) }
    var moduleColorPickerTarget by remember { mutableStateOf<AppModule?>(null) }
    var showModulesList by remember { mutableStateOf(false) }

    // Developer options unlock (tap version 7 times)
    var devTapCount by rememberSaveable { mutableIntStateOf(0) }
    var devUnlocked by rememberSaveable { mutableStateOf(false) }
    val devAlreadyMsg = stringResource(R.string.app_settings_dev_already_unlocked)
    val devUnlockedMsg = stringResource(R.string.app_settings_dev_unlocked)
    val devStepsMsg = stringResource(R.string.app_settings_dev_steps_away)

    // Notification permission launcher (Android 13+)
    val notifDeniedMsg = stringResource(R.string.app_settings_benachrichtigungen_wurden_verweigert)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted)
        if (!granted) {
            Toast.makeText(context, notifDeniedMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // Backup restore file picker
    val restoreFailedMsg = stringResource(R.string.app_settings_backup_restore_failed)
    val restoreFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreDialog = true
        }
    }

    val exportSuccessMsg = stringResource(R.string.app_settings_export_success)
    val exportPartialMsg = stringResource(R.string.app_settings_export_partial)
    val exportFailedMsg = stringResource(R.string.app_settings_export_fehlgeschlagen)
    val exportFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = viewModel.exportData(context, uri)
                when (result) {
                    2 -> Toast.makeText(context, exportSuccessMsg, Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(context, exportPartialMsg, Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(context, exportFailedMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val backupCreatedMsg = stringResource(R.string.app_settings_backup_erstellt)
    val backupFailedMsg = stringResource(R.string.app_settings_backup_fehlgeschlagen)
    val backupFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream") // or appropriate MIME
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = viewModel.createBackup(uri)
                if (success) {
                    Toast.makeText(context, backupCreatedMsg, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, backupFailedMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Import package file picker
    val importPackagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            navController.navigate(
                de.lifemodule.app.ui.navigation.AppRoute.Import(packageUri = uri.toString())
            )
        }
    }

    // ── Color picker dialogs (shown over Scaffold) ──────────────────────────
    if (showGlobalColorPicker) {
        ColorPickerDialog(
            title = stringResource(R.string.app_settings_globale_akzentfarbe),
            initialColor = globalAccent,
            onDismiss = { showGlobalColorPicker = false },
            onColorPicked = { themeViewModel.setGlobalAccent(it) },
            onReset = {
                themeViewModel.setGlobalAccent(hexToColor("A2FF00"))
                showGlobalColorPicker = false
            }
        )
    }
    moduleColorPickerTarget?.let { targetModule ->
        ColorPickerDialog(
            title = "${targetModule.emoji} ${stringResource(targetModule.displayNameRes)}",
            initialColor = moduleColorMap[targetModule.id] ?: globalAccent,
            onDismiss = { moduleColorPickerTarget = null },
            onColorPicked = { themeViewModel.setModuleAccent(targetModule.id, it) }
        )
    }

    Scaffold(
        topBar = {
            LMTopBar(
                title = stringResource(R.string.app_settings_einstellungen),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ═══════════════════════════════════════
            // DESIGN & FARBEN
            // ═══════════════════════════════════════
            item { SectionTitle(stringResource(R.string.app_settings_design_farben)) }

            // Global accent colour
            item {
                SettingsCard(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.app_settings_globale_akzentfarbe),
                    subtitle = "#${colorToHex(globalAccent)}",
                    onClick = { showGlobalColorPicker = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(globalAccent, CircleShape)
                    )
                }
            }

            // Per-module overrides
            item {
                LMCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showModulesList = !showModulesList },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = globalAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.app_settings_modulfarben),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(R.string.app_settings_individuelle_farben_pro_modul),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                            }
                            Icon(
                                imageVector = if (showModulesList)
                                    Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Secondary
                            )
                        }
                        if (showModulesList) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(4.dp))
                            AppModule.entries.forEach { module ->
                                val modColor = moduleColorMap[module.id]
                                val effectiveColor = modColor ?: globalAccent
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(module.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(module.displayNameRes),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            if (modColor != null) "#${colorToHex(effectiveColor)}" else stringResource(R.string.app_settings_global),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (modColor != null) effectiveColor else Secondary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(effectiveColor, CircleShape)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    TextButton(
                                        onClick = { moduleColorPickerTarget = module }
                                    ) {
                                        Text(
                                            stringResource(R.string.app_settings_aendern),
                                            color = effectiveColor,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    if (modColor != null) {
                                        TextButton(
                                            onClick = { themeViewModel.setModuleAccent(module.id, null) }
                                        ) {
                                            Text(
                                                stringResource(R.string.app_settings_reset),
                                                color = Secondary,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // MASTER NOTIFICATION TOGGLE
            // ═══════════════════════════════════════
            item {
                SectionTitle(stringResource(R.string.app_settings_benachrichtigungen))
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.app_settings_push_benachrichtigungen),
                    subtitle = if (notificationsEnabled) stringResource(R.string.app_settings_aktiviert) else stringResource(R.string.app_settings_deaktiviert)
                ) {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@Switch
                                }
                            }
                            viewModel.setNotificationsEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = accent)
                    )
                }
            }

            // ═══════════════════════════════════════
            // ️ EXACT ALARM PERMISSION BANNER
            // ═══════════════════════════════════════
            if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                if (!alarmManager.canScheduleExactAlarms()) {
                    item {
                        LMCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        context.startActivity(intent)
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9F0A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.app_settings_exact_alarm_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF9F0A)
                                    )
                                    Text(
                                        text = stringResource(R.string.app_settings_exact_alarm_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // 📅 CALENDAR NOTIFICATIONS
            // ═══════════════════════════════════════
            if (notificationsEnabled) {
                item {
                    LMCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📅", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.app_settings_kalender),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.app_settings_erinnerungen_vor_terminen),
                                style = MaterialTheme.typography.bodySmall,
                                color = Secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            CheckboxRow(stringResource(R.string.app_settings_1_stunde_vorher), calendarReminder1h) { viewModel.setCalendar1h(it) }
                            CheckboxRow(stringResource(R.string.app_settings_1_tag_vorher), calendarReminder1d) { viewModel.setCalendar1d(it) }
                            CheckboxRow(stringResource(R.string.app_settings_1_woche_vorher), calendarReminder1w) { viewModel.setCalendar1w(it) }
                        }
                    }
                }

                // ═══════════════════════════════════════
                // 🎓 STUNDENPLAN NOTIFICATIONS
                // ═══════════════════════════════════════
                item {
                    LMCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎓", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.app_settings_stundenplan),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = scheduleEnabled,
                                    onCheckedChange = { viewModel.setScheduleEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = accent)
                                )
                            }

                            if (scheduleEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.app_settings_erinnerung_min_vorher, scheduleMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary
                                )
                                Slider(
                                    value = scheduleMinutes.toFloat(),
                                    onValueChange = { viewModel.setScheduleMinutes(it.toInt()) },
                                    valueRange = 5f..60f,
                                    steps = 10,
                                    colors = SliderDefaults.colors(
                                        thumbColor = accent,
                                        activeTrackColor = accent
                                    )
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════
                // 💊 SUPPLEMENT NOTIFICATIONS
                // ═══════════════════════════════════════
                item {
                    LMCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💊", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.app_settings_supplements),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = supplementEnabled,
                                    onCheckedChange = { viewModel.setSupplementEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = accent)
                                )
                            }

                            if (supplementEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showSuppTimeDialog = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.app_settings_taegliche_erinnerung_um),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Secondary
                                    )
                                    Text(
                                        stringResource(R.string.app_settings_uhrzeit_format, supplementTime),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════
                // ✅ HABIT NOTIFICATIONS
                // ═══════════════════════════════════════
                item {
                    LMCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.app_settings_habits),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = habitEnabled,
                                    onCheckedChange = { viewModel.setHabitEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = accent)
                                )
                            }

                            if (habitEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showHabitTimeDialog = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.app_settings_taegliche_erinnerung_um),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Secondary
                                    )
                                    Text(
                                        stringResource(R.string.app_settings_uhrzeit_format, habitTime),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // 🕐 STUNDENPLAN ZEITFENSTER
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_stundenplan_zeitfenster))
            }

            item {
                LMCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.app_settings_schedule_start_hour, scheduleStartHour),
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Slider(
                            value = scheduleStartHour.toFloat(),
                            onValueChange = { viewModel.setScheduleStartHour(it.toInt()) },
                            valueRange = 0f..23f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.app_settings_schedule_end_hour, scheduleEndHour),
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Slider(
                            value = scheduleEndHour.toFloat(),
                            onValueChange = { viewModel.setScheduleEndHour(it.toInt()) },
                            valueRange = 1f..24f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent
                            )
                        )
                    }
                }
            }

            // ═══════════════════════════════════════
            // 🌐 LANGUAGE
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_sprache))
            }

            item {
                val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                val currentLabel = appLanguages.firstOrNull { it.first == currentTag }?.second
                    ?: appLanguages.first().second
                SettingsCard(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.app_settings_sprache),
                    subtitle = currentLabel,
                    onClick = { showLanguageDialog = true }
                )
            }

            // ═══════════════════════════════════════
            // FEIERTAGE
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_feiertage))
            }

            item {
                val countryLabel = holidayCountries.firstOrNull { it.first == selectedHolidayCountry }?.second
                    ?: holidayCountries.first().second
                SettingsCard(
                    icon = Icons.Default.LocationOn,
                    title = stringResource(R.string.app_settings_feiertage_land),
                    subtitle = countryLabel,
                    onClick = { showCountryDialog = true }
                )
            }

            // Show region picker only when country = DE
            if (selectedHolidayCountry == "DE") {
                item {
                    SettingsCard(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.app_settings_bundesland),
                        subtitle = bundeslaender().firstOrNull { it.first == selectedRegion }?.second
                            ?: stringResource(R.string.app_settings_alle),
                        onClick = { showRegionDialog = true }
                    )
                }
            }

            // ═══════════════════════════════════════
            // PERSÖNLICHE DATEN
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_personal_data))
            }

            item {
                val age by viewModel.userAge.collectAsStateWithLifecycle()
                LMCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.app_settings_age) + ": $age",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Slider(
                            value = age.toFloat(),
                            onValueChange = { viewModel.setUserAge(it.toInt()) },
                            valueRange = 10f..120f,
                            steps = 109,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent
                            )
                        )
                    }
                }
            }

            item {
                val heightCm by viewModel.userHeightCm.collectAsStateWithLifecycle()
                LMCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.app_settings_height) + ": $heightCm cm",
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                        Slider(
                            value = heightCm.toFloat(),
                            onValueChange = { viewModel.setUserHeightCm(it.toInt()) },
                            valueRange = 100f..250f,
                            steps = 149,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent
                            )
                        )
                    }
                }
            }

            // ═══════════════════════════════════════
            // DATEN
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_daten))
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.app_settings_alle_daten_exportieren),
                    subtitle = stringResource(R.string.app_settings_export_untertitel, totalDataRows),
                    onClick = {
                        val timestamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        exportFilePicker.launch("LifeModule_Export_$timestamp.zip")
                    }
                )
            }

            // ═══════════════════════════════════════
            // BACKUP
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_backup))
            }

            // Backup reminder: warn if no backup in 30+ days
            item {
                val prefs = remember {
                    context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
                }
                val lastBackupMillis = remember { prefs.getLong("last_backup_millis", 0L) }
                val daysSinceBackup = if (lastBackupMillis == 0L) -1L
                    else java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastBackupMillis)
                val isGerman = java.util.Locale.getDefault().language == "de"

                if (lastBackupMillis == 0L || daysSinceBackup >= 30) {
                    val msg = if (lastBackupMillis == 0L) {
                        if (isGerman) "Du hast noch nie ein Backup erstellt. Sichere deine Daten regelmäßig!"
                        else "You have never created a backup. Back up your data regularly!"
                    } else {
                        if (isGerman) "Dein letztes Backup ist $daysSinceBackup Tage her. Zeit für ein neues!"
                        else "Your last backup was $daysSinceBackup days ago. Time for a new one!"
                    }
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Save,
                    title = stringResource(R.string.app_settings_backup_erstellen),
                    subtitle = stringResource(R.string.app_settings_backup_erstellen_subtitle),
                    onClick = {
                        val timestamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        backupFilePicker.launch("LifeModule_Backup_$timestamp.lmbackup")
                    }
                )
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Restore,
                    title = stringResource(R.string.app_settings_backup_wiederherstellen),
                    subtitle = stringResource(R.string.app_settings_backup_wiederherstellen_subtitle),
                    onClick = {
                        restoreFilePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }
                )
            }

            item {
                SettingsCard(
                    icon = Icons.Default.CloudUpload,
                    title = stringResource(R.string.app_settings_google_backup),
                    subtitle = if (googleBackupEnabled) stringResource(R.string.app_settings_google_backup_on) else stringResource(R.string.app_settings_google_backup_off)
                ) {
                    Switch(
                        checked = googleBackupEnabled,
                        onCheckedChange = { viewModel.setGoogleBackupEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = accent)
                    )
                }
            }

            // ── Import Package ──────────────────────────────────────────
            item {
                SettingsCard(
                    icon = Icons.AutoMirrored.Filled.Input,
                    title = stringResource(R.string.app_settings_import_package),
                    subtitle = stringResource(R.string.app_settings_import_package_subtitle),
                    onClick = {
                        importPackagePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }
                )
            }

            // ═══════════════════════════════════════
            // SICHERHEIT
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_sicherheit))
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.app_settings_screenshot_schutz),
                    subtitle = if (screenshotProtectionEnabled) stringResource(R.string.app_settings_screenshot_schutz_on) else stringResource(R.string.app_settings_screenshot_schutz_off)
                ) {
                    Switch(
                        checked = screenshotProtectionEnabled,
                        onCheckedChange = {
                            viewModel.setScreenshotProtectionEnabled(it)
                            // Apply immediately to the current window
                            (context as? MainActivity)?.applyScreenshotProtection()
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = accent)
                    )
                }
            }

            // ═══════════════════════════════════════
            // DATENSCHUTZ & LIZENZ
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_datenschutz_lizenz))
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.app_settings_datenschutz),
                    subtitle = null,
                    onClick = { navController.navigate(AppRoute.Privacy) }
                )
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Gavel,
                    title = stringResource(R.string.app_settings_lizenz),
                    subtitle = null,
                    onClick = { navController.navigate(AppRoute.Licenses) }
                )
            }

            // ═══════════════════════════════════════
            // FEHLERPROTOKOLL
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_fehlerprotokoll))
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Report,
                    title = stringResource(R.string.app_settings_fehlerprotokoll_titel),
                    subtitle = stringResource(R.string.app_settings_fehlerprotokoll_untertitel),
                    onClick = { navController.navigate(AppRoute.ErrorLog) }
                )
            }

            // ═══════════════════════════════════════
            // INFO
            // ═══════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionTitle(stringResource(R.string.app_settings_info))
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.app_settings_about),
                    subtitle = stringResource(R.string.app_settings_app_beschreibung, BuildConfig.VERSION_NAME),
                    onClick = {
                        if (devUnlocked) {
                            Toast.makeText(context, devAlreadyMsg, Toast.LENGTH_SHORT).show()
                            navController.navigate(AppRoute.Debug)
                        } else {
                            devTapCount++
                            val remaining = 7 - devTapCount
                            when {
                                remaining <= 0 -> {
                                    devUnlocked = true
                                    Toast.makeText(context, devUnlockedMsg, Toast.LENGTH_LONG).show()
                                    navController.navigate(AppRoute.Debug)
                                }
                                remaining <= 3 -> {
                                    Toast.makeText(
                                        context,
                                        String.format(devStepsMsg, remaining),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                else -> { /* Still counting silently */ }
                            }
                        }
                    }
                )
            }

            // ═══════════════════════════════════════
            // DEBUG (hidden - tap version 7× to unlock)
            // ═══════════════════════════════════════
            if (devUnlocked) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SectionTitle(stringResource(R.string.app_settings_entwickler))
                }
                item {
                    SettingsCard(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.app_settings_time_travel),
                        subtitle = stringResource(R.string.app_settings_time_travel_subtitle),
                        onClick = { navController.navigate(AppRoute.Debug) }
                    )
                }
                item {
                    SettingsCard(
                        icon = Icons.Default.DeleteForever,
                        title = stringResource(R.string.app_settings_reset_all_data),
                        subtitle = stringResource(R.string.app_settings_reset_all_data_subtitle),
                        onClick = { showResetDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ── Dialogs ──

    // Region picker
    if (showRegionDialog) {
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text(stringResource(R.string.app_settings_bundesland_waehlen), style = MaterialTheme.typography.titleMedium) },
            text = {
                val bundeslaenderList = bundeslaender()
                LazyColumn {
                    items(items = bundeslaenderList) { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setRegion(code)
                                    showRegionDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRegion == code,
                                onClick = {
                                    viewModel.setRegion(code)
                                    showRegionDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionDialog = false }) {
                    Text(stringResource(R.string.app_settings_fertig), color = accent)
                }
            }
        )
    }

    // Language picker
    if (showLanguageDialog) {
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.app_settings_sprache_waehlen), style = MaterialTheme.typography.titleMedium) },
            text = {
                LazyColumn {
                    items(items = appLanguages) { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val locales = if (code.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                                        else LocaleListCompat.forLanguageTags(code)
                                    AppCompatDelegate.setApplicationLocales(locales)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTag == code,
                                onClick = {
                                    val locales = if (code.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                                        else LocaleListCompat.forLanguageTags(code)
                                    AppCompatDelegate.setApplicationLocales(locales)
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.app_settings_fertig), color = accent)
                }
            }
        )
    }

    // Holiday country picker
    if (showCountryDialog) {
        AlertDialog(
            onDismissRequest = { showCountryDialog = false },
            title = { Text(stringResource(R.string.app_settings_land_waehlen), style = MaterialTheme.typography.titleMedium) },
            text = {
                LazyColumn {
                    items(items = holidayCountries) { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setHolidayCountry(code)
                                    showCountryDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedHolidayCountry == code,
                                onClick = {
                                    viewModel.setHolidayCountry(code)
                                    showCountryDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryDialog = false }) {
                    Text(stringResource(R.string.app_settings_fertig), color = accent)
                }
            }
        )
    }

    // Supplement time picker
    if (showSuppTimeDialog) {
        TimePickerDialog(
            title = stringResource(R.string.app_settings_supplementerinnerung),
            currentValue = supplementTime,
            onSelect = { time ->
                viewModel.setSupplementTime(time)
                showSuppTimeDialog = false
            },
            onDismiss = { showSuppTimeDialog = false }
        )
    }

    // Habit time picker
    if (showHabitTimeDialog) {
        TimePickerDialog(
            title = stringResource(R.string.app_settings_habiterinnerung),
            currentValue = habitTime,
            onSelect = { time ->
                viewModel.setHabitTime(time)
                showHabitTimeDialog = false
            },
            onDismiss = { showHabitTimeDialog = false }
        )
    }

    // Reset all data confirmation dialog
    if (showResetDialog) {
        val resetSuccessMsg = stringResource(R.string.app_settings_reset_success)
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    stringResource(R.string.app_settings_reset_all_data),
                    style = MaterialTheme.typography.titleMedium,
                    color = Destructive
                )
            },
            text = {
                Text(
                    stringResource(R.string.app_settings_reset_confirm_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    scope.launch {
                        viewModel.resetAllData()
                        Toast.makeText(context, resetSuccessMsg, Toast.LENGTH_LONG).show()
                        // Force restart via recreate
                        (context as? android.app.Activity)?.recreate()
                    }
                }) {
                    Text(stringResource(R.string.app_settings_reset_confirm), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.app_settings_abbrechen))
                }
            },
            containerColor = Surface
        )
    }

    // Restore backup confirmation dialog
    if (showRestoreDialog && pendingRestoreUri != null) {
        val restoreSuccessMsg = stringResource(R.string.app_settings_backup_restore_success)
        AlertDialog(
            onDismissRequest = {
                showRestoreDialog = false
                pendingRestoreUri = null
            },
            title = {
                Text(
                    stringResource(R.string.app_settings_backup_restore_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Destructive
                )
            },
            text = {
                Text(
                    stringResource(R.string.app_settings_backup_restore_confirm_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    val uri = pendingRestoreUri!!
                    pendingRestoreUri = null
                    scope.launch {
                        val success = viewModel.restoreBackup(uri)
                        if (success) {
                            Toast.makeText(context, restoreSuccessMsg, Toast.LENGTH_LONG).show()
                            (context as? android.app.Activity)?.recreate()
                        } else {
                            Toast.makeText(context, restoreFailedMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.app_settings_backup_restore_confirm), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    pendingRestoreUri = null
                }) {
                    Text(stringResource(R.string.app_settings_abbrechen))
                }
            },
            containerColor = Surface
        )
    }
}

// ──────────────────────────────────────────
// HELPER COMPOSABLES
// ──────────────────────────────────────────

/**
 * RGB-slider + Hex-input colour picker dialog.
 * Immediately calls [onColorPicked] when the user taps "Übernehmen".
 * If [onReset] is non-null a "Standard wiederherstellen" link is shown.
 */
@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorPicked: (Color) -> Unit,
    onReset: (() -> Unit)? = null
) {
    var red   by remember(initialColor) { mutableFloatStateOf(initialColor.red) }
    var green by remember(initialColor) { mutableFloatStateOf(initialColor.green) }
    var blue  by remember(initialColor) { mutableFloatStateOf(initialColor.blue) }
    val currentColor = Color(red, green, blue)
    var hexInput by remember(initialColor) {
        mutableStateOf(colorToHex(initialColor))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                // ── Colour preview ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(currentColor, RoundedCornerShape(12.dp))
                )

                // ── R slider ───────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R", modifier = Modifier.width(18.dp), color = Color.Red, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = red,
                        onValueChange = { red = it; hexInput = colorToHex(Color(red, green, blue)) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                    )
                    Text("${(red * 255).roundToInt()}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall)
                }

                // ── G slider ───────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", modifier = Modifier.width(18.dp), color = Color.Green, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = green,
                        onValueChange = { green = it; hexInput = colorToHex(Color(red, green, blue)) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green)
                    )
                    Text("${(green * 255).roundToInt()}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall)
                }

                // ── B slider ───────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B", modifier = Modifier.width(18.dp), color = Color(0xFF8888FF.toInt()), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = blue,
                        onValueChange = { blue = it; hexInput = colorToHex(Color(red, green, blue)) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF8888FF.toInt()),
                            activeTrackColor = Color(0xFF8888FF.toInt())
                        )
                    )
                    Text("${(blue * 255).roundToInt()}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall)
                }

                // ── Hex input ────────────────────────────────────────
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val cleaned = input.trimStart('#')
                        if (cleaned.length == 6) {
                            val parsed = hexToColor(cleaned)
                            red = parsed.red
                            green = parsed.green
                            blue = parsed.blue
                        }
                    },
                    label = { Text(stringResource(R.string.app_settings_hex_label)) },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Reset link ────────────────────────────────────────
                if (onReset != null) {
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.app_settings_standard_wiederherstellen), color = Secondary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorPicked(Color(red, green, blue))
                onDismiss()
            }) { Text(stringResource(R.string.app_settings_uebernehmen)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.app_settings_abbrechen)) }
        }
    )
}

// ──────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = LocalAccentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = accent)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    currentValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalAccentColor.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            LazyColumn {
                items(items = timeOptions) { time ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(time) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == time,
                            onClick = { onSelect(time) },
                            colors = RadioButtonDefaults.colors(selectedColor = accent)
                        )
                        Text(
                            text = stringResource(R.string.app_settings_uhrzeit_format, time),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.app_settings_fertig), color = accent)
            }
        }
    )
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val accent = LocalAccentColor.current
    LMCard(
        modifier = if (onClick != null) Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) else Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary
                    )
                }
            }
            trailing?.invoke()
        }
    }
}
