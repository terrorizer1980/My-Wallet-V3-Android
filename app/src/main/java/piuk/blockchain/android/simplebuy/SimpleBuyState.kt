package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.compareTo
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import java.util.Date
import java.util.regex.Pattern

/**
 * This is an object that gets serialized with Gson so any properties that we don't
 * want to get serialized should be tagged as @Transient
 *
 */
data class SimpleBuyState(
    val id: String? = null,
    val supportedPairsAndLimits: List<SimpleBuyPair>? = null,
    val enteredAmount: String = "",
    val currency: String = "USD",
    val predefinedAmounts: List<FiatValue> = emptyList(),
    val selectedCryptoCurrency: CryptoCurrency? = null,
    private val orderState: OrderState = OrderState.UNITIALISED,
    private val expirationDate: Date? = null,
    val kycStartedButNotCompleted: Boolean = false,
    val kycVerificationState: KycState = KycState.PENDING,
    val bankAccount: BankAccount? = null,
    val currentScreen: FlowScreen = FlowScreen.INTRO,
    // we use this flag to avoid navigating back and forth, reset after navigating
    @Transient val confirmationActionRequested: Boolean = false
) : MviState {

    @delegate:Transient
    val order: SimpleBuyOrder by unsafeLazy {
        SimpleBuyOrder(
            orderState,
            amount,
            expirationDate
        )
    }

    @Transient
    private val pattern = Pattern.compile("-?\\d+(\\.\\d+)?")

    @delegate:Transient
    val availableCryptoCurrencies: List<CryptoCurrency> by unsafeLazy {
        supportedPairsAndLimits?.filter { it.fiatCurrency == currency }?.map { it.cryptoCurrency }?.distinct()
            ?: emptyList()
    }

    @delegate:Transient
    private val amount: FiatValue? by unsafeLazy {
        if (enteredAmount.isEmpty() || pattern.matcher(enteredAmount).matches().not()) null else
            FiatValue.fromMajor(currency, enteredAmount.toBigDecimal())
    }

    @delegate:Transient
    val maxAmount: FiatValue? by unsafeLazy {
        supportedPairsAndLimits?.find { it.cryptoCurrency == selectedCryptoCurrency && it.fiatCurrency == currency }
            ?.buyLimits?.maxLimit(currency)
    }

    @delegate:Transient
    val minAmount: FiatValue? by unsafeLazy {
        supportedPairsAndLimits?.find { it.cryptoCurrency == selectedCryptoCurrency && it.fiatCurrency == currency }
            ?.buyLimits?.minLimit(currency)
    }

    fun maxDecimalDigitsForAmount(): Int =
        maxAmount?.userDecimalPlaces ?: 0

    fun maxIntegerDigitsForAmount(): Int =
        maxAmount?.toStringParts()?.major?.length ?: 0

    @delegate:Transient
    val isAmountValid: Boolean by unsafeLazy {
        order.amount?.let {
            if (maxAmount != null && minAmount != null) {
                it <= maxAmount!! && it >= minAmount!!
            } else false
        } ?: false
    }

    @delegate:Transient
    val error: InputError? by unsafeLazy {
        order.amount?.let {
            if (maxAmount != null && minAmount != null) {
                when {
                    it > maxAmount!! -> InputError.ABOVE_MAX
                    it < minAmount!! -> InputError.BELOW_MIN
                    else -> null
                }
            } else null
        }
    }
}

enum class OrderState {
    UNITIALISED, INITIALISED, CANCELLED, CONFIRMED
}

enum class KycState {
    PENDING, FAILED, UNDECIDED, VERIFIED
}

enum class FlowScreen {
    INTRO, ENTER_AMOUNT, KYC, KYC_VERIFICATION, CHECKOUT, BANK_DETAILS
}

enum class InputError {
    BELOW_MIN, ABOVE_MAX
}

data class SimpleBuyOrder(
    val orderState: OrderState = OrderState.UNITIALISED,
    val amount: FiatValue? = null,
    val expirationDate: Date? = null
)
