package com.fadhil.financereceipt.ui.category

import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.IncomeGroup

/** Induk bersifat pilihan UI; hanya ID subkategori yang disimpan di transaksi. */
data class IncomeCategorySection(
    val key: String,
    val label: String,
    val categories: List<CategoryEntity>
) {
    companion object {

        fun fromCategories(categories: List<CategoryEntity>): List<IncomeCategorySection> {
            val income = categories.filter { it.transactionType == "income"
                    && !it.categoryName.trim().equals("Belum dikelompokkan", ignoreCase = true) }
                .sortedBy { it.categoryName.lowercase(java.util.Locale.ROOT) }
            val sections = IncomeGroup.values().map { group ->
                IncomeCategorySection(group.name, group.label, income.filter { it.incomeGroup == group.name })
            }
            return sections.filter { it.categories.isNotEmpty() }
        }
    }
}
