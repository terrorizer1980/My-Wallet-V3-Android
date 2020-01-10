package piuk.blockchain.android.simplebuy

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.compareTo
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.regex.Pattern

data class SimpleBuyState(
    val minAmount: FiatValue? = null,
    val maxAmount: FiatValue? = null,
    val enteredAmount: String = "",
    private val currency: String = "USD",
    val predefinedAmounts: List<FiatValue> = emptyList(),
    val selectedCryptoCurrency: CryptoCurrency? = null,
    val orderState: OrderState = OrderState.UNITIALISED,
    val kycVerificationState: KycState = KycState.PENDING,
    val exchangePriceState: ExchangePriceState? = null
) : MviState {

    private val pattern = Pattern.compile("-?\\d+(\\.\\d+)?")

    val enteredFiat: FiatValue? by unsafeLazy {
        if (enteredAmount.isEmpty() || pattern.matcher(enteredAmount).matches().not()) null else
            FiatValue.fromMajor(currency, enteredAmount.toBigDecimal())
    }

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

    val error: InputError? by unsafeLazy {
        enteredFiat?.let {
            if (maxAmount != null && minAmount != null && enteredFiat != null) {
                when {
                    it > maxAmount -> InputError.ABOVE_MAX
                    it < minAmount -> InputError.BELOW_MIN
                    else -> null
                }
            } else null
        }
    }
}

data class ExchangePriceState(
    val price: FiatValue? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)

enum class OrderState {
    UNITIALISED, INITIALISED, CANCELLED, CONFIRMED
}

enum class KycState {
    PENDING, FAILED, UNDECIDED, VERIFIED
}

enum class InputError {
    BELOW_MIN, ABOVE_MAX
}