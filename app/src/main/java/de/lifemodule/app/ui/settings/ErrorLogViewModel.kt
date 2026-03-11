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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.lifemodule.app.data.error.ErrorLogEntity
import de.lifemodule.app.data.error.ErrorLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ErrorLogViewModel @Inject constructor(
    private val errorLogger: ErrorLogger,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val errorLogs: StateFlow<List<ErrorLogEntity>> = errorLogger.errorLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearLogs() {
        viewModelScope.launch {
            try {
                errorLogger.clearAllLogs()
                Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Timber.e(e, "[ErrorLog] Failed to clear logs")
            }
        }
    }

    fun copyLogToClipboard(log: ErrorLogEntity) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val textToCopy = """
            Module: ${log.module}
            Message: ${log.message}
            Time: ${java.util.Date(log.timestamp)}
            Stacktrace: ${log.stackTrace ?: "N/A"}
        """.trimIndent()
        val clip = ClipData.newPlainText("Error Log", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
