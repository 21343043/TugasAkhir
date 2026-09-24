package com.fadhil.financereceipt.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(
            value = ["category_name", "transaction_type"],
            unique = true
        )
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    val categoryId: Long = 0,

    @ColumnInfo(name = "category_name")
    val categoryName: String,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String,

    @ColumnInfo(name = "emoji")
    val emoji: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // Null untuk pemasukan atau kategori lama yang belum dapat dipetakan.
    @ColumnInfo(name = "financial_group", defaultValue = "NULL")
    val financialGroup: String? = null,

    // Persentase kelompok, bukan jatah untuk setiap kategori di dalamnya.
    @ColumnInfo(name = "recommended_percentage", defaultValue = "NULL")
    val recommendedPercentage: Int? = percentageFor(financialGroup)
) {
    init {
        require(recommendedPercentage == percentageFor(financialGroup)) {
            "Pasangan kelompok dan persentase kategori tidak valid."
        }
        require(financialGroup == null || transactionType == "expense") {
            "Kelompok budgeting hanya berlaku untuk kategori pengeluaran."
        }
    }

    companion object {
        const val NEEDS = "NEEDS"
        const val WANTS = "WANTS"
        const val SAVINGS = "SAVINGS"

        fun percentageFor(group: String?): Int? = when (group) {
            null -> null
            NEEDS -> 50
            WANTS -> 30
            SAVINGS -> 20
            else -> throw IllegalArgumentException("Kelompok keuangan tidak valid: $group")
        }
    }
}
