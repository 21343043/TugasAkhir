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

data class TransactionCategoryUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
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
}