package com.fadhil.financereceipt.ui.history

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory
import com.fadhil.financereceipt.data.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val errorMessage: String? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(FinanceDatabase.getInstance(application))
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()
    private var historyJob: Job? = null

    init { loadHistory() }

    fun loadHistory() {
        if (historyJob?.isActive == true) return
        historyJob = viewModelScope.launch {
            _uiState.value = HistoryUiState()
            try {
                repository.observeHistory().collect { rows ->
                    _uiState.value = HistoryUiState(isLoading = false, transactions = rows)
                }
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
}
