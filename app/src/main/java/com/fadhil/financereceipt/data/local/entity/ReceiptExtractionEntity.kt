package com.fadhil.financereceipt.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Snapshot hasil mesin tetap terpisah dari koreksi pengguna untuk evaluasi TA. */
@Entity(
    tableName = "receipt_extractions",
    foreignKeys = [ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["transaction_id"], childColumns = ["transaction_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["transaction_id"], unique = true)]
)
data class ReceiptExtractionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "extraction_id") val extractionId: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "store_name") val storeName: String,
    @ColumnInfo(name = "receipt_date") val receiptDate: Long,
    @ColumnInfo(name = "total_amount") val totalAmount: Long,
    @ColumnInfo(name = "raw_ocr_text") val rawOcrText: String,
    @ColumnInfo(name = "normalized_text") val normalizedText: String,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "detected_store_name") val detectedStoreName: String?,
    @ColumnInfo(name = "detected_receipt_date") val detectedReceiptDate: Long?,
    @ColumnInfo(name = "detected_total_amount") val detectedTotalAmount: Long?,
    @ColumnInfo(name = "parser_version") val parserVersion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
