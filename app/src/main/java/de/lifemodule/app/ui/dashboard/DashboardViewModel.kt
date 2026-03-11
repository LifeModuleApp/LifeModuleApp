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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val prefs: ModulePreferences
) : ViewModel() {

    // ── Tiles - backed by DataStore flow, updated automatically ──────────────
    //
    // Initialised with whatever DataStore has emitted so far (may be emptyList()
    // on very first cold start). The init{} collector keeps it in sync.
    private val _tiles = MutableStateFlow(prefs.tilesFlow.value)
    val tiles: StateFlow<List<ModuleTile>> = _tiles.asStateFlow()

    private val _columnCount = MutableStateFlow(prefs.columnCountFlow.value)
    val columnCount: StateFlow<Int> = _columnCount.asStateFlow()

    init {
        viewModelScope.launch {
            try { prefs.tilesFlow.collect { _tiles.value = it } }
            catch (e: Exception) { Timber.e(e, "[Dashboard] Failed to collect tiles") }
        }
        viewModelScope.launch {
            try { prefs.columnCountFlow.collect { _columnCount.value = it } }
            catch (e: Exception) { Timber.e(e, "[Dashboard] Failed to collect column count") }
        }
    }

    /**
     * No-op - DataStore flows auto-update when any write happens.
     * Kept for source-compatibility with the DisposableEffect in DashboardScreen.
     */
    fun refresh() { /* DataStore -> tilesFlow -> _tiles is always live */ }

    /** Cycles the column count between 2 -> 3 -> 4 -> 2. */
    fun cycleColumnCount() {
        val next = if (_columnCount.value >= 4) 2 else _columnCount.value + 1
        _columnCount.value = next   // immediate UI update
        prefs.setColumnCount(next)  // async persist
    }

    /**
     * Commits the final drag order. Called once when a drag gesture ends.
     * [orderedTiles] is the fully re-ordered tile list from the local drag state.
     */
    fun applyDragOrder(orderedTiles: List<ModuleTile>) {
        _tiles.value = orderedTiles // immediate UI update
        val allModules = orderedTiles.map { it.module } +
            AppModule.entries.filter { m -> orderedTiles.none { it.module == m } }
        prefs.saveModuleOrder(allModules) // async persist
    }

    /** Changes the display size of a single tile and persists it. */
    fun setTileSize(module: AppModule, size: ModuleSize) {
        prefs.setModuleSize(module, size) // async persist
        _tiles.value = _tiles.value.map {
            if (it.module == module) it.copy(size = size) else it
        }
    }
}
