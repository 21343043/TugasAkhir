package com.fadhil.financereceipt.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

data class CategorySelectionOption(
    val key: String,
    val label: String,
    val emoji: String,
    val testTag: String
)

/** Satu desain untuk keempat popup. Tidak mengakses database atau menyimpan transaksi. */
@Composable
fun CategorySelectionDialog(
    options: List<CategorySelectionOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Pilih kategori",
    dialogTag: String = "category_selection_dialog"
) {
    val configuration = LocalConfiguration.current
    // Ruang judul/footer diperbesar mengikuti skala font. List saja yang bergulir.
    val listHeight = (configuration.screenHeightDp * 0.85f -
        180f * configuration.fontScale.coerceAtLeast(1f)).coerceIn(56f, 360f).dp
    val ink = Color(0xFF172B46)
    val accent = Color(0xFFEF0012)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(0.94f).testTag(dialogTag),
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, color = ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(listHeight).testTag("${dialogTag}_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(options, key = { it.key }) { option ->
                    val selected = option.key == selectedKey
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(if (selected) Color(0xFFFFECEF) else Color.Transparent, RoundedCornerShape(12.dp))
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(option.key) })
                            .testTag(option.testTag)
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(option.emoji.ifBlank { "📁" }, fontSize = 22.sp, modifier = Modifier.width(32.dp))
                        Text(option.label, color = ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (selected) Text("✓", color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("${dialogTag}_cancel")) {
                Text("Batal", color = accent, fontSize = 14.sp)
            }
        }
    )
}
