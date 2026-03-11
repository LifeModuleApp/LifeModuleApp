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

package de.lifemodule.app.data.error

import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorLogger @Inject constructor(
    private val errorLogDao: ErrorLogDao
) {
    val errorLogs: Flow<List<ErrorLogEntity>> = errorLogDao.getAllErrorLogs()

    suspend fun logError(module: String, message: String, exception: Throwable? = null, severity: String = "ERROR") {
        // Also print to Timber as a fallback
        if (severity == "WARNING") {
            Timber.tag(module).w(exception, message)
        } else {
            Timber.tag(module).e(exception, message)
        }
        
        val stackTrace = exception?.stackTraceToString()
        val logEntry = ErrorLogEntity(
            module = module,
            severity = severity,
            message = message,
            stackTrace = stackTrace
        )
        try {
            errorLogDao.insertErrorLog(logEntry)
            
            // Auto-Cleanup: Delete logs older than 30 days
            val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
            errorLogDao.deleteOldLogs(System.currentTimeMillis() - thirtyDaysMillis)
        } catch (e: Exception) {
            // Failsafe in case DB insertion fails
            Timber.e(e, "ErrorLogger: Failed to insert error log into DB")
        }
    }

    suspend fun clearAllLogs() {
        errorLogDao.deleteAllErrorLogs()
    }
}
