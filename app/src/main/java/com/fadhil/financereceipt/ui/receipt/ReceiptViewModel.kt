package com.fadhil.financereceipt.ui.receipt

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.local.entity.ReceiptExtractionEntity
import com.fadhil.financereceipt.data.repository.ReceiptRepository
import com.fadhil.financereceipt.receipt.ReceiptImageStore
import com.fadhil.financereceipt.receipt.ReceiptOcr
import com.fadhil.financereceipt.receipt.ReceiptParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReceiptUiState(
    val imagePath: String? = null,
    val busy: Boolean = false,
    val busyLabel: String = "",
    val rawText: String = "",
    val result: ReceiptParser.Result? = null,
    val storeName: String = "",
    val dateMillis: Long? = null,
    val amount: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val error: String? = null,
    val savedId: Long? = null
)

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FinanceDatabase.getInstance(application)
    private val repository = ReceiptRepository(database)
    val images = ReceiptImageStore(application)
    private val ocr = ReceiptOcr(application)
    private val parser = ReceiptParser()
    private val _state = MutableStateFlow(ReceiptUiState())
    val state = _state.asStateFlow()
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories = _categories.asStateFlow()
    private val _categoryError = MutableStateFlow<String?>(null)
    val categoryError = _categoryError.asStateFlow()
    private var categoryJob: kotlinx.coroutines.Job? = null

    init {
        loadCategories()
        viewModelScope.launch {
            runCatching {
                val paths = database.receiptExtractionDao().getImagePaths()
                withContext(Dispatchers.IO) { images.cleanOrphans(paths) }
            }
        }
    }

    fun loadCategories() {
        if (categoryJob?.isActive == true) return
        _categoryError.value = null
        categoryJob = viewModelScope.launch {
            try { database.categoryDao().observeByType("expense").collect { _categories.value = it } }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { _categoryError.value = "Kategori gagal dimuat. Coba lagi." }
        }
    }

    fun showError(message: String) { _state.value = _state.value.copy(error = message) }
    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun selectImage(uri: Uri, fromCamera: Boolean = false) {
        if (_state.value.busy || _state.value.savedId != null) return
        val old = _state.value.imagePath
        _state.value = _state.value.copy(busy = true, busyLabel = "Menyiapkan gambar…", error = null)
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) { images.importImage(uri) }
                _state.value = ReceiptUiState(imagePath = path)
                withContext(Dispatchers.IO) { images.deleteDraft(old) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                _state.value = _state.value.copy(busy = false, error =
                    if (error is IllegalArgumentException) error.message else "Gambar tidak dapat dibuka. Pilih ulang gambar.")
            } finally {
                if (fromCamera) images.deleteCamera(uri)
            }
        }
    }

    fun rotateImage() {
        val current = _state.value
        val path = current.imagePath ?: return
        if (current.busy) return
        _state.value = current.copy(busy = true, busyLabel = "Memutar gambar…")
        viewModelScope.launch {
            try {
                val rotated = withContext(Dispatchers.IO) { images.rotate(path) }
                _state.value = ReceiptUiState(imagePath = rotated)
                withContext(Dispatchers.IO) { images.deleteDraft(path) }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { _state.value = current.copy(error = "Gambar gagal diputar. Pilih ulang gambar.") }
        }
    }

    fun processImage() {
        val current = _state.value
        val path = current.imagePath ?: return
        if (current.busy || current.savedId != null) return
        _state.value = current.copy(busy = true, busyLabel = "Membaca struk…", error = null)
        viewModelScope.launch {
            try {
                val read = ocr.recognize(path)
                require(read.rawText.isNotBlank()) { "Teks tidak terbaca. Foto ulang dengan cahaya cukup dan posisi tegak." }
                val parsed = withContext(Dispatchers.Default) { parser.parse(read.readingOrderText) }
                _state.value = current.copy(
                    rawText = read.rawText, result = parsed, storeName = parsed.storeName.orEmpty(),
                    dateMillis = parsed.dateMillis, amount = parsed.totalAmount?.toString().orEmpty(),
                    busy = false, error = null
                )
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                _state.value = current.copy(error = if (error is IllegalArgumentException) error.message
                    else "Struk gagal diproses. Coba lagi atau pilih foto lain.")
            }
        }
    }

    fun edit(storeName: String = _state.value.storeName, dateMillis: Long? = _state.value.dateMillis,
             amount: String = _state.value.amount, categoryId: Long? = _state.value.categoryId,
             note: String = _state.value.note) {
        if (_state.value.busy || _state.value.savedId != null) return
        _state.value = _state.value.copy(storeName = storeName.take(120), dateMillis = dateMillis,
            amount = amount, categoryId = categoryId, note = note.take(500), error = null)
    }

    fun backToPreview() {
        if (!_state.value.busy) _state.value = ReceiptUiState(imagePath = _state.value.imagePath)
    }

    fun save() {
        val current = _state.value
        if (current.busy || current.savedId != null) return
        val result = current.result ?: return
        val imagePath = current.imagePath ?: return
        val amount = current.amount.toLongOrNull()
        val categoryId = current.categoryId
        val date = current.dateMillis
        if (current.storeName.isBlank() || date == null || amount == null || amount <= 0 || categoryId == null) {
            showError("Lengkapi nama toko, tanggal, kategori, dan nominal yang lebih dari nol.")
            return
        }
        _state.value = current.copy(busy = true, busyLabel = "Menyimpan pengeluaran…", error = null)
        viewModelScope.launch {
            // Selesaikan copy + transaksi DB walaupun pemilik layar dihancurkan saat menyimpan.
            withContext(NonCancellable) {
                var savedImage: String? = null
                try {
                    val image = withContext(Dispatchers.IO) { images.persist(imagePath) }
                    savedImage = image
                    val id = repository.save(categoryId, current.note, ReceiptExtractionEntity(
                        transactionId = 0, storeName = current.storeName, receiptDate = date,
                        totalAmount = amount, rawOcrText = current.rawText,
                        normalizedText = result.normalizedText, imagePath = image,
                        detectedStoreName = result.storeName, detectedReceiptDate = result.dateMillis,
                        detectedTotalAmount = result.totalAmount, parserVersion = ReceiptParser.VERSION
                    ))
                    // Jangan letakkan operasi yang dapat gagal setelah commit dalam rollback gambar.
                    _state.value = current.copy(savedId = id, busy = false, error = null)
                } catch (error: Exception) {
                    savedImage?.let { image -> withContext(Dispatchers.IO) { runCatching { images.deletePersisted(image) } } }
                    _state.value = current.copy(error = if (error is IllegalArgumentException) error.message
                        else "Penyimpanan gagal. Periksa ruang penyimpanan dan coba lagi.")
                }
            }
        }
    }

}
