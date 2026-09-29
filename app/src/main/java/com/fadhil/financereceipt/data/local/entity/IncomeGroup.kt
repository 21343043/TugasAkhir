package com.fadhil.financereceipt.data.local.entity

/** Kode tersimpan stabil; label hanya untuk tampilan. Bukan kelompok budgeting. */
enum class IncomeGroup(val label: String) {
    EMPLOYMENT("Employment Income"),
    BUSINESS("Business Income"),
    INVESTMENT("Investment Income"),
    OTHER("Other Income");

    companion object {
        fun fromCode(code: String?): IncomeGroup? = values().firstOrNull { it.name == code }
    }
}
