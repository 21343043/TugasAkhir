package com.fadhil.financereceipt.data.repository

import androidx.room.withTransaction
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.TransactionEntity

class TransactionRepository(
    private val database: FinanceDatabase
) {
    suspend fun saveManualTransaction(
        categoryId: Long,
        transactionType: String,
        amount: Long,
        transactionDate: Long,
        note: String
    ): Long {
        require(transactionType == "income" || transactionType == "expense") {
            "Jenis transaksi tidak valid."
        }
        require(amount > 0L) {
            "Nominal harus lebih dari nol."
        }
        require(note.length <= 500) {
            "Keterangan maksimal 500 karakter."
        }

        return database.withTransaction {
            val category = database.categoryDao().getById(categoryId)

            requireNotNull(category) {
                "Kategori tidak ditemukan. Silakan pilih ulang."
            }
            require(category.transactionType == transactionType) {
                "Kategori tidak sesuai dengan jenis transaksi."
            }

            database.transactionDao().insert(
                TransactionEntity(
                    categoryId = categoryId,
                    transactionType = transactionType,
                    amount = amount,
                    transactionDate = transactionDate,
                    note = note.trim(),
                    source = "manual"
                )
            )
        }
    }
}