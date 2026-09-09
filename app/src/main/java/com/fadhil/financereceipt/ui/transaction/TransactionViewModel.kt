package com.fadhil.financereceipt.ui.transaction

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.fadhil.financereceipt.data.repository.TransactionRepository

data class TransactionCategoryUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val errorMessage: String? = null
)

data class TransactionSaveUiState(
    val isSaving: Boolean = false,
    val savedTransactionId: Long? = null,
    val errorMessage: String? = null
)
class TransactionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = CategoryRepository(
        FinanceDatabase.getInstance(application)
    )

    private val _categoryState =
        MutableStateFlow(TransactionCategoryUiState())

    val categoryState = _categoryState.asStateFlow()

    private var categoryJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        if (categoryJob?.isActive == true) return

        categoryJob = viewModelScope.launch {
            _categoryState.value = TransactionCategoryUiState()

            try {
                repository.initializeDefaultCategories()

                repository.observeAll().collect { categories ->
                    _categoryState.value = TransactionCategoryUiState(
                        isLoading = false,
                        categories = categories
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(
                    "TransactionViewModel",
                    "Gagal memuat kategori",
                    error
                )

                _categoryState.value = TransactionCategoryUiState(
                    isLoading = false,
                    errorMessage = "Kategori gagal dimuat. Silakan coba lagi."
                )
            }
        }
    }
    private val transactionRepository = TransactionRepository(
        FinanceDatabase.getInstance(getApplication<Application>())
    )

    private val _saveState = MutableStateFlow(TransactionSaveUiState())
    val saveState = _saveState.asStateFlow()

    fun saveTransaction(
        categoryId: Long?,
        transactionType: String,
        amountText: String,
        transactionDate: Long,
        note: String
    ) {
        // Cegah pengiriman ulang selama menyimpan atau setelah berhasil.
        if (
            _saveState.value.isSaving ||
            _saveState.value.savedTransactionId != null
        ) {
            return
        }

        val amount = amountText.toLongOrNull()

        if (categoryId == null) {
            _saveState.value = TransactionSaveUiState(
                errorMessage = "Pilih kategori terlebih dahulu."
            )
            return
        }

        if (amount == null || amount <= 0L) {
            _saveState.value = TransactionSaveUiState(
                errorMessage = "Nominal harus lebih dari nol."
            )
            return
        }

        _saveState.value = TransactionSaveUiState(isSaving = true)

        viewModelScope.launch {
            try {
                val transactionId = transactionRepository.saveManualTransaction(
                    categoryId = categoryId,
                    transactionType = transactionType,
                    amount = amount,
                    transactionDate = transactionDate,
                    note = note
                )

                _saveState.value = TransactionSaveUiState(
                    savedTransactionId = transactionId
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(
                    "TransactionViewModel",
                    "Gagal menyimpan transaksi",
                    error
                )

                _saveState.value = TransactionSaveUiState(
                    errorMessage = if (error is IllegalArgumentException) {
                        error.message ?: "Isian transaksi tidak valid."
                    } else {
                        "Transaksi gagal disimpan. Silakan coba lagi."
                    }
                )
            }
        }
    }

    fun clearSaveError() {
        _saveState.value = _saveState.value.copy(errorMessage = null)
    }
}