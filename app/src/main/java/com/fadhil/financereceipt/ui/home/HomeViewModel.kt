package com.fadhil.financereceipt.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.repository.TransactionRepository
import com.fadhil.financereceipt.utils.FinancialSummaryCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = TransactionRepository(FinanceDatabase.getInstance(application))
    private val monthKey = "financial_analysis_month"
    private val analysisAnchor = savedStateHandle.getStateFlow(
        monthKey, homePeriod(System.currentTimeMillis(), "Bulanan").start
    )
    private val _uiState = MutableStateFlow(HomeUiState(
        analysisMonth = homePeriod(analysisAnchor.value, "Bulanan")
    ))
    val uiState = _uiState.asStateFlow()
    private var homeJob: Job? = null

    init { loadHome() }

    fun shiftAnalysisMonth(direction: Int) {
        if (_uiState.value.isLoading) return
        require(direction == -1 || direction == 1)
        selectMonth(shiftHomePeriod(analysisAnchor.value, "Bulanan", direction))
    }

    fun resetAnalysisMonth() = selectMonth(System.currentTimeMillis())

    private fun selectMonth(anchor: Long) {
        val period = homePeriod(anchor, "Bulanan")
        if (period.start == analysisAnchor.value && _uiState.value.errorMessage == null) return
        _uiState.value = _uiState.value.copy(
            isLoading = true, analysisMonth = period, financialSummary = null, errorMessage = null
        )
        savedStateHandle[monthKey] = period.start
        loadHome()
    }

    fun loadHome() {
        if (homeJob?.isActive == true) return
        _uiState.value = HomeUiState(analysisMonth = homePeriod(analysisAnchor.value, "Bulanan"))
        homeJob = viewModelScope.launch {
            try {
                combine(repository.observeHistory(), analysisAnchor) { rows, anchor ->
                    val month = homePeriod(anchor, "Bulanan")
                    val entries = rows.map { row ->
                        FinancialSummaryCalculator.Entry(
                            row.transaction.transactionDate,
                            row.transaction.transactionType,
                            row.transaction.amount,
                            row.financialGroup
                        )
                    }
                    HomeUiState(
                        isLoading = false,
                        transactions = rows,
                        analysisMonth = month,
                        financialSummary = FinancialSummaryCalculator.calculate(
                            entries, month.start, month.endExclusive
                        )
                    )
                }.flowOn(Dispatchers.Default).collect { state ->
                    // Jangan tampilkan hasil bulan sebelumnya bila pengguna baru berganti bulan.
                    if (state.analysisMonth.start == analysisAnchor.value) _uiState.value = state
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("HomeViewModel", "Gagal membaca ringkasan", error)
                _uiState.value = HomeUiState(
                    isLoading = false,
                    analysisMonth = homePeriod(analysisAnchor.value, "Bulanan"),
                    errorMessage = "Beranda gagal dimuat. Silakan coba lagi."
                )
            }
        }
    }
}
