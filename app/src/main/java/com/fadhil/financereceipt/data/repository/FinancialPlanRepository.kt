package com.fadhil.financereceipt.data.repository

import androidx.room.withTransaction
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.FinancialPlanEntity
import com.fadhil.financereceipt.utils.BudgetStatusCalculator
import com.fadhil.financereceipt.utils.BudgetUsage
import com.fadhil.financereceipt.utils.planMonthBounds
import com.fadhil.financereceipt.utils.sumPlanExpenses
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class PlanProgressItem(
    val plan: FinancialPlanEntity,
    val category: CategoryEntity,
    val usage: BudgetUsage
)

data class MonthlyPlans(
    val categories: List<CategoryEntity>,
    val plans: List<PlanProgressItem>
)

class FinancialPlanRepository(private val database: FinanceDatabase) {
    suspend fun initializeCategories() {
        CategoryRepository(database).initializeDefaultCategories()
    }

    fun observeMonth(year: Int, month: Int): Flow<MonthlyPlans> {
        val (start, end) = planMonthBounds(year, month)
        return combine(
            database.financialPlanDao().observeByMonth(year, month),
            database.categoryDao().observeAll(),
            database.transactionDao().observeAll()
        ) { plans, categories, transactions ->
            val categoriesById = categories.associateBy { it.categoryId }
            val spentByCategory = sumPlanExpenses(transactions, start, end)
            val progress = plans.map { plan ->
                val category = checkNotNull(categoriesById[plan.categoryId]) {
                    "Kategori anggaran tidak ditemukan."
                }
                PlanProgressItem(
                    plan,
                    category,
                    BudgetStatusCalculator.calculate(
                        plan.budgetAmount, spentByCategory[plan.categoryId] ?: BigInteger.ZERO
                    )
                )
            }.sortedBy { it.category.categoryName }
            MonthlyPlans(categories.filter { it.transactionType == "expense" }, progress)
        }
    }

    suspend fun createPlan(categoryId: Long, year: Int, month: Int, amount: Long): Long {
        require(amount > 0) { "Anggaran harus lebih dari nol." }
        planMonthBounds(year, month)
        return database.withTransaction {
            val category = requireNotNull(database.categoryDao().getById(categoryId)) {
                "Kategori tidak ditemukan. Silakan pilih ulang."
            }
            require(category.transactionType == "expense") {
                "Plan hanya dapat dibuat untuk kategori pengeluaran."
            }
            require(database.financialPlanDao().getByCategoryAndMonth(categoryId, year, month) == null) {
                "Kategori tersebut sudah memiliki plan pada bulan ini."
            }
            val now = System.currentTimeMillis()
            database.financialPlanDao().insert(
                FinancialPlanEntity(
                    categoryId = categoryId, planYear = year, planMonth = month,
                    budgetAmount = amount, createdAt = now, updatedAt = now
                )
            )
        }
    }

    suspend fun updateBudget(planId: Long, amount: Long) {
        require(amount > 0) { "Anggaran harus lebih dari nol." }
        database.withTransaction {
            val existing = requireNotNull(database.financialPlanDao().getById(planId)) {
                "Plan tidak ditemukan. Tutup form lalu muat ulang daftar."
            }
            val updated = database.financialPlanDao().update(
                existing.copy(budgetAmount = amount, updatedAt = System.currentTimeMillis())
            )
            check(updated == 1) { "Plan gagal diperbarui." }
        }
    }

    suspend fun deletePlan(planId: Long) {
        database.withTransaction {
            requireNotNull(database.financialPlanDao().getById(planId)) {
                "Plan tidak ditemukan. Tutup dialog lalu muat ulang daftar."
            }
            check(database.financialPlanDao().deleteById(planId) == 1) {
                "Plan gagal dihapus."
            }
        }
    }
}
