package com.fadhil.financereceipt.ui.home

import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory
import com.fadhil.financereceipt.utils.FinancialSummaryCalculator

data class HomeUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val errorMessage: String? = null,
    val analysisMonth: HomePeriod = homePeriod(System.currentTimeMillis(), "Bulanan"),
    val financialSummary: FinancialSummaryCalculator.Summary? = null
)
