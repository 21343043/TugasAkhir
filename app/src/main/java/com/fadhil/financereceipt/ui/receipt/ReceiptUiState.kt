package com.fadhil.financereceipt.ui.receipt

import com.fadhil.financereceipt.domain.receipt.ReceiptParser

data class ReceiptUiState(
    val imagePath: String? = null,
    val busy: Boolean = false,
    val busyLabel: String = "",
    val rawText: String = "",
    val result: ReceiptParser.Result? = null,
    val storeName: String = "",
    val dateMillis: Long? = null,
    val amount: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val error: String? = null,
    val savedId: Long? = null
)
