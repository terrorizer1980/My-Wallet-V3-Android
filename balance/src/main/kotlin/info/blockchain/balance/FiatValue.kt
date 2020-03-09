package info.blockchain.balance

import info.blockchain.utils.tryParseBigDecimal
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private data class Key(val locale: Locale, val currencyCode: String, val includeSymbol: Boolean)

private object FiatFormat {

    private val cache: MutableMap<Key, NumberFormat> = ConcurrentHashMap()

    operator fun get(key: Key) = cache.getOrPut(key) {
        val currencyInstance = Currency.getInstance(key.currencyCode)
        val fmt = NumberFormat.getCurrencyInstance(key.locale) as DecimalFormat
        fmt.apply {
            decimalFormatSymbols =
                decimalFormatSymbols.apply {
                    currency = currencyInstance
                    if (!key.includeSymbol) {
                        currencySymbol = ""
                    }
                }
            minimumFractionDigits = currencyInstance.defaultFractionDigits
            maximumFractionDigits = currencyInstance.defaultFractionDigits
            roundingMode = RoundingMode.DOWN
        }
    }
}

// TODO: AND-1363 Remove suppress, possibly by implementing equals manually as copy is not needed
@Suppress("DataClassPrivateConstructor")
data class FiatValue private constructor(
    override val currencyCode: String,
    internal val value: BigDecimal
) : Money {

    // ALWAYS for display, so use default Locale
    override val symbol: String = Currency.getInstance(currencyCode).getSymbol(Locale.getDefault())

    override val maxDecimalPlaces: Int get() = maxDecimalPlaces(currencyCode)

    override val isZero: Boolean get() = value.signum() == 0

    override val isPositive: Boolean get() = value.signum() == 1

    override fun toBigDecimal(): BigDecimal = value

    val valueMinor: Long = value.movePointRight(maxDecimalPlaces).toLong()

    override fun toStringWithSymbol(): String =
        FiatFormat[Key(Locale.getDefault(), currencyCode, includeSymbol = true)].format(value)

    override fun toStringWithoutSymbol(): String =
        FiatFormat[Key(Locale.getDefault(), currencyCode, includeSymbol = false)]
            .format(value)
            .trim()

    override fun toNetworkString(): String =
        FiatFormat[Key(Locale.US, currencyCode, includeSymbol = false)]
            .format(value)
            .trim()
            .removeComma()

    operator fun plus(other: FiatValue): FiatValue {
        if (currencyCode != other.currencyCode)
            throw ValueTypeMismatchException("add", currencyCode, other.currencyCode)
        return FiatValue(currencyCode, value + other.value)
    }

    operator fun minus(other: FiatValue): FiatValue {
        if (currencyCode != other.currencyCode)
            throw ValueTypeMismatchException("subtract", currencyCode, other.currencyCode)
        return FiatValue(currencyCode, value - other.value)
    }

    override fun toZero(): FiatValue = fromMajor(currencyCode, BigDecimal.ZERO)

    override fun equals(other: Any?): Boolean =
        (other is FiatValue) && (other.currencyCode == currencyCode) && (other.value.compareTo(value) == 0)

    override fun hashCode(): Int {
        var result = currencyCode.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    companion object {

        fun fromMinor(currencyCode: String, minor: Long) =
            fromMajor(
                currencyCode,
                BigDecimal.valueOf(minor).movePointLeft(maxDecimalPlaces(currencyCode))
            )

        @JvmStatic
        fun fromMajor(currencyCode: String, major: BigDecimal, round: Boolean = true) =
            FiatValue(
                currencyCode,
                if (round) major.setScale(
                    maxDecimalPlaces(currencyCode),
                    RoundingMode.DOWN
                ) else major
            )

        fun fromMajorOrZero(currencyCode: String, major: String, locale: Locale = Locale.getDefault()) =
            fromMajor(
                currencyCode,
                major.tryParseBigDecimal(locale) ?: BigDecimal.ZERO
            )

        fun zero(currencyCode: String) = FiatValue(currencyCode, BigDecimal.ZERO)

        private fun maxDecimalPlaces(currencyCode: String) = Currency.getInstance(currencyCode).defaultFractionDigits
    }
}

@Suppress("SameParameterValue")
private fun ensureComparable(operation: String, currencyCodeA: String, currencyCodeB: String) {
    if (currencyCodeA != currencyCodeB)
        throw ValueTypeMismatchException(operation, currencyCodeA, currencyCodeB)
}

operator fun FiatValue.compareTo(b: FiatValue): Int {
    ensureComparable("compare", currencyCode, b.currencyCode)
    return valueMinor.compareTo(b.valueMinor)
}

fun FiatValue?.percentageDelta(previous: FiatValue?): Double =
    if (this != null && previous != null && !previous.isZero) {
        val current = this.toBigDecimal()
        val prev = previous.toBigDecimal()

        (current - prev)
            .divide(prev, 4, RoundingMode.HALF_EVEN)
            .movePointRight(2)
            .toDouble()
    } else {
        Double.NaN
    }

fun FiatValue?.toFloat(): Float =
    this?.toBigDecimal()?.toFloat() ?: 0.0f

fun CryptoValue.toFiat(price: FiatValue) =
    FiatValue.fromMajor(price.currencyCode, price.toBigDecimal() * toBigDecimal())

fun Iterable<FiatValue>.sum(): FiatValue? {
    if (!iterator().hasNext()) return null
    var total = FiatValue.zero(first().currencyCode)
    this.forEach { total += it }

    return total
}
