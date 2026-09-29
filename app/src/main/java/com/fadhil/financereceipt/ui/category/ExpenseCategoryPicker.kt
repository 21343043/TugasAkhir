package com.fadhil.financereceipt.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Adapter data pengeluaran; seluruh desain dan alur popup berada di komponen bersama. */
@Composable
fun ExpenseCategoryPicker(
    sections: List<ExpenseCategorySection>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    GroupedCategoryPicker(
        groups = sections.map { section ->
            val emoji = when (section.key) {
                "NEEDS" -> "🏠"
                "WANTS" -> "🎮"
                "SAVINGS" -> "💰"
                else -> "📁"
            }
            CategoryPickerGroup(section.key, section.label, emoji,
                section.categories.map { category ->
                    CategorySelectionOption(category.categoryId.toString(),
                        ExpenseCategorySection.displayName(category), category.emoji,
                        "expense_subcategory_${category.categoryId}")
                })
        },
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = onCategorySelected,
        kind = "pengeluaran", tagPrefix = "expense", enabled = enabled, modifier = modifier
    )
}
