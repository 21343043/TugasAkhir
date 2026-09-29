package com.fadhil.financereceipt.ui.category

import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import java.util.Locale

data class ExpenseCategorySection(
    val key: String,
    val label: String,
    val categories: List<CategoryEntity>
) {
    companion object {
        private val groups = listOf(
            CategoryEntity.NEEDS to "Needs",
            CategoryEntity.WANTS to "Wants",
            CategoryEntity.SAVINGS to "Savings"
        )
        private val names = listOf(
            "Makanan", "Transportasi", "Tempat Tinggal", "Tagihan", "Kesehatan", "Pendidikan",
            "Belanja", "Hiburan", "Hobi", "Traveling", "Perawatan Diri",
            "Tabungan", "Dana Darurat", "Investasi"
        )

        fun isLegacyDebt(category: CategoryEntity): Boolean =
            category.categoryName.trim().lowercase(Locale.ROOT) == "hutang"

        // Hanya label pemilih yang dirapikan, bukan nama/ID dalam database.
        fun displayName(category: CategoryEntity): String {
            val normalized = category.categoryName.trim().lowercase(Locale.ROOT)
            return names.firstOrNull { it.lowercase(Locale.ROOT) == normalized }
                ?: category.categoryName
        }

        fun fromCategories(
            categories: List<CategoryEntity>,
            retainedCategoryId: Long? = null
        ): List<ExpenseCategorySection> {
            val expenses = categories.filter {
                it.hasValidExpenseGroup() &&
                    !it.categoryName.trim().equals("Belum dikelompokkan", ignoreCase = true) &&
                    (!isLegacyDebt(it) || it.categoryId == retainedCategoryId)
            }.sortedWith(compareBy<CategoryEntity> {
                names.indexOf(displayName(it)).let { index -> if (index < 0) Int.MAX_VALUE else index }
            }.thenBy { it.categoryName.lowercase(Locale.ROOT) }.thenBy { it.categoryId })
            val sections = groups.map { (key, label) ->
                ExpenseCategorySection(key, "$label (${CategoryEntity.percentageFor(key)}%)",
                    expenses.filter { it.financialGroup == key })
            }
            return sections.filter { it.categories.isNotEmpty() }
        }
    }
}
