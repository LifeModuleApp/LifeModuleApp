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

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the entire app.
 *
 * Every destination is a @Serializable data object (no args) or
 * data class (with nav args). Navigation 2.8+ serialises these into
 * the backstack automatically - no more string-template routes.
 *
 * Usage:
 *   navController.navigate(AppRoute.Nutrition)
 *   navController.navigate(AppRoute.GymSession(sessionId = 42L))
 *
 * In NavHost:
 *   composable<AppRoute.Nutrition> { NutritionScreen(...) }
 *   composable<AppRoute.GymSession> { backStackEntry ->
 *       val args = backStackEntry.toRoute<AppRoute.GymSession>()
 *       ActiveSessionScreen(sessionId = args.sessionId, ...)
 *   }
 */
sealed interface AppRoute {

    // ── App shell ──────────────────────────────────────────────────────────
    @Serializable data object Dashboard     : AppRoute
    @Serializable data object ModuleManager : AppRoute
    @Serializable data object Settings      : AppRoute
    @Serializable data object ErrorLog      : AppRoute
    @Serializable data object Analytics     : AppRoute

    // ── Nutrition ──────────────────────────────────────────────────────────
    @Serializable data object Nutrition              : AppRoute
    @Serializable data object NutritionAddEntry      : AppRoute
    @Serializable data object NutritionFoodDb        : AppRoute
    @Serializable data object NutritionAddFood       : AppRoute
    /** [barcode] is pre-filled into the new-food form. */
    @Serializable data class  NutritionAddFoodBarcode(val barcode: String) : AppRoute
    @Serializable data object NutritionScannerDb     : AppRoute
    @Serializable data object NutritionScannerEntry  : AppRoute

    // ── Supplements ────────────────────────────────────────────────────────
    @Serializable data object Supplements                           : AppRoute
    @Serializable data object SupplementsAdd                       : AppRoute
    @Serializable data class  SupplementDetail(val supplementId: String) : AppRoute

    // ── Habits ─────────────────────────────────────────────────────────────
    @Serializable data object Habits    : AppRoute
    @Serializable data object HabitsAdd : AppRoute

    // ── Mental Health ──────────────────────────────────────────────────────
    @Serializable data object MentalHealth    : AppRoute
    @Serializable data object MentalHealthAdd : AppRoute

    // ── Uni Schedule ───────────────────────────────────────────────────────
    @Serializable data object UniSchedule    : AppRoute
    @Serializable data object UniScheduleAdd : AppRoute

    // ── Calendar ───────────────────────────────────────────────────────────
    @Serializable data object Calendar    : AppRoute
    @Serializable data object CalendarAdd : AppRoute
    /** Pre-fill the date field when adding an event from the calendar day view. */
    @Serializable data class  CalendarAddForDate(val preselectedDate: String) : AppRoute

    // ── Gym ────────────────────────────────────────────────────────────────
    @Serializable data object Gym : AppRoute
    /** Workout template editor (create playlist) */
    @Serializable data object GymTemplateAdd : AppRoute
    /** Active workout session - exercises from a playlist */
    @Serializable data class  GymWorkoutSession(val sessionId: String, val workoutId: String) : AppRoute

    // ── Weight ─────────────────────────────────────────────────────────────
    @Serializable data object Weight    : AppRoute
    @Serializable data object WeightAdd : AppRoute

    // ── Shopping List ──────────────────────────────────────────────────────
    @Serializable data object Shopping : AppRoute

    // ── Work Time ─────────────────────────────────────────────────────────
    @Serializable data object WorkTime : AppRoute

    // ── Health Connect ─────────────────────────────────────────────────────
    @Serializable data object HealthConnect : AppRoute
    @Serializable data object HealthStepsDetail : AppRoute

    // ── Logbook (Fahrtenbuch, GoBD) ────────────────────────────────────────
    @Serializable data object Logbook : AppRoute
    @Serializable data object LogbookAdd : AppRoute
    @Serializable data object LogbookVehicles : AppRoute
    @Serializable data object LogbookAddVehicle : AppRoute

    // ── Receipt Scanner (GoBD) ─────────────────────────────────────────────
    @Serializable data object Scanner : AppRoute
    @Serializable data object ScannerCapture : AppRoute
    @Serializable data class  ScannerDetail(val receiptId: String) : AppRoute

    // ── Recipes ────────────────────────────────────────────────────────────
    @Serializable data object Recipes : AppRoute
    @Serializable data object RecipesAdd : AppRoute
    @Serializable data class  RecipeDetail(val recipeId: String) : AppRoute

    // ── Privacy ─────────────────────────────────────────────────────────
    @Serializable data object Privacy : AppRoute

    // ── About & Licenses ───────────────────────────────────────────────
    @Serializable data object About : AppRoute
    @Serializable data object Licenses : AppRoute

    // ── Debug (nur Debug-Builds) ───────────────────────────────────────────
    @Serializable data object Debug : AppRoute

    // ── Import ─────────────────────────────────────────────────────────────
    /** [packageUri] is the content URI of the ZIP file to import. */
    @Serializable data class Import(val packageUri: String) : AppRoute
}
