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

package de.lifemodule.app.ui.scanner

import de.lifemodule.app.data.scanner.ReceiptDao
import de.lifemodule.app.data.scanner.ReceiptRecordEntity
import de.lifemodule.app.data.scanner.SpendingByCategorySummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerRepository @Inject constructor(
    private val receiptDao: ReceiptDao
) {
    fun getAllRecords(): Flow<List<ReceiptRecordEntity>> = receiptDao.getAllRecords()

    fun getUnfinalizedRecords(): Flow<List<ReceiptRecordEntity>> = receiptDao.getUnfinalizedRecords()

    fun getRecordsInRange(startMillis: Long, endMillis: Long): Flow<List<ReceiptRecordEntity>> =
        receiptDao.getRecordsInRange(startMillis, endMillis)

    suspend fun getRecordByUuid(uuid: String): ReceiptRecordEntity? = receiptDao.getRecordByUuid(uuid)

    suspend fun insertRecord(record: ReceiptRecordEntity): Long = receiptDao.insertRecord(record)

    suspend fun updateRecord(record: ReceiptRecordEntity) = receiptDao.updateRecord(record)

    suspend fun getSpendingByCategory(startMillis: Long, endMillis: Long): List<SpendingByCategorySummary> =
        receiptDao.getSpendingByCategory(startMillis, endMillis)
}
