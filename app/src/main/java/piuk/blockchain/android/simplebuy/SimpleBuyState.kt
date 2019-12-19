package piuk.blockchain.android.simplebuy

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.compareTo
import piuk.blockchain.android.ui.base.mvi.MviState
import java.util.regex.Pattern

data class SimpleBuyState(
    val minAmount: FiatValue? = null,
    val maxAmount: FiatValue? = null,
    private val enteredAmount: String = "",
    private val currency: String = "USD",
    val selectedCryptoCurrency: CryptoCurrency? = null,
    val exchangePriceState: ExchangePriceState? = null
) : MviState {

    private val pattern = Pattern.compile("-?\\d+(\\.\\d+)?")

    private val enteredFiat: FiatValue?
        get() = if (enteredAmount.isEmpty() || pattern.matcher(enteredAmount).matches().not()) null else
            FiatValue.fromMajor(currency, enteredAmount.toBigDecimal())

    fun maxDecimalDigitsForAmount(): Int =
        maxAmount?.userDecimalPlaces ?: 0

    fun maxIntegerDigitsForAmount(): Int =
        maxAmount?.toStringParts()?.major?.length ?: 0

    fun isAmountValid(): Boolean =
        enteredFiat?.let {
            if (maxAmount != null && minAmount != null && enteredFiat != null) {
                it <= maxAmount && it >= minAmount
            } else false
        } ?: false
}

data class ExchangePriceState(
    val price: FiatValue? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)