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

package de.lifemodule.app.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"

    // Nutrition
    const val NUTRITION = "nutrition"
    const val NUTRITION_ADD_ENTRY = "nutrition/add_entry"
    const val NUTRITION_FOOD_DB = "nutrition/food_db"
    const val NUTRITION_ADD_FOOD = "nutrition/add_food"
    const val NUTRITION_ADD_FOOD_BARCODE = "nutrition/add_food/{barcode}"
    const val NUTRITION_SCANNER_DB = "nutrition/scanner_db"
    const val NUTRITION_SCANNER_ENTRY = "nutrition/scanner_entry"

    fun addFoodWithBarcode(barcode: String) = "nutrition/add_food/$barcode"

    // Supplements
    const val SUPPLEMENTS = "supplements"
    const val SUPPLEMENTS_ADD = "supplements/add"
    const val SUPPLEMENTS_DETAIL = "supplements/detail/{supplementId}"

    fun supplementDetail(supplementId: String) = "supplements/detail/$supplementId"

    // Habits
    const val HABITS = "habits"
    const val HABITS_ADD = "habits/add"

    // Mental Health
    const val MENTAL_HEALTH = "mental_health"
    const val MENTAL_HEALTH_ADD = "mental_health/add"

    // Uni Schedule
    const val UNI_SCHEDULE = "uni_schedule"
    const val UNI_SCHEDULE_ADD = "uni_schedule/add"

    // Calendar
    const val CALENDAR = "calendar"
    const val CALENDAR_ADD = "calendar/add"

    // ── Gym (redesigned) ─────────────────────────────────────────────────
    /** Hub screen with tabs: Training ↔ Pläne */
    const val GYM = "gym"
    /** Start a new training session (choose type, exercises) */
    const val GYM_START = "gym/start"
    /** Active session - receives sessionId nav arg */
    const val GYM_SESSION = "gym/session/{sessionId}"
    /** Add a new workout template (plan) */
    const val GYM_TEMPLATE_ADD = "gym/template/add"
    /** Legacy alias so old navigation still compiles */
    const val GYM_ADD = GYM_TEMPLATE_ADD

    fun gymSession(sessionId: String) = "gym/session/$sessionId"

    // Analytics
    const val ANALYTICS = "analytics"

    // Weight Tracker
    const val WEIGHT = "weight"
    const val WEIGHT_ADD = "weight/add"

    // Shopping List
    const val SHOPPING = "shopping"

    // Work Time
    const val WORK_TIME = "work_time"

    // Health Connect
    const val HEALTH_CONNECT = "health_connect"

    // Logbook (Fahrtenbuch)
    const val LOGBOOK = "logbook"
    const val LOGBOOK_ADD = "logbook/add"
    const val LOGBOOK_VEHICLES = "logbook/vehicles"
    const val LOGBOOK_ADD_VEHICLE = "logbook/vehicles/add"

    // Receipt Scanner
    const val SCANNER = "scanner"
    const val SCANNER_CAPTURE = "scanner/capture"
    const val SCANNER_DETAIL = "scanner/detail/{receiptId}"

    fun scannerDetail(receiptId: String) = "scanner/detail/$receiptId"

    // Recipes
    const val RECIPES = "recipes"
    const val RECIPES_ADD = "recipes/add"
    const val RECIPES_DETAIL = "recipes/detail/{recipeId}"

    fun recipeDetail(recipeId: String) = "recipes/detail/$recipeId"

    // Module Manager
    const val MODULE_MANAGER = "module_manager"

    // Settings
    const val SETTINGS = "settings"
}
