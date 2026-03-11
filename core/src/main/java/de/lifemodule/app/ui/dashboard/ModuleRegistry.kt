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

package de.lifemodule.app.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.core.R

/**
 * How many grid columns a module tile spans, and its aspect ratio.
 * SMALL  -> 1×1  (square)
 * WIDE   -> 2×1  (wide rectangle)
 * LARGE  -> 2×2  (large square, 2 cols wide + 1:1 aspect ratio)
 */
enum class ModuleSize(val colSpan: Int, val label: String, val aspectRatio: Float) {
    SMALL(1, "1×1", 1f),
    WIDE(2, "2×1", 2f),
    LARGE(2, "2×2", 1f)
}

/** A module entry together with its persisted display size. */
data class ModuleTile(
    val module: AppModule,
    val size: ModuleSize = ModuleSize.SMALL
)

enum class ModuleCategory(@StringRes val displayNameRes: Int) {
    HEALTH(R.string.core_dashboard_gesundheit),
    FITNESS(R.string.core_dashboard_fitness),
    LIFESTYLE(R.string.core_dashboard_lifestyle),
    ORGANIZATION(R.string.core_dashboard_organisation),
    WORK(R.string.core_dashboard_arbeit),
    SYSTEM(R.string.core_dashboard_system)
}

enum class AppModule(
    val id: String,
    @StringRes val displayNameRes: Int,
    val emoji: String,
    val icon: ImageVector,
    val route: AppRoute,
    val category: ModuleCategory,
    val defaultEnabled: Boolean = true
) {
    NUTRITION(
        "nutrition", R.string.core_module_nutrition, "🍎",
        Icons.Default.Restaurant, AppRoute.Nutrition,
        ModuleCategory.HEALTH
    ),
    SUPPLEMENTS(
        "supplements", R.string.core_module_supplements, "💊",
        Icons.Default.Medication, AppRoute.Supplements,
        ModuleCategory.HEALTH
    ),
    HABITS(
        "habits", R.string.core_module_habits, "✅",
        Icons.Default.CheckBox, AppRoute.Habits,
        ModuleCategory.LIFESTYLE
    ),
    MENTAL_HEALTH(
        "mental_health", R.string.core_module_mental_health, "🧠",
        Icons.Default.Mood, AppRoute.MentalHealth,
        ModuleCategory.HEALTH
    ),
    GYM(
        "gym", R.string.core_module_gym, "🏋️",
        Icons.Default.FitnessCenter, AppRoute.Gym,
        ModuleCategory.FITNESS
    ),
    WEIGHT(
        "weight", R.string.core_module_weight, "⚖️",
        Icons.Default.MonitorWeight, AppRoute.Weight,
        ModuleCategory.FITNESS
    ),
    CALENDAR(
        "calendar", R.string.core_module_calendar, "📅",
        Icons.Default.CalendarMonth, AppRoute.Calendar,
        ModuleCategory.ORGANIZATION
    ),
    UNI_SCHEDULE(
        "uni_schedule", R.string.core_module_schedule, "🎓",
        Icons.Default.School, AppRoute.UniSchedule,
        ModuleCategory.ORGANIZATION, defaultEnabled = false
    ),
    WORK_TIME(
        "work_time", R.string.core_module_work_time, "🕐",
        Icons.Default.Timer, AppRoute.WorkTime,
        ModuleCategory.WORK, defaultEnabled = false
    ),
    SHOPPING(
        "shopping", R.string.core_module_shopping, "🛒",
        Icons.Default.ShoppingCart, AppRoute.Shopping,
        ModuleCategory.LIFESTYLE, defaultEnabled = false
    ),
    ANALYTICS(
        "analytics", R.string.core_module_analytics, "📊",
        Icons.AutoMirrored.Filled.ShowChart, AppRoute.Analytics,
        ModuleCategory.SYSTEM
    ),
    HEALTH_CONNECT(
        "health_connect", R.string.core_module_health_connect, "💚",
        Icons.Default.Favorite, AppRoute.HealthConnect,
        ModuleCategory.HEALTH, defaultEnabled = false
    ),
    LOGBOOK(
        "logbook", R.string.core_module_logbook, "🚗",
        Icons.Default.DirectionsCar, AppRoute.Logbook,
        ModuleCategory.WORK, defaultEnabled = false
    ),
    SCANNER(
        "scanner", R.string.core_module_scanner, "🧾",
        Icons.Default.CameraAlt, AppRoute.Scanner,
        ModuleCategory.WORK, defaultEnabled = false
    ),
    RECIPES(
        "recipes", R.string.core_module_recipes, "📖",
        Icons.Default.MenuBook, AppRoute.Recipes,
        ModuleCategory.LIFESTYLE, defaultEnabled = false
    )
}
