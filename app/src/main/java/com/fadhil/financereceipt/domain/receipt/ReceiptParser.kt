package com.fadhil.financereceipt.domain.receipt

import java.math.BigDecimal
import java.text.Normalizer
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

/** Parser aturan deterministik. Tidak bergantung pada Android maupun Room. */
class ReceiptParser {
    companion object {
        const val VERSION = "rules-1.0"
        private val NUMERIC_DATE = Regex("""(?<!\d)(\d{1,4})([/.-])(\d{1,2})\2(\d{2,4})(?!\d)""")
        private val WORD_DATE = Regex("""(?i)(?<!\d)(\d{1,2})[\s-]+([a-z]+)[\s,-]+(\d{4}|\d{2})(?!\d)""")
        private val AMOUNT = Regex("""(?<![\p{L}\d/:.,-])(?:Rp\.?\s*)?(\d+(?:[.,]\d+)*)(?![\p{L}\d/:.,%])""", RegexOption.IGNORE_CASE)
        private val EXCLUDED_TOTAL = Regex("""(?i)\b(sub\s*total|kembali(?:an)?|change|tunai|cash|debit|kredit|credit|diskon|discount|ppn|pajak|tax|hemat|saving|total\s+(?:item|qty|barang|diskon))\b""")
        private val TOTAL = Regex("""(?i)\b(grand\s*total|total\s*(?:bayar|belanja|pembayaran)|jumlah\s*(?:bayar|pembayaran)|total|jumlah|bayar|amount\s*due)\b""")

    }

    data class Result(
        @JvmField val storeName: String?,
        @JvmField val dateMillis: Long?,
        @JvmField val totalAmount: Long?,
        @JvmField val normalizedText: String,
        @JvmField val warnings: List<String>
    )

    fun parse(text: String?): Result {
        val lines = Normalizer.normalize(text.orEmpty(), Normalizer.Form.NFKC)
            .replace('\u00a0', ' ').split(Regex("""\r?\n"""))
            .map { it.replace(Regex("""[\p{Cntrl}&&[^\t]]"""), "")
                .replace(Regex("""[\t ]+"""), " ").trim() }.filter { it.isNotEmpty() }
        val warnings = mutableListOf<String>()
        var date: Long? = null
        for (line in lines) {
            if (Regex(""".*\b(exp|expired|berlaku|kadaluarsa|kedaluwarsa)\b.*""")
                    .matches(line.lowercase(Locale.ROOT))) continue
            val candidate = parseDate(line) ?: continue
            if (date == null) date = candidate
            else if (date != candidate) {
                val warning = "Terdapat beberapa tanggal. Periksa tanggal transaksi."
                if (warning !in warnings) warnings.add(warning)
            }
        }
        val store = lines.take(8).firstOrNull { line ->
            line.length in 3..120 && Regex("""[\p{L}]{3}""").containsMatchIn(line) &&
                !NUMERIC_DATE.containsMatchIn(line) && !WORD_DATE.containsMatchIn(line) &&
                !Regex(""".*\b(jl|jln|jalan|telp|telepon|phone|npwp|kasir|cashier|tanggal|waktu|invoice|receipt|struk|nota|no|nomor|www|http|pelanggan|member|selamat|terima|subtotal|total|tunai|kembali)\b.*""")
                    .matches(line.lowercase(Locale.ROOT)) &&
                line.count { Character.isDigit(it) } <= line.length / 3
        }
        var total: Long? = null
        var bestPriority = -1
        val competingTotals = mutableListOf<Long>()
        for ((index, line) in lines.withIndex()) {
            if (EXCLUDED_TOTAL.containsMatchIn(line)) continue
            val keyword = TOTAL.find(line) ?: continue
            val key = keyword.value.lowercase(Locale.ROOT)
            val priority = when {
                "grand" in key || "due" in key || ("bayar" in key && key != "bayar") -> 3
                key == "bayar" -> 1
                else -> 2
            }
            val tail = line.substring(keyword.range.last + 1).trim()
            var amount = lastAmount(tail)
            if (amount == null && Regex("""[:=Rp.\s]*""").matches(tail)) {
                val next = lines.getOrNull(index + 1)?.trim()
                if (next != null && Regex("""(?i)(?:Rp\.?\s*)?\d[\d.,]*[\s,-]*""").matches(next))
                    amount = lastAmount(next)
            }
            if (amount == null || amount <= 0) continue
            if (priority > bestPriority) {
                total = amount
                bestPriority = priority
                competingTotals.clear()
                competingTotals.add(amount)
            } else if (priority == bestPriority) {
                total = amount
                competingTotals.add(amount)
            }
        }
        if (competingTotals.distinct().size > 1)
            warnings.add("Terdapat beberapa kandidat total. Periksa nominal pembayaran.")
        if (bestPriority == 1) warnings.add("Nominal diambil dari label Bayar. Pastikan bukan uang yang diserahkan.")
        if (store == null) warnings.add("Nama toko belum ditemukan. Isi secara manual.")
        if (date == null) warnings.add("Tanggal belum ditemukan atau tidak valid. Pilih tanggal transaksi.")
        if (total == null) warnings.add("Total belum ditemukan atau formatnya meragukan. Isi nominal secara manual.")
        return Result(store, date, total, lines.joinToString("\n"), java.util.Collections.unmodifiableList(warnings.toList()))
    }

    private fun lastAmount(input: String): Long? {
        val text = input.replaceFirst(Regex("""^\s*[:=]\s*"""), "")
            .replace(Regex("""(?<=\d)[.,]-(?=\s|$)"""), "")
        var result: Long? = null
        for (match in AMOUNT.findAll(text)) {
            val end = match.range.last + 1
            if (end < text.length && Regex("""^\s*[%:/].*""").matches(text.substring(end))) continue
            parseRupiah(match.groupValues[1])?.let { result = it }
        }
        return result
    }

    /** Pecahan rupiah nonnol dan overflow ditolak tanpa pembulatan. */
    fun parseRupiah(input: String?): Long? {
        if (input == null) return null
        val value = input.trim().replaceFirst(Regex("""(?i)^Rp\.?\s*"""), "").trim()
            .replaceFirst(Regex("""[.,]-$"""), "")
        val decimal = when {
            Regex("""\d+""").matches(value) -> value
            Regex("""\d{1,3}(?:\.\d{3})+(?:,\d{1,2})?""").matches(value) -> value.replace(".", "").replace(',', '.')
            Regex("""\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?""").matches(value) -> value.replace(",", "")
            Regex("""\d+[.,]\d{1,2}""").matches(value) -> value.replace(',', '.')
            else -> return null
        }
        return try { BigDecimal(decimal).longValueExact().takeIf { it > 0 } }
        catch (_: ArithmeticException) { null }
        catch (_: NumberFormatException) { null }
    }

    private fun parseDate(text: String): Long? {
        for (match in NUMERIC_DATE.findAll(text)) {
            val parts = match.groupValues
            val iso = parts[1].length == 4
            val year = parts[if (iso) 1 else 4].toInt().let { if (it < 100) it + 2000 else it }
            validDate(year, parts[3].toInt(), parts[if (iso) 4 else 1].toInt())?.let { return it }
        }
        val months = listOf("jan|januari|january", "feb|februari|february", "mar|maret|march",
            "apr|april", "mei|may", "jun|juni|june", "jul|juli|july", "agu|agt|agustus|aug|august",
            "sep|sept|september", "okt|oktober|oct|october", "nov|november", "des|desember|dec|december")
        for (match in WORD_DATE.findAll(text)) {
            val parts = match.groupValues
            val month = months.indexOfFirst { Regex(it).matches(parts[2].lowercase(Locale.ROOT)) } + 1
            if (month == 0) continue
            val year = parts[3].toInt().let { if (it < 100) it + 2000 else it }
            validDate(year, month, parts[1].toInt())?.let { return it }
        }
        return null
    }

    private fun validDate(year: Int, month: Int, day: Int): Long? {
        if (year !in 2000..2099) return null
        val calendar = GregorianCalendar().apply {
            clear()
            isLenient = false
            set(year, month - 1, day, 12, 0, 0)
        }
        return try { calendar.timeInMillis } catch (_: IllegalArgumentException) { null }
    }
}
