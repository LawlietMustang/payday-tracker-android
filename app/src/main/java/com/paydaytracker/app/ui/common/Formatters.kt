package com.paydaytracker.app.ui.common

import java.text.NumberFormat
import java.util.Locale

object Formatters {

    fun formatMoney(amount: Double, currencyCode: String = "EUR"): String {
        val locale = Locale.GERMANY
        val format = NumberFormat.getCurrencyInstance(locale)
        try {
            format.currency = java.util.Currency.getInstance(currencyCode)
        } catch (_: Exception) {}
        return format.format(amount)
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "$h h" else "$h h $m min"
    }

    fun formatMonthTitle(monthKey: String): String {
        return try {
            val parts = monthKey.split("-").map { it.toInt() }
            val ym = java.time.YearMonth.of(parts[0], parts[1])
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY)
            ym.format(formatter)
        } catch (_: Exception) {
            monthKey
        }
    }
}
