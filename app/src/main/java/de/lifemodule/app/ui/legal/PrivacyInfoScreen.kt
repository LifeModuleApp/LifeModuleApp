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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.lifemodule.app.ui.components.LMTopBar
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Secondary
import java.util.Locale

/**
 * Privacy information screen – DSGVO-compliant, shown in DE or EN
 * based on the device locale. No network, fully local.
 */
@Composable
fun PrivacyInfoScreen(navController: NavController) {
    val isGerman = Locale.getDefault().language == "de"
    val title = if (isGerman) "Datenschutz" else "Privacy"

    Scaffold(
        topBar = {
            LMTopBar(
                title = title,
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isGerman) PRIVACY_DE else PRIVACY_EN,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private const val PRIVACY_DE = """1. Verantwortlicher

Verantwortlich für die Datenverarbeitung im Sinne der DSGVO ist:
Paul Bernhard Colin Witzke
Freiligrathstraße 6
44623 Herne, Deutschland
E-Mail: contact@lifemodule.de

2. Grundprinzip (Privacy First)

LifeModule ist eine rein lokal arbeitende Open-Source-Anwendung. Es werden zu keinem Zeitpunkt personenbezogene Daten an externe Server, Cloud-Dienste, den Entwickler oder sonstige Dritte übertragen. Die App enthält keine Werbe-Tracker oder Analyse-Software.

3. Lokale Datenspeicherung und Sicherheit

Alle von dir eingegebenen Daten werden ausschließlich lokal auf deinem Endgerät in einer verschlüsselten Datenbank (SQLCipher AES-256) gespeichert. Du behältst die vollständige Kontrolle über deine Daten.

4. Verarbeitung von Gesundheitsdaten (Art. 9 DSGVO)

Die App ermöglicht die Erfassung sensibler Gesundheitsdaten (z. B. Gewicht, Schlafdaten, Herzfrequenz). Die Verarbeitung dieser Daten erfolgt ausschließlich lokal auf deinem Gerät. Du kannst alle Daten jederzeit löschen, indem du die App-Daten in den Android-Einstellungen löschst oder die App deinstallierst.

5. Google Health Connect

Sofern du der App die entsprechende Berechtigung erteilst, liest LifeModule Daten aus Google Health Connect (z. B. Schritte, Distanz, Herzfrequenz, Schlafdaten). Dieser Zugriff erfolgt ausschließlich lesend. Die Daten werden nur lokal auf deinem Gerät zur Darstellung von Statistiken verwendet und niemals an Dritte weitergegeben.

6. Deine Rechte

Da der Entwickler keinen Zugriff auf deine Daten hat, kannst du deine Betroffenenrechte direkt in der App ausüben:

• Löschung: App-Daten in den Android-Einstellungen löschen oder die App deinstallieren.
• Datenübertragbarkeit: Daten über die integrierten Export-Funktionen (z. B. CSV-Export) exportieren.

7. Quellcode

Der vollständige Quellcode der App ist auf GitHub verfügbar. Du kannst jederzeit überprüfen, wie deine Daten verarbeitet werden.

8. Kein Medizinprodukt

LifeModule ist keine medizinische Software und kein Medizinprodukt im Sinne der EU-Medizinprodukteverordnung (MDR 2017/745). Die App stellt keine Diagnosen, gibt keine Therapieempfehlungen und ersetzt keine ärztliche Beratung. Alle angezeigten Werte (z. B. Kalorien, Nährwerte, Gesundheitsdaten) dienen ausschließlich der persönlichen Übersicht und Selbstorganisation.

9. Datensicherung und Datenverlust

LifeModule befindet sich in aktiver Entwicklung. Es kann vorkommen, dass durch Software-Fehler oder Aktualisierungen Daten verloren gehen – beispielsweise durch Datenbankmigrationen bei App-Updates. Der Entwickler übernimmt keine Haftung für Datenverlust. Bitte nutze regelmäßig die integrierte Backup-Funktion, um deine Daten zu sichern."""

private const val PRIVACY_EN = """1. Data Controller

The data controller responsible for processing data under the GDPR is:
Paul Bernhard Colin Witzke
Freiligrathstraße 6
44623 Herne, Germany
Email: contact@lifemodule.de

2. Core Principle (Privacy First)

LifeModule is a fully local, open-source application. At no point is personal data transmitted to external servers, cloud services, the developer, or any third parties. The app contains no advertising trackers or analytics software.

3. Local Data Storage and Security

All data you enter is stored exclusively and locally on your device in an encrypted database (SQLCipher AES-256). You retain full control over your data.

4. Processing of Health Data (Art. 9 GDPR)

The app allows for the recording of sensitive health data (e.g., weight, sleep data, heart rate). The processing of this data occurs entirely locally on your device. You may delete all data at any time by clearing the app's data in your Android settings or by uninstalling the app.

5. Google Health Connect

If you grant the app the necessary permissions, LifeModule reads data from Google Health Connect (e.g., steps, distance, heart rate, sleep data). This access is read-only. Data is used solely locally on your device to display statistics and is never shared with third parties.

6. Your Rights

Because the developer has no access to your data, you can exercise your data subject rights directly within the app:

• Erasure: Clear the app's data in Android settings or uninstall the app.
• Data Portability: Export your data using the built-in export functions (e.g., CSV export).

7. Source Code

The complete source code of the app is available on GitHub. You can verify at any time how your data is processed.

8. Not a Medical Product

LifeModule is not medical software and is not a medical device within the meaning of the EU Medical Device Regulation (MDR 2017/745). The app does not provide diagnoses, therapy recommendations, or a substitute for professional medical advice. All displayed values (e.g., calories, nutritional data, health data) are for personal reference and self-organization only.

9. Data Backup and Data Loss

LifeModule is under active development. Data loss may occur due to software bugs or app updates – for example, through database migrations during updates. The developer assumes no liability for data loss. Please use the built-in backup function regularly to safeguard your data."""
