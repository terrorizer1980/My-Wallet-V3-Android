package piuk.blockchain.androidcore.utils.extensions

import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

fun String.toSafeDouble(locale: Locale = Locale.getDefault()): Double = try {
    var amount = this
    if (amount.isEmpty()) amount = "0"
    NumberFormat.getInstance(locale).parse(amount).toDouble()
} catch (e: ParseException) {
    0.0
}

fun String.toSafeLong(locale: Locale = Locale.getDefault()): Long = try {
    var amount = this
    if (amount.isEmpty()) amount = "0"
    Math.round(NumberFormat.getInstance(locale).parse(amount).toDouble() * 1e8)
} catch (e: ParseException) {
    0L
}