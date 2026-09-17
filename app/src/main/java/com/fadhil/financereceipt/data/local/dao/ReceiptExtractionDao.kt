package com.fadhil.financereceipt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fadhil.financereceipt.data.local.entity.ReceiptExtractionEntity

@Dao
interface ReceiptExtractionDao {
    @Insert
    suspend fun insert(extraction: ReceiptExtractionEntity): Long

    @Query("SELECT * FROM receipt_extractions WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: Long): ReceiptExtractionEntity?

    @Query("SELECT image_path FROM receipt_extractions")
    suspend fun getImagePaths(): List<String>
}
