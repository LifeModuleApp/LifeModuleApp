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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.lifemodule.app.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import de.lifemodule.app.data.LifeModuleDatabase

@Singleton
class ModulePreferences @Inject constructor(
    @Named("modules") private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
    private val db: LifeModuleDatabase
) {
    companion object {
        private val KEY_ORDER   = stringPreferencesKey("module_order")
        private val KEY_COLUMNS = intPreferencesKey("dashboard_columns")
        private fun enabledKey(id: String) = booleanPreferencesKey("module_enabled_$id")
        private fun sizeKey(id: String)    = stringPreferencesKey("module_size_$id")
    }

    // ── Internal raw-prefs StateFlow - one subscription to DataStore ────────
    private val currentPrefs: StateFlow<Preferences> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .stateIn(scope, SharingStarted.Eagerly, emptyPreferences())

    // ── Public reactive StateFlows - consumed by DashboardViewModel ────────

    val columnCountFlow: StateFlow<Int> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[KEY_COLUMNS] ?: 2 }
        .stateIn(scope, SharingStarted.Eagerly, 2)

    /** Eagerly emits the full ordered+sized tile list. Always up to date. */
    val tilesFlow: StateFlow<List<ModuleTile>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> buildTiles(prefs) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ── Sync convenience wrappers (safe after Eagerly start) ───────────────

    fun getColumnCount(): Int = columnCountFlow.value

    fun isModuleEnabled(module: AppModule): Boolean =
        currentPrefs.value[enabledKey(module.id)] ?: module.defaultEnabled

    fun getEnabledModules(): List<AppModule> = tilesFlow.value.map { it.module }

    fun getModuleOrder(): List<AppModule> {
        val prefs = currentPrefs.value
        val savedIds = prefs[KEY_ORDER]?.split(",") ?: return AppModule.entries.toList()
        val ordered  = savedIds.mapNotNull { id -> AppModule.entries.find { it.id == id } }
        val remaining = AppModule.entries.filter { it !in ordered }
        return ordered + remaining
    }

    fun getModuleSize(module: AppModule): ModuleSize {
        val name = currentPrefs.value[sizeKey(module.id)]
        return ModuleSize.entries.find { it.name == name } ?: ModuleSize.SMALL
    }

    fun getEnabledTiles(): List<ModuleTile> = tilesFlow.value

    // ── Write operations (fire-and-forget) ───────────────────────────────

    fun setColumnCount(count: Int) {
        scope.launch { dataStore.edit { it[KEY_COLUMNS] = count.coerceIn(2, 4) } }
    }

    fun setModuleEnabled(module: AppModule, enabled: Boolean) {
        scope.launch { dataStore.edit { it[enabledKey(module.id)] = enabled } }
    }

    fun saveModuleOrder(modules: List<AppModule>) {
        scope.launch {
            dataStore.edit { it[KEY_ORDER] = modules.joinToString(",") { m -> m.id } }
        }
    }

    fun setModuleSize(module: AppModule, size: ModuleSize) {
        scope.launch { dataStore.edit { it[sizeKey(module.id)] = size.name } }
    }

    suspend fun wipeModuleData(module: AppModule) = withContext(Dispatchers.IO) {
        db.runInTransaction {
            val wdb = db.openHelper.writableDatabase
            when (module) {
                AppModule.NUTRITION -> {
                    wdb.execSQL("DELETE FROM food_items")
                    wdb.execSQL("DELETE FROM daily_food_entries")
                }
                AppModule.SUPPLEMENTS -> {
                    wdb.execSQL("DELETE FROM supplements")
                    wdb.execSQL("DELETE FROM supplement_ingredients")
                    wdb.execSQL("DELETE FROM supplement_log")
                }
                AppModule.HABITS -> {
                    wdb.execSQL("DELETE FROM habits")
                    wdb.execSQL("DELETE FROM habit_log")
                }
                AppModule.MENTAL_HEALTH -> wdb.execSQL("DELETE FROM mood_entries")
                AppModule.GYM -> {
                    wdb.execSQL("DELETE FROM gym_sessions")
                    wdb.execSQL("DELETE FROM session_sets")
                }
                AppModule.WEIGHT -> wdb.execSQL("DELETE FROM weight_entries")
                AppModule.CALENDAR -> wdb.execSQL("DELETE FROM calendar_events")
                AppModule.UNI_SCHEDULE -> wdb.execSQL("DELETE FROM courses")
                AppModule.WORK_TIME -> wdb.execSQL("DELETE FROM work_time_entries")
                AppModule.SHOPPING -> wdb.execSQL("DELETE FROM shopping_items")
                else -> {} // Analytics / Health Connect have no persistent local tables
            }
        }
    }

    // ── Private builder ─────────────────────────────────────────────────

    private fun buildTiles(prefs: Preferences): List<ModuleTile> {
        // Restore saved order (new modules appended at end)
        val savedIds = prefs[KEY_ORDER]?.split(",") ?: emptyList()
        val ordered  = savedIds.mapNotNull { id -> AppModule.entries.find { it.id == id } }
        val remaining = AppModule.entries.filter { it !in ordered }
        val fullOrder = ordered + remaining

        return fullOrder
            .filter { module ->
                prefs[enabledKey(module.id)] ?: module.defaultEnabled
            }
            .map { module ->
                val sizeName = prefs[sizeKey(module.id)]
                val size = ModuleSize.entries.find { it.name == sizeName } ?: ModuleSize.SMALL
                ModuleTile(module, size)
            }
    }
}
