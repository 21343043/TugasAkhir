package com.fadhil.financereceipt.data.repository

import androidx.room.withTransaction
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val database: FinanceDatabase
) {
    private val categoryDao = database.categoryDao()

    fun observeByType(type: String): Flow<List<CategoryEntity>> {
        return categoryDao.observeByType(type)
    }

    fun observeAll(): Flow<List<CategoryEntity>> {
        return categoryDao.observeAll()
    }

    suspend fun initializeDefaultCategories() {
        database.withTransaction {
            if (categoryDao.countCategories() == 0) {
                categoryDao.insertAll(defaultCategories())
            }
        }
    }

    private fun defaultCategories(): List<CategoryEntity> {
        return listOf(
            CategoryEntity(
                categoryName = "Gaji",
                transactionType = "income",
                emoji = "💼"
            ),
            CategoryEntity(
                categoryName = "Bonus",
                transactionType = "income",
                emoji = "🎁"
            ),
            CategoryEntity(
                categoryName = "Uang Saku",
                transactionType = "income",
                emoji = "💰"
            ),
            CategoryEntity(
                categoryName = "Lainnya",
                transactionType = "income",
                emoji = "📥"
            ),
            CategoryEntity(
                categoryName = "Makanan",
                transactionType = "expense",
                emoji = "🍔"
            ),
            CategoryEntity(
                categoryName = "Transportasi",
                transactionType = "expense",
                emoji = "🚗"
            ),
            CategoryEntity(
                categoryName = "Belanja",
                transactionType = "expense",
                emoji = "🛍️"
            ),
            CategoryEntity(
                categoryName = "Tagihan",
                transactionType = "expense",
                emoji = "💡"
            ),
            CategoryEntity(
                categoryName = "Pendidikan",
                transactionType = "expense",
                emoji = "📚"
            ),
            CategoryEntity(
                categoryName = "Lainnya",
                transactionType = "expense",
                emoji = "📤"
            )
        )
    }
}