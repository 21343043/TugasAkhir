package com.fadhil.financereceipt.receipt

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import com.fadhil.financereceipt.ui.history.ReceiptPhotoUiState
import com.fadhil.financereceipt.ui.history.TransactionEditDialog
import com.fadhil.financereceipt.ui.history.TransactionManageState
import com.fadhil.financereceipt.ui.receipt.ReceiptPhotoDialog
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Jalankan pada emulator/perangkat; semua berkas uji dibuat dengan nama unik dan dibersihkan. */
@RunWith(AndroidJUnit4::class)
class ReceiptPhotoTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savedCopyRemainsReadableAfterDraftIsDeleted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drafts = File(context.cacheDir, "receipt_drafts").apply { mkdirs() }
        val draft = File(drafts, "test-${UUID.randomUUID()}.jpg")
        val original = Bitmap.createBitmap(80, 160, Bitmap.Config.ARGB_8888)
        var savedPath: String? = null
        val store = ReceiptImageStore(context)
        try {
            original.eraseColor(android.graphics.Color.WHITE)
            draft.outputStream().use { assertTrue(original.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
            val relativePath = store.persist(draft.path)
            savedPath = relativePath
            store.deleteDraft(draft.path)
            assertFalse(draft.exists())
            val restored = ReceiptImageStore(context).readPersistedBitmap(relativePath)
            assertNotNull(restored)
            assertEquals(80, restored!!.width)
            assertEquals(160, restored.height)
            restored.recycle()
            store.deletePersisted(relativePath)
            assertNull(store.readPersistedBitmap(relativePath))
        } finally {
            original.recycle()
            store.deleteDraft(draft.path)
            savedPath?.let { store.deletePersisted(it) }
        }
    }

    @Test fun photoViewerKeepsUnsavedEditAndSupportsZoomReset() {
        val bitmap = Bitmap.createBitmap(80, 160, Bitmap.Config.ARGB_8888)
        var savedAmount: String? = null
        compose.setContent {
            MaterialTheme {
                var photoOpen by remember { mutableStateOf(false) }
                TransactionEditDialog(
                    transaction = TransactionEntity(transactionId = 1, categoryId = 1,
                        transactionType = "expense", amount = 25000,
                        transactionDate = 1789128000000L, source = "scan_struk"),
                    categories = listOf(CategoryEntity(categoryId = 1, categoryName = "Belanja",
                        transactionType = "expense", emoji = "🛒")),
                    manageState = TransactionManageState(),
                    onDismiss = {},
                    onSave = { _, _, amount, _, _ -> savedAmount = amount },
                    onViewReceipt = { photoOpen = true }
                )
                if (photoOpen) ReceiptPhotoDialog(
                    state = ReceiptPhotoUiState(transactionId = 1, bitmap = bitmap),
                    onDismiss = { photoOpen = false }, onRetry = {}
                )
            }
        }
        compose.onNodeWithText("Nominal").performScrollTo().performTextReplacement("37500")
        compose.onNodeWithText("Lihat foto struk").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Foto struk transaksi tersimpan").assertIsDisplayed()
        compose.onNodeWithContentDescription("Perbesar foto").performClick()
        compose.onNodeWithText("150%").assertIsDisplayed()
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.onNodeWithText("Tutup").performClick()
        compose.onNodeWithText("37500").assertExists()
        compose.onNodeWithText("Simpan").performClick()
        compose.runOnIdle { assertEquals("37500", savedAmount) }
    }

    @Test fun missingPhotoShowsRecoverableMessage() {
        var retried = false
        var closed = false
        compose.setContent {
            MaterialTheme {
                ReceiptPhotoDialog(
                    state = ReceiptPhotoUiState(transactionId = 1,
                        errorMessage = "Foto struk tidak tersedia."),
                    onDismiss = { closed = true }, onRetry = { retried = true }
                )
            }
        }
        compose.onNodeWithText("Foto struk tidak tersedia.").assertIsDisplayed()
        compose.onNodeWithText("Coba lagi").performClick()
        compose.runOnIdle { assertTrue(retried) }
        compose.onNodeWithText("Tutup").performClick()
        compose.runOnIdle { assertTrue(closed) }
    }
}
