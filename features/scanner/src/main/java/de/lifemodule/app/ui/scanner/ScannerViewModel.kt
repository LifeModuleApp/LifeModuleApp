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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.lifemodule.app.data.BaseEntity
import de.lifemodule.app.data.scanner.ReceiptCategory
import de.lifemodule.app.data.scanner.ReceiptRecordEntity
import de.lifemodule.app.util.HashChainUtil
import de.lifemodule.app.util.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ScannerRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    val records: StateFlow<List<ReceiptRecordEntity>> =
        repository.getAllRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unfinalizedRecords: StateFlow<List<ReceiptRecordEntity>> =
        repository.getUnfinalizedRecords()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()

    fun clearSaveState() { _saveSuccess.value = null }

    /**
     * Create a receipt record from a captured image + OCR results.
     */
    fun createReceipt(
        vendor: String,
        receiptDate: Long = timeProvider.currentTimeMillis(),
        totalAmount: Double,
        vatAmount: Double?,
        currency: String,
        category: ReceiptCategory,
        notes: String?,
        imagePath: String,
        imageSha256: String
    ) {
        viewModelScope.launch {
            try {
                val now = timeProvider.now()
                val epochMillis = now.toInstant(ZoneOffset.UTC).toEpochMilli()

                repository.insertRecord(
                    ReceiptRecordEntity(
                        createdAt = epochMillis,
                        updatedAt = epochMillis,
                        capturedAt = epochMillis,
                        vendor = vendor,
                        receiptDate = receiptDate,
                        totalAmount = totalAmount,
                        vatAmount = vatAmount,
                        currency = currency,
                        category = category,
                        notes = notes,
                        imagePath = imagePath,
                        imageSha256 = imageSha256
                    )
                )
                _saveSuccess.value = true
                Timber.i("[Scanner] Receipt created: %s, %.2f %s", vendor, totalAmount, currency)
            } catch (e: Exception) {
                Timber.e(e, "[Scanner] Failed to insert receipt record")
                _saveSuccess.value = false
            }
        }
    }

    /**
     * Update a non-finalized receipt (e.g. correct OCR mistakes).
     */
    fun updateReceipt(record: ReceiptRecordEntity) {
        viewModelScope.launch {
            try {
                if (record.isFinalized) {
                    Timber.w("[Scanner] Attempted update on finalized record %s - ignored", record.uuid)
                    return@launch
                }
                val now = timeProvider.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                repository.updateRecord(record.copy(updatedAt = now))
                Timber.i("[Scanner] Receipt %s updated", record.uuid)
            } catch (e: Exception) {
                Timber.e(e, "[Scanner] Failed to update receipt %s", record.uuid)
            }
        }
    }

    /**
     * Finalize a receipt - compute finalization hash, seal the record.
     */
    fun finalizeReceipt(uuid: String) {
        viewModelScope.launch {
            try {
                val record = repository.getRecordByUuid(uuid) ?: run {
                    Timber.e("[Scanner] Cannot finalize - record %s not found", uuid)
                    return@launch
                }
                if (record.isFinalized) {
                    Timber.w("[Scanner] Record %s already finalized", uuid)
                    return@launch
                }

                val hash = HashChainUtil.computeReceiptFinalizationHash(
                    uuid = record.uuid,
                    capturedAt = record.capturedAt,
                    vendor = record.vendor,
                    receiptDate = record.receiptDate,
                    totalAmount = record.totalAmount,
                    vatAmount = record.vatAmount,
                    currency = record.currency,
                    category = record.category.name,
                    notes = record.notes,
                    imageSha256 = record.imageSha256,
                    createdAt = record.createdAt
                )

                val now = timeProvider.now().toInstant(ZoneOffset.UTC).toEpochMilli()
                repository.updateRecord(
                    record.copy(
                        isFinalized = true,
                        finalizationHash = hash,
                        updatedAt = now
                    )
                )
                Timber.i("[Scanner] Receipt %s finalized with hash %s", uuid, hash.take(16))
            } catch (e: Exception) {
                Timber.e(e, "[Scanner] Failed to finalize receipt %s", uuid)
            }
        }
    }
}
