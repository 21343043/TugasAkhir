package com.fadhil.financereceipt.data.repository

import androidx.room.withTransaction
import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.TransactionEntity

class TransactionRepository(
    private val database: FinanceDatabase
) {
    fun observeHistory(): Flow<List<TransactionWithCategory>> =
        database.transactionDao().observeWithCategory()

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

    suspend fun updateTransaction(
        transactionId: Long,
        categoryId: Long,
        transactionType: String,
        amount: Long,
        transactionDate: Long,
        note: String
    ) {
        require(transactionType == "income" || transactionType == "expense") {
            "Jenis transaksi tidak valid."
        }
        require(amount > 0L) { "Nominal harus lebih dari nol." }
        require(note.length <= 500) { "Keterangan maksimal 500 karakter." }
        database.withTransaction {
            val existing = requireNotNull(database.transactionDao().getById(transactionId)) {
                "Transaksi tidak ditemukan. Tutup form lalu muat ulang riwayat."
            }
            val category = requireNotNull(database.categoryDao().getById(categoryId)) {
                "Kategori tidak ditemukan. Silakan pilih ulang."
            }
            require(category.transactionType == transactionType) {
                "Kategori tidak sesuai dengan jenis transaksi."
            }
            // Pertahankan ID, sumber pencatatan, dan waktu pembuatan transaksi.
            check(database.transactionDao().update(existing.copy(
                categoryId = categoryId,
                transactionType = transactionType,
                amount = amount,
                transactionDate = transactionDate,
                note = note.trim()
            )) == 1) { "Transaksi gagal diperbarui." }
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        database.withTransaction {
            requireNotNull(database.transactionDao().getById(transactionId)) {
                "Transaksi tidak ditemukan. Tutup dialog lalu muat ulang riwayat."
            }
            check(database.transactionDao().deleteById(transactionId) == 1) {
                "Transaksi gagal dihapus."
            }
        }
    }

}