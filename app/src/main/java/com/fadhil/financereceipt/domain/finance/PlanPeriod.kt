package com.fadhil.financereceipt.domain.finance

import java.util.Calendar

/** Batas bulan dalam zona waktu perangkat: awal termasuk, akhir tidak termasuk. */
fun planMonthBounds(year: Int, month: Int): Pair<Long, Long> {
    require(year in 1900..9999 && month in 1..12) { "Periode tidak valid." }
    val start = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1, 0, 0, 0)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    return start.timeInMillis to end.timeInMillis
}
