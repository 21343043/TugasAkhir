package com.fadhil.financereceipt.ui.history

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory
import com.fadhil.financereceipt.data.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fadhil.financereceipt.receipt.ReceiptImageStore

data class HistoryUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val errorMessage: String? = null
)

data class TransactionManageState(
    val isWorking: Boolean = false,
    val completedMessage: String? = null,
    val errorMessage: String? = null
)

data class ReceiptPhotoUiState(
    val transactionId: Long? = null,
    val isLoading: Boolean = false,
    val bitmap: Bitmap? = null,
    val errorMessage: String? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FinanceDatabase.getInstance(application)
    private val repository = TransactionRepository(database)
    private val _manageState = MutableStateFlow(TransactionManageState())
    val manageState = _manageState.asStateFlow()
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()
    private var historyJob: Job? = null
    private var receiptPhotoJob: Job? = null
    private val _receiptPhotoState = MutableStateFlow(ReceiptPhotoUiState())
    val receiptPhotoState = _receiptPhotoState.asStateFlow()

    init { loadHistory() }

    fun loadReceiptPhoto(transactionId: Long) {
        receiptPhotoJob?.cancel()
        _receiptPhotoState.value = ReceiptPhotoUiState(transactionId, isLoading = true)
        receiptPhotoJob = viewModelScope.launch {
            try {
                val receipt = database.receiptExtractionDao().getByTransactionId(transactionId)
                val bitmap = receipt?.let {
                    withContext(Dispatchers.IO) {
                        ReceiptImageStore(getApplication()).readPersistedBitmap(it.imagePath)
                    }
                }
                _receiptPhotoState.value = ReceiptPhotoUiState(
                    transactionId = transactionId,
                    bitmap = bitmap,
                    errorMessage = if (bitmap == null) "Foto struk tidak tersedia." else null
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("HistoryViewModel", "Gagal membuka foto struk", error)
                _receiptPhotoState.value = ReceiptPhotoUiState(
                    transactionId = transactionId,
                    errorMessage = "Foto struk gagal dimuat. Silakan coba lagi."
                )
            }
        }
    }

    fun closeReceiptPhoto() {
        receiptPhotoJob?.cancel()
        receiptPhotoJob = null
        // Lepaskan referensi; jangan recycle bitmap yang mungkin masih dipakai renderer.
        _receiptPhotoState.value = ReceiptPhotoUiState()
    }

    fun loadHistory() {
        if (historyJob?.isActive == true) return
        historyJob = viewModelScope.launch {
            _uiState.value = HistoryUiState()
            try {
                combine(repository.observeHistory(), database.categoryDao().observeAll()) { rows, categories ->
                    HistoryUiState(isLoading = false, transactions = rows, categories = categories)
                }.collect { _uiState.value = it }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("HistoryViewModel", "Gagal membaca riwayat", error)
                _uiState.value = HistoryUiState(
                    isLoading = false,
                    errorMessage = "Riwayat gagal dimuat. Silakan coba lagi."
                )
            }
        }
    }

    fun updateTransaction(
        transactionId: Long, categoryId: Long?, transactionType: String,
        amountText: String, transactionDate: Long, note: String
    ) {
        if (_manageState.value.isWorking || _manageState.value.completedMessage != null) return
        val amount = amountText.toLongOrNull()
        if (categoryId == null || amount == null || amount <= 0L) {
            _manageState.value = TransactionManageState(errorMessage =
                if (categoryId == null) "Pilih kategori terlebih dahulu."
                else "Masukkan nominal yang valid dan lebih dari nol.")
            return
        }
        performChange("Transaksi berhasil diperbarui.") {
            repository.updateTransaction(
                transactionId, categoryId, transactionType, amount, transactionDate, note
            )
        }
    }

    fun deleteTransaction(transactionId: Long) {
        performChange("Transaksi berhasil dihapus.") {
            val imagePath = database.receiptExtractionDao().getByTransactionId(transactionId)?.imagePath
            repository.deleteTransaction(transactionId)
            // Relasi ekstraksi dihapus CASCADE; hapus berkas hanya setelah DB berhasil commit.
            if (imagePath != null) withContext(Dispatchers.IO) {
                runCatching { ReceiptImageStore(getApplication()).deletePersisted(imagePath) }
                Unit
            }
        }
    }

    fun resetManageState() {
        if (!_manageState.value.isWorking) _manageState.value = TransactionManageState()
    }

    private fun performChange(message: String, operation: suspend () -> Unit) {
        if (_manageState.value.isWorking || _manageState.value.completedMessage != null) return
        // Kunci sebelum meluncurkan coroutine agar ketukan berulang tidak diproses.
        _manageState.value = TransactionManageState(isWorking = true)
        viewModelScope.launch {
            try {
                operation()
                _manageState.value = TransactionManageState(completedMessage = message)
            } catch (error: CancellationException) {
                _manageState.value = TransactionManageState()
                throw error
            } catch (error: Exception) {
                Log.e("HistoryViewModel", "Gagal mengubah transaksi", error)
                _manageState.value = TransactionManageState(errorMessage =
                    if (error is IllegalArgumentException) error.message ?: "Isian tidak valid."
                    else "Perubahan gagal disimpan. Silakan coba lagi.")
            }
        }
    }

}
