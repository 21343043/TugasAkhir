package com.fadhil.financereceipt.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Adapter data pemasukan; seluruh desain dan alur popup berada di komponen bersama. */
@Composable
fun IncomeCategoryPicker(
    sections: List<IncomeCategorySection>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    GroupedCategoryPicker(
        groups = sections.map { section ->
            val emoji = when (section.key) {
                "EMPLOYMENT" -> "💼"
                "BUSINESS" -> "🏢"
                "INVESTMENT" -> "📈"
                "OTHER" -> "💰"
                else -> "📁"
            }
            CategoryPickerGroup(section.key, section.label, emoji,
                section.categories.map { category ->
                    CategorySelectionOption(category.categoryId.toString(),
                        category.categoryName, category.emoji,
                        "income_subcategory_${category.categoryId}")
                })
        },
        selectedCategoryId = selectedCategoryId,
        onCategorySelected = onCategorySelected,
        kind = "pemasukan", tagPrefix = "income", enabled = enabled, modifier = modifier
    )
}
