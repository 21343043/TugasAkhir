package com.fadhil.financereceipt.ui.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class CategoryPickerGroup(
    val key: String,
    val label: String,
    val emoji: String,
    val options: List<CategorySelectionOption>
)

/** Alur dan state bersama, supaya perilaku popup pemasukan/pengeluaran identik. */
@Composable
internal fun GroupedCategoryPicker(
    groups: List<CategoryPickerGroup>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    kind: String,
    tagPrefix: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedKey = selectedCategoryId?.toString()
    val selectedGroup = groups.firstOrNull { group -> group.options.any { it.key == selectedKey } }
    var groupKey by rememberSaveable { mutableStateOf(selectedGroup?.key) }
    var stage by rememberSaveable { mutableStateOf<String?>(null) }
    val activeGroup = selectedGroup ?: groups.firstOrNull { it.key == groupKey }
    val selectedOption = activeGroup?.options?.firstOrNull { it.key == selectedKey }
    val ink = Color(0xFF172B46)

    LaunchedEffect(selectedGroup?.key, selectedCategoryId) {
        if (selectedGroup != null) groupKey = selectedGroup.key
    }
    LaunchedEffect(enabled) { if (!enabled) stage = null }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kelompok $kind", fontSize = 12.sp, color = ink)
        OutlinedButton(
            onClick = { stage = "group" }, enabled = enabled && groups.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_group_picker"),
            shape = RoundedCornerShape(14.dp)
        ) { Text(activeGroup?.label ?: "Pilih kelompok $kind", color = ink) }

        Text("Subkategori $kind", fontSize = 12.sp, color = ink)
        OutlinedButton(
            onClick = { stage = "subcategory" }, enabled = enabled && activeGroup != null,
            modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_subcategory_picker"),
            shape = RoundedCornerShape(14.dp)
        ) { Text(selectedOption?.let { "${it.emoji} ${it.label}" } ?: "Pilih subkategori $kind", color = ink) }

        if (selectedCategoryId != null && selectedGroup == null && groups.isNotEmpty()) {
            Text("Kategori lama tidak tersedia untuk dipilih. Pilih kelompok dan subkategori sebelum menyimpan.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }

    if (stage == "group" && enabled) {
        CategorySelectionDialog(
            title = "Pilih kelompok $kind",
            options = groups.map { CategorySelectionOption(it.key, it.label, it.emoji, "${tagPrefix}_group_${it.key}") },
            selectedKey = activeGroup?.key,
            onSelect = { key ->
                if (activeGroup?.key != key) onCategorySelected(null)
                groupKey = key
                stage = "subcategory"
            },
            onDismiss = { stage = null },
            dialogTag = "${tagPrefix}_group_dialog"
        )
    }
    if (stage == "subcategory" && enabled && activeGroup != null) {
        CategorySelectionDialog(
            title = "Pilih subkategori $kind",
            options = activeGroup.options,
            selectedKey = selectedKey,
            onSelect = { key ->
                onCategorySelected(key.toLong())
                stage = null
            },
            onDismiss = { stage = null },
            dialogTag = "${tagPrefix}_subcategory_dialog"
        )
    }
}
