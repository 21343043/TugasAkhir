package com.fadhil.financereceipt.ui.home

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Awal inklusif, akhir eksklusif; memakai zona waktu perangkat. */
data class HomePeriod(val start: Long, val endExclusive: Long, val label: String)

fun homePeriod(anchorMillis: Long, period: String): HomePeriod {
    val start = Calendar.getInstance().apply {
        timeInMillis = anchorMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    when (period) {
        "Mingguan" -> {
            val daysSinceMonday = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7
            start.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
        }
        "Bulanan" -> start.set(Calendar.DAY_OF_MONTH, 1)
        "Tahunan" -> { start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.MONTH, Calendar.JANUARY) }
    }
    val end = (start.clone() as Calendar).apply {
        when (period) {
            "Mingguan" -> add(Calendar.DAY_OF_MONTH, 7)
            "Bulanan" -> add(Calendar.MONTH, 1)
            "Tahunan" -> add(Calendar.YEAR, 1)
            else -> add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val locale = Locale.forLanguageTag("id-ID")
    fun format(calendar: Calendar, pattern: String) = SimpleDateFormat(pattern, locale).format(calendar.time)
    val label = when (period) {
        "Mingguan" -> {
            val last = (end.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
            "${format(start, "dd MMM yyyy")} – ${format(last, "dd MMM yyyy")}"
        }
        "Bulanan" -> format(start, "MMMM yyyy")
        "Tahunan" -> format(start, "yyyy")
        else -> format(start, "dd MMMM yyyy")
    }
    return HomePeriod(start.timeInMillis, end.timeInMillis, label)
}

fun shiftHomePeriod(anchorMillis: Long, period: String, direction: Int): Long {
    // Berpindah dari awal periode mencegah tanggal 29–31 melompati bulan.
    return Calendar.getInstance().apply {
        timeInMillis = homePeriod(anchorMillis, period).start
        when (period) {
            "Mingguan" -> add(Calendar.DAY_OF_MONTH, 7 * direction)
            "Bulanan" -> add(Calendar.MONTH, direction)
            "Tahunan" -> add(Calendar.YEAR, direction)
            else -> add(Calendar.DAY_OF_MONTH, direction)
        }
    }.timeInMillis
}
