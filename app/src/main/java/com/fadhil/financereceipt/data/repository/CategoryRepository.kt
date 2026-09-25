package com.fadhil.financereceipt.data.repository

import androidx.room.withTransaction
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.IncomeGroup
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val database: FinanceDatabase
) {
    private val categoryDao = database.categoryDao()

    fun observeByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(type)

    fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeByIncomeGroup(group: IncomeGroup): Flow<List<CategoryEntity>> =
        categoryDao.observeByIncomeGroup(group.name)

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

    private fun income(name: String, emoji: String, group: IncomeGroup) = CategoryEntity(
        categoryName = name,
        transactionType = "income",
        emoji = emoji,
        incomeGroup = group.name
    )

    private fun defaultCategories(): List<CategoryEntity> = listOf(
        income("Gaji", "💼", IncomeGroup.EMPLOYMENT),
        income("Bonus", "🎁", IncomeGroup.EMPLOYMENT),
        income("Tunjangan", "💼", IncomeGroup.EMPLOYMENT),
        income("Honor", "📝", IncomeGroup.EMPLOYMENT),
        income("Komisi", "🤝", IncomeGroup.EMPLOYMENT),

        income("Business Profit", "🏪", IncomeGroup.BUSINESS),
        income("Freelance", "💻", IncomeGroup.BUSINESS),
        income("Sales", "🛒", IncomeGroup.BUSINESS),
        income("Project Income", "📋", IncomeGroup.BUSINESS),

        income("Dividend", "📈", IncomeGroup.INVESTMENT),
        income("Rental Income", "🏠", IncomeGroup.INVESTMENT),
        income("Investment Return", "💹", IncomeGroup.INVESTMENT),

        income("Hadiah", "🎁", IncomeGroup.OTHER),
        income("Cashback", "💰", IncomeGroup.OTHER),
        income("Refund", "↩️", IncomeGroup.OTHER),
        // Tetap tersedia agar pengguna lama tidak kehilangan pilihan ini.
        income("Uang Saku", "💰", IncomeGroup.OTHER),
        income("Lainnya", "📥", IncomeGroup.OTHER),

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
