# LifeModule

A modular, privacy-focused life tracking app for Android.

LifeModule keeps all your data encrypted on-device. No accounts, no cloud, no tracking, no subscriptions.

## Features

| Module | Description |
|--------|-------------|
| **Nutrition** | Track meals, calories, and macros. Barcode scanner for quick food lookup. |
| **Gym** | Workout templates, exercise library, session logging with sets/reps/weight. |
| **Health** | Google Health Connect integration (steps, distance, sleep, heart rate). Weight tracking. |
| **Habits** | Daily/weekly habit tracking with streaks. Positive and negative habits, emoji support. |
| **Mood** | Daily mood, energy, stress, and sleep quality journaling. |
| **Planner** | Calendar with events, holidays (bundled offline), and recurring entries. Course schedule. |
| **Recipes** | Recipe management with ingredients, prep/cook time, and servings. |
| **Shopping** | Shopping list with categories and checkboxes. |
| **Logbook** | Digital vehicle logbook with append-only hash chains. |
| **Scanner** | Receipt scanner with on-device OCR. |
| **Work Time** | Clock in/out tracking with break management and project tagging. |
| **Analytics** | Activity dashboard across all modules. |
| **Backup** | Full database export/import as encrypted `.lmbackup` files. |

## Disclaimer

LifeModule is **not** a medical device or medical software within the meaning of the EU Medical Device Regulation (MDR 2017/745). The app does not provide diagnoses, therapy recommendations, or a substitute for professional medical advice. All displayed values (e.g., calories, nutritional data, health metrics) are for personal reference and self-organization only.

LifeModule is **not** certified tax or accounting software. Features like the vehicle logbook and receipt scanner are tools for personal record-keeping. They do not guarantee compliance with any tax regulations. Consult a tax professional for binding advice.

The app is under active development. Data loss may occur due to software bugs or updates. Use the built-in backup function regularly to safeguard your data.

## Privacy

- All data stored locally in an encrypted database (SQLCipher AES-256)
- No internet permission — the app cannot phone home
- No analytics, no ads, no tracking SDKs
- Health Connect access is read-only
- Full source code available for audit

For the full privacy policy, see [lifemodule.de/app-datenschutz](https://lifemodule.de/app-datenschutz).

## Architecture

```
LifeModule/
├── app/              # Main application, navigation, DI, themes
├── core/             # Shared database, entities, DAOs, utilities
└── features/
    ├── analytics/    # Activity dashboard
    ├── gym/          # Workout tracking
    ├── health/       # Health Connect + weight
    ├── logbook/      # Vehicle logbook
    ├── nutrition/    # Meals & food items
    ├── planner/      # Calendar & courses
    ├── recipes/      # Recipe management
    ├── scanner/      # Receipt scanner
    └── shopping/     # Shopping list
```

- **Language:** 100% Kotlin
- **UI:** Jetpack Compose with Material 3
- **DI:** Hilt
- **Database:** Room + SQLCipher (AES-256 encryption)
- **Build:** Gradle with version catalog, KSP for annotation processing
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35

Each feature is a self-contained Gradle module. Habits, Mood, Work Time, and Backup are integrated into `core/` and `app/`.

## Building

**Requirements:** Android Studio Ladybug or newer, JDK 17

```bash
git clone https://github.com/LifeModuleApp/LifeModuleApp.git
cd LifeModuleApp
./gradlew assembleDebug
```

The debug build uses an auto-generated encryption key. For release builds, configure your signing keystore in Android Studio.

## Database

The app uses Room with explicit schema migrations. User data is preserved across updates.

- Schema exports are committed to `app/schemas/` and `core/schemas/` for migration testing
- Downgrade protection prevents crashes on older versions

## Contributing

Issues and pull requests are welcome.

The modular architecture makes it straightforward to add new features — create a new module under `features/`, define your entities in `core/`, and wire up navigation in `app/`.

When modifying the database schema:
1. Write a `Migration` object in `Migrations.kt` before incrementing the version
2. Register it in `DatabaseModule.kt`
3. Test against the exported schema JSONs in `app/schemas/` and `core/schemas/`

## Third-Party Libraries

LifeModule uses the following open-source libraries:

| Library | License |
|---------|---------|
| Jetpack Compose (BOM 2024.12.01) | Apache License 2.0 |
| AndroidX Core, Lifecycle, Activity, Navigation, Room, DataStore, SQLite KTX | Apache License 2.0 |
| Dagger Hilt / Hilt Navigation Compose | Apache License 2.0 |
| Coil Compose | Apache License 2.0 |
| CameraX | Apache License 2.0 |
| ML Kit Barcode Scanning / Text Recognition | Apache License 2.0 |
| Accompanist Permissions | Apache License 2.0 |
| Vico Charts | Apache License 2.0 |
| Kotlinx Serialization JSON | Apache License 2.0 |
| Health Connect Client | Apache License 2.0 |
| Guava | Apache License 2.0 |
| Timber | Apache License 2.0 |
| SQLCipher for Android | BSD 3-Clause License |

Full license details are available in-app under **Settings → License**.

## License

Copyright (C) 2026 Paul Bernhard Colin Witzke

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [GNU General Public License](LICENSE) for more details.
