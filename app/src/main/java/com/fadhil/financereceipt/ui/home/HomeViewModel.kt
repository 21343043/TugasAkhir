package com.fadhil.financereceipt.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(FinanceDatabase.getInstance(application))
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private var homeJob: Job? = null

    init { loadHome() }

    fun loadHome() {
        if (homeJob?.isActive == true) return
        homeJob = viewModelScope.launch {
            _uiState.value = HomeUiState()
            try {
                repository.observeHistory().collect { rows ->
                    _uiState.value = HomeUiState(isLoading = false, transactions = rows)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("HomeViewModel", "Gagal membaca ringkasan", error)
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = "Beranda gagal dimuat. Silakan coba lagi."
                )
            }
        }
    }
}
