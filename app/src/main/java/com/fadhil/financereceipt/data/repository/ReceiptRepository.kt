package com.fadhil.financereceipt.data.repository

import androidx.room.withTransaction
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.ReceiptExtractionEntity
import com.fadhil.financereceipt.data.local.entity.TransactionEntity

class ReceiptRepository(private val database: FinanceDatabase) {
    suspend fun save(categoryId: Long, note: String, receipt: ReceiptExtractionEntity): Long {
        require(receipt.storeName.isNotBlank() && receipt.storeName.length <= 120) {
            "Nama toko wajib diisi, maksimal 120 karakter."
        }
        require(receipt.totalAmount > 0) { "Nominal harus lebih dari nol." }
        require(note.length <= 500) { "Keterangan maksimal 500 karakter." }
        require(receipt.rawOcrText.isNotBlank() && receipt.imagePath.isNotBlank()) {
            "Hasil scan belum tersedia. Silakan pindai ulang."
        }
        return database.withTransaction {
            val category = requireNotNull(database.categoryDao().getById(categoryId)) {
                "Kategori tidak ditemukan. Pilih kategori kembali."
            }
            require(category.transactionType == "expense") { "Pilih kategori pengeluaran." }
            val id = database.transactionDao().insert(TransactionEntity(
                categoryId = categoryId, transactionType = "expense", amount = receipt.totalAmount,
                transactionDate = receipt.receiptDate,
                note = note.trim().ifBlank { receipt.storeName.trim() }, source = "scan_struk"
            ))
            database.receiptExtractionDao().insert(receipt.copy(
                extractionId = 0, transactionId = id, storeName = receipt.storeName.trim()
            ))
            id
        }
    }
}
