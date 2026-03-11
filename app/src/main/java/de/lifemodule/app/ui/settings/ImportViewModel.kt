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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lifemodule.app.data.packages.ImportPackageUseCase
import de.lifemodule.app.data.packages.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Manages the state of a package import operation.
 *
 * States:
 * - [ImportState.Idle]       - waiting for user to trigger import
 * - [ImportState.Importing]  - extraction + merge in progress
 * - [ImportState.Done]       - result available
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importPackageUseCase: ImportPackageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state = _state.asStateFlow()

    /**
     * Starts importing the package at [uri].
     * Does nothing if an import is already in progress.
     */
    fun importPackage(uri: Uri) {
        if (_state.value is ImportState.Importing) return

        _state.value = ImportState.Importing

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = importPackageUseCase.invoke(uri)
                _state.value = ImportState.Done(result)
            } catch (e: Exception) {
                Timber.e(e, "[Import] Unexpected error during import")
                _state.value = ImportState.Done(
                    ImportResult(
                        packageId = "",
                        inserted = 0,
                        skipped = 0,
                        failed = 0,
                        errors = listOf(e.message ?: "Unknown error")
                    )
                )
            }
        }
    }

    /** Resets state to [ImportState.Idle]. */
    fun reset() {
        _state.value = ImportState.Idle
    }
}

sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Done(val result: ImportResult) : ImportState
}
