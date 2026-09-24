package com.fadhil.financereceipt.data.repository

import androidx.room.withTransaction
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val database: FinanceDatabase
) {
    private val categoryDao = database.categoryDao()

    fun observeByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(type)

    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeByFinancialGroup(group: String): Flow<List<CategoryEntity>> {
        CategoryEntity.percentageFor(group) // Menolak nilai selain NEEDS/WANTS/SAVINGS.
        return categoryDao.observeByFinancialGroup(group)
    }

    suspend fun initializeDefaultCategories() {
        database.withTransaction {
            // IGNORE + indeks unik nama/jenis menambah kategori yang belum ada.
            // Data lama tidak ditimpa dan ID kategori tidak berubah.
            categoryDao.insertAll(defaultCategories())
        }
    }

    private fun expense(name: String, emoji: String, group: String) = CategoryEntity(
        categoryName = name,
        transactionType = "expense",
        emoji = emoji,
        financialGroup = group,
        recommendedPercentage = CategoryEntity.percentageFor(group)
    )

    private fun defaultCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(categoryName = "Gaji", transactionType = "income", emoji = "💼"),
        CategoryEntity(categoryName = "Bonus", transactionType = "income", emoji = "🎁"),
        CategoryEntity(categoryName = "Uang Saku", transactionType = "income", emoji = "💰"),
        CategoryEntity(categoryName = "Lainnya", transactionType = "income", emoji = "📥"),

        expense("Makanan", "🍔", CategoryEntity.NEEDS),
        expense("Transportasi", "🚗", CategoryEntity.NEEDS),
        expense("Tagihan Pokok", "💡", CategoryEntity.NEEDS),
        expense("Kesehatan", "🏥", CategoryEntity.NEEDS),
        expense("Pendidikan", "📚", CategoryEntity.NEEDS),
        expense("Tempat tinggal", "🏠", CategoryEntity.NEEDS),

        expense("Hiburan", "🎬", CategoryEntity.WANTS),
        expense("Shopping", "🛍️", CategoryEntity.WANTS),
        expense("Traveling", "✈️", CategoryEntity.WANTS),
        expense("Hobi", "🎨", CategoryEntity.WANTS),

        expense("Tabungan", "🏦", CategoryEntity.SAVINGS),
        expense("Hutang", "🧾", CategoryEntity.SAVINGS),
        expense("Investasi", "📈", CategoryEntity.SAVINGS),
        expense("Dana darurat", "🛟", CategoryEntity.SAVINGS)
    )
}
