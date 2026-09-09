package com.fadhil.financereceipt.data.local.entity

import androidx.room.Embedded

// Hasil query gabungan, bukan tabel baru.
data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    val categoryName: String,
    val categoryEmoji: String
)
