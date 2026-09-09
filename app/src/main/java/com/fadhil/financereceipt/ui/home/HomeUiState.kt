package com.fadhil.financereceipt.ui.home

import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory

data class HomeUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val errorMessage: String? = null
)
