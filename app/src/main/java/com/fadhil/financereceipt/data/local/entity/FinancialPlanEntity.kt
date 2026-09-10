package com.fadhil.financereceipt.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_plans",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["category_id", "plan_year", "plan_month"], unique = true),
        Index(value = ["plan_year", "plan_month"])
    ]
)
data class FinancialPlanEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "plan_id")
    val planId: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "plan_year")
    val planYear: Int,

    // Bulan kalender 1–12, bukan nilai Calendar.MONTH (0–11).
    @ColumnInfo(name = "plan_month")
    val planMonth: Int,

    @ColumnInfo(name = "budget_amount")
    val budgetAmount: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
