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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import de.lifemodule.app.ui.analytics.AnalyticsScreen
import de.lifemodule.app.ui.calendar.AddEventScreen
import de.lifemodule.app.ui.calendar.CalendarScreen
import de.lifemodule.app.ui.dashboard.DashboardScreen
import de.lifemodule.app.ui.dashboard.ModuleManagerScreen
import de.lifemodule.app.ui.dashboard.ModulePreferences
import de.lifemodule.app.ui.debug.DebugScreen
import de.lifemodule.app.ui.gym.AddWorkoutScreen
import de.lifemodule.app.ui.gym.GymScreen
import de.lifemodule.app.ui.gym.WorkoutSessionScreen
import de.lifemodule.app.ui.health.HealthScreen
import de.lifemodule.app.ui.health.StepCounterDetailScreen
import de.lifemodule.app.ui.legal.PrivacyInfoScreen
import de.lifemodule.app.ui.legal.AboutScreen
import de.lifemodule.app.ui.legal.LicenseScreen
import de.lifemodule.app.ui.habits.AddHabitScreen
import de.lifemodule.app.ui.habits.HabitsScreen
import de.lifemodule.app.ui.logbook.AddLogbookEntryScreen
import de.lifemodule.app.ui.logbook.AddVehicleScreen
import de.lifemodule.app.ui.logbook.LogbookScreen
import de.lifemodule.app.ui.logbook.VehicleManagementScreen
import de.lifemodule.app.ui.mentalhealth.AddMoodScreen
import de.lifemodule.app.ui.mentalhealth.MentalHealthScreen
import de.lifemodule.app.ui.nutrition.AddFoodEntryScreen
import de.lifemodule.app.ui.nutrition.AddFoodItemScreen
import de.lifemodule.app.ui.nutrition.BarcodeScannerScreen
import de.lifemodule.app.ui.nutrition.FoodDatabaseScreen
import de.lifemodule.app.ui.nutrition.NutritionScreen
import de.lifemodule.app.ui.nutrition.NutritionViewModel
import de.lifemodule.app.ui.recipes.AddRecipeScreen
import de.lifemodule.app.ui.recipes.RecipeDetailScreen
import de.lifemodule.app.ui.recipes.RecipesScreen
import de.lifemodule.app.ui.scanner.ReceiptDetailScreen
import de.lifemodule.app.ui.scanner.ScannerCaptureScreen
import de.lifemodule.app.ui.scanner.ScannerScreen
import de.lifemodule.app.ui.schedule.AddCourseScreen
import de.lifemodule.app.ui.schedule.ScheduleScreen
import de.lifemodule.app.ui.settings.ErrorLogScreen
import de.lifemodule.app.ui.settings.ImportScreen
import de.lifemodule.app.ui.settings.ImportViewModel
import de.lifemodule.app.ui.settings.SettingsScreen
import de.lifemodule.app.ui.shopping.ShoppingScreen
import de.lifemodule.app.ui.supplements.AddSupplementScreen
import de.lifemodule.app.ui.supplements.SupplementDetailScreen
import de.lifemodule.app.ui.supplements.SupplementsScreen
import de.lifemodule.app.ui.weight.AddWeightScreen
import de.lifemodule.app.ui.weight.WeightScreen
import de.lifemodule.app.ui.worktime.WorkTimeScreen
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController,
    modulePreferences: ModulePreferences,
    timeProvider: TimeProvider
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Dashboard
    ) {
        composable<AppRoute.Dashboard> {
            DashboardScreen(navController = navController)
        }

        // ── Module Manager ──
        composable<AppRoute.ModuleManager> {
            ModuleManagerScreen(
                navController = navController,
                modulePreferences = modulePreferences
            )
        }

        // ── Nutrition ──
        composable<AppRoute.Nutrition> {
            NutritionScreen(navController = navController)
        }
        composable<AppRoute.NutritionAddEntry> {
            AddFoodEntryScreen(navController = navController)
        }
        composable<AppRoute.NutritionFoodDb> {
            FoodDatabaseScreen(navController = navController)
        }
        composable<AppRoute.NutritionAddFood> {
            AddFoodItemScreen(navController = navController)
        }

        // Add food with pre-filled barcode
        composable<AppRoute.NutritionAddFoodBarcode> { backStackEntry ->
            val args = backStackEntry.toRoute<AppRoute.NutritionAddFoodBarcode>()
            AddFoodItemScreen(
                navController = navController,
                initialBarcode = args.barcode
            )
        }

        // Scanner from Food DB screen
        composable<AppRoute.NutritionScannerDb> {
            BarcodeScannerScreen(
                onBarcodeScanned = { barcode ->
                    navController.popBackStack()
                    navController.navigate(AppRoute.NutritionAddFoodBarcode(barcode))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Scanner from AddEntry
        composable<AppRoute.NutritionScannerEntry> {
            val parentEntry = navController.previousBackStackEntry
            val viewModel: NutritionViewModel = if (parentEntry != null) {
                hiltViewModel(parentEntry)
            } else {
                hiltViewModel()
            }
            val scope = rememberCoroutineScope()

            BarcodeScannerScreen(
                onBarcodeScanned = { barcode ->
                    scope.launch {
                        val food = viewModel.findByBarcode(barcode)
                        if (food != null) {
                            navController.popBackStack()
                        } else {
                            navController.popBackStack()
                            navController.navigate(AppRoute.NutritionAddFoodBarcode(barcode))
                        }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Supplements ──
        composable<AppRoute.Supplements> {
            SupplementsScreen(navController = navController)
        }
        composable<AppRoute.SupplementsAdd> {
            AddSupplementScreen(navController = navController)
        }
        composable<AppRoute.SupplementDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<AppRoute.SupplementDetail>()
            SupplementDetailScreen(
                navController = navController,
                supplementId = args.supplementId
            )
        }

        // ── Habits ──
        composable<AppRoute.Habits> {
            HabitsScreen(navController = navController)
        }
        composable<AppRoute.HabitsAdd> {
            AddHabitScreen(navController = navController)
        }

        // ── Mental Health ──
        composable<AppRoute.MentalHealth> {
            MentalHealthScreen(navController = navController)
        }
        composable<AppRoute.MentalHealthAdd> {
            AddMoodScreen(navController = navController)
        }

        // ── Uni Schedule ──
        composable<AppRoute.UniSchedule> {
            ScheduleScreen(navController = navController)
        }
        composable<AppRoute.UniScheduleAdd> {
            AddCourseScreen(navController = navController)
        }

        // ── Calendar ──
        composable<AppRoute.Calendar> {
            CalendarScreen(navController = navController)
        }
        composable<AppRoute.CalendarAdd> {
            AddEventScreen(navController = navController)
        }
        composable<AppRoute.CalendarAddForDate> { backStackEntry ->
            val args = backStackEntry.toRoute<AppRoute.CalendarAddForDate>()
            AddEventScreen(
                navController = navController,
                preselectedDate = args.preselectedDate
            )
        }

        // ── Gym ──
        composable<AppRoute.Gym> {
            GymScreen(navController = navController)
        }
        // Workout template editor (create playlist)
        composable<AppRoute.GymTemplateAdd> {
            AddWorkoutScreen(navController = navController)
        }
        // Active workout session - exercises from a playlist
        composable<AppRoute.GymWorkoutSession> { backStackEntry ->
            val args = backStackEntry.toRoute<AppRoute.GymWorkoutSession>()
            WorkoutSessionScreen(
                navController = navController,
                workoutId = args.workoutId
            )
        }

        // ── Weight Tracker ──
        composable<AppRoute.Weight> {
            WeightScreen(navController = navController)
        }
        composable<AppRoute.WeightAdd> {
            AddWeightScreen(navController = navController)
        }

        // ── Shopping List ──
        composable<AppRoute.Shopping> {
            ShoppingScreen(navController = navController)
        }

        // ── Work Time ──
        composable<AppRoute.WorkTime> {
            WorkTimeScreen(navController = navController)
        }

        // ── Analytics ──
        composable<AppRoute.Analytics> {
            AnalyticsScreen(navController = navController)
        }

        // ── Settings ──
        composable<AppRoute.Settings> {
            SettingsScreen(navController = navController)
        }
        composable<AppRoute.ErrorLog> {
            ErrorLogScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── Health Connect ──
        composable<AppRoute.HealthConnect> {
            HealthScreen(navController = navController)
        }
        composable<AppRoute.HealthStepsDetail> {
            StepCounterDetailScreen(navController = navController)
        }

        // ── Logbook (Fahrtenbuch) ──
        composable<AppRoute.Logbook> {
            LogbookScreen(navController = navController)
        }
        composable<AppRoute.LogbookAdd> {
            AddLogbookEntryScreen(navController = navController)
        }
        composable<AppRoute.LogbookVehicles> {
            VehicleManagementScreen(navController = navController)
        }
        composable<AppRoute.LogbookAddVehicle> {
            AddVehicleScreen(navController = navController)
        }

        // ── Receipt Scanner ──
        composable<AppRoute.Scanner> {
            ScannerScreen(navController = navController)
        }
        composable<AppRoute.ScannerCapture> {
            ScannerCaptureScreen(navController = navController)
        }
        composable<AppRoute.ScannerDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<AppRoute.ScannerDetail>()
            ReceiptDetailScreen(
                receiptId = args.receiptId,
                navController = navController
            )
        }

        // ── Recipes ──
        composable<AppRoute.Recipes> {
            RecipesScreen(navController = navController)
        }
        composable<AppRoute.RecipesAdd> {
            AddRecipeScreen(navController = navController)
        }
        composable<AppRoute.RecipeDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<AppRoute.RecipeDetail>()
            RecipeDetailScreen(
                recipeId = args.recipeId,
                navController = navController
            )
        }

        // ── Privacy ──
        composable<AppRoute.Privacy> {
            PrivacyInfoScreen(navController = navController)
        }

        // ── About & Licenses ──
        composable<AppRoute.About> {
            AboutScreen(navController = navController)
        }
        composable<AppRoute.Licenses> {
            LicenseScreen(navController = navController)
        }

        // ── Debug (nur Debug-Builds) ──
        composable<AppRoute.Debug> {
            DebugScreen(navController = navController, timeProvider = timeProvider)
        }

        // ── Import Package ──
        composable<AppRoute.Import> { backStackEntry ->
            val args = backStackEntry.toRoute<AppRoute.Import>()
            val viewModel: ImportViewModel = hiltViewModel()
            val uri = android.net.Uri.parse(args.packageUri)
            ImportScreen(
                viewModel = viewModel,
                packageUri = uri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
