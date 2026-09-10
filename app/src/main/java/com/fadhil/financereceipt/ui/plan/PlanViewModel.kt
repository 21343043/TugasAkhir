package com.fadhil.financereceipt.ui.plan

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fadhil.financereceipt.data.local.database.FinanceDatabase
import com.fadhil.financereceipt.data.local.entity.CategoryEntity
import com.fadhil.financereceipt.data.repository.FinancialPlanRepository
import com.fadhil.financereceipt.data.repository.PlanProgressItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class PlanUiState(
    val year: Int = 0,
    val month: Int = 0,
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val plans: List<PlanProgressItem> = emptyList(),
    val errorMessage: String? = null
)

data class PlanSaveState(
    val isSaving: Boolean = false,
    val savedPlanId: Long? = null,
    val errorMessage: String? = null
)

data class PlanManageState(
    val isWorking: Boolean = false,
    val completedMessage: String? = null,
    val errorMessage: String? = null
)

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinancialPlanRepository(FinanceDatabase.getInstance(application))
    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow(PlanSaveState())
    val saveState = _saveState.asStateFlow()
    private var observation: Job? = null
    private val _manageState = MutableStateFlow(PlanManageState())
    val manageState = _manageState.asStateFlow()

    fun loadMonth(year: Int, month: Int, force: Boolean = false) {
        if (!force && observation?.isActive == true &&
            _uiState.value.year == year && _uiState.value.month == month) return
        observation?.cancel()
        _uiState.value = PlanUiState(year = year, month = month)
        observation = viewModelScope.launch {
            try {
                repository.initializeCategories()
                repository.observeMonth(year, month).collect { data ->
                    _uiState.value = PlanUiState(
                        year = year, month = month, isLoading = false,
                        categories = data.categories, plans = data.plans
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("PlanViewModel", "Gagal membaca plan", error)
                _uiState.value = PlanUiState(
                    year = year, month = month, isLoading = false,
                    errorMessage = "Plan gagal dimuat. Silakan coba lagi."
                )
            }
        }
    }

    fun savePlan(categoryId: Long?, year: Int, month: Int, amountText: String) {
        if (_saveState.value.isSaving || _saveState.value.savedPlanId != null || _manageState.value.isWorking) return
        val amount = amountText.toLongOrNull()
        if (categoryId == null) {
            _saveState.value = PlanSaveState(errorMessage = "Pilih kategori terlebih dahulu.")
            return
        }
        if (amount == null || amount <= 0) {
            _saveState.value = PlanSaveState(errorMessage = "Anggaran harus lebih dari nol.")
            return
        }
        _saveState.value = PlanSaveState(isSaving = true)
        viewModelScope.launch {
            try {
                val id = repository.createPlan(categoryId, year, month, amount)
                _saveState.value = PlanSaveState(savedPlanId = id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("PlanViewModel", "Gagal menyimpan plan", error)
                _saveState.value = PlanSaveState(
                    errorMessage = if (error is IllegalArgumentException) {
                        error.message ?: "Isian plan tidak valid."
                    } else "Plan gagal disimpan. Silakan coba lagi."
                )
            }
        }
    }

    fun resetSaveState() {
        if (!_saveState.value.isSaving) _saveState.value = PlanSaveState()
    }

    fun updateBudget(planId: Long, amountText: String) {
        if (_manageState.value.isWorking || _manageState.value.completedMessage != null || _saveState.value.isSaving) return
        val amount = amountText.toLongOrNull()
        if (amount == null || amount <= 0) {
            _manageState.value = PlanManageState(errorMessage = "Anggaran harus lebih dari nol.")
            return
        }
        performPlanChange("Batas anggaran berhasil diperbarui.") {
            repository.updateBudget(planId, amount)
        }
    }

    fun deletePlan(planId: Long) {
        performPlanChange("Plan berhasil dihapus. Data transaksi tetap tersimpan.") {
            repository.deletePlan(planId)
        }
    }

    private fun performPlanChange(message: String, operation: suspend () -> Unit) {
        if (_manageState.value.isWorking || _manageState.value.completedMessage != null || _saveState.value.isSaving) return
        _manageState.value = PlanManageState(isWorking = true)
        viewModelScope.launch {
            try {
                operation()
                _manageState.value = PlanManageState(completedMessage = message)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("PlanViewModel", "Gagal mengubah plan", error)
                _manageState.value = PlanManageState(
                    errorMessage = if (error is IllegalArgumentException) {
                        error.message ?: "Isian tidak valid."
                    } else "Perubahan gagal disimpan. Silakan coba lagi."
                )
            }
        }
    }

    fun resetManageState() {
        if (!_manageState.value.isWorking) _manageState.value = PlanManageState()
    }
}
