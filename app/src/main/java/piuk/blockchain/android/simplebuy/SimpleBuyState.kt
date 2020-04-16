package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
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
    val enteredAmount: String = "", // Major units
    val fiatCurrency: String = "USD",
    val predefinedAmounts: List<FiatValue> = emptyList(),
    val selectedCryptoCurrency: CryptoCurrency? = null,
    val orderState: OrderState = OrderState.UNINITIALISED,
    private val expirationDate: Date? = null,
    val quote: Quote? = null,
    val kycStartedButNotCompleted: Boolean = false,
    val kycVerificationState: KycState? = null,
    val bankAccount: BankAccount? = null,
    val currentScreen: FlowScreen = FlowScreen.INTRO,
    val supportedFiatCurrencies: List<String> = emptyList(),
    @Transient val errorState: ErrorState? = null,
    // we use this flag to avoid navigating back and forth, reset after navigating
    @Transient val confirmationActionRequested: Boolean = false
) : MviState {

    @delegate:Transient
    val order: SimpleBuyOrder by unsafeLazy {
        SimpleBuyOrder(
            orderState,
            amount,
            expirationDate,
            quote
        )
    }

    @Transient
    private val pattern = Pattern.compile("-?\\d+(\\.\\d+)?")

    @delegate:Transient
    val availableCryptoCurrencies: List<CryptoCurrency> by unsafeLazy {
        supportedPairsAndLimits?.filter { it.fiatCurrency == fiatCurrency }?.map { it.cryptoCurrency }?.distinct()
            ?: emptyList()
    }

    @delegate:Transient
    private val amount: FiatValue? by unsafeLazy {
        if (enteredAmount.isEmpty() || pattern.matcher(enteredAmount).matches().not()) null else
            FiatValue.fromMajor(fiatCurrency, enteredAmount.toBigDecimal())
    }

    @delegate:Transient
    val maxAmount: FiatValue? by unsafeLazy {
        supportedPairsAndLimits?.find { it.cryptoCurrency == selectedCryptoCurrency && it.fiatCurrency == fiatCurrency }
            ?.buyLimits?.maxLimit(fiatCurrency)
    }

    @delegate:Transient
    val minAmount: FiatValue? by unsafeLazy {
        supportedPairsAndLimits?.find { it.cryptoCurrency == selectedCryptoCurrency && it.fiatCurrency == fiatCurrency }
            ?.buyLimits?.minLimit(fiatCurrency)
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

enum class KycState {
    /** Docs submitted for Gold and state is pending. Or kyc backend query returned an error  */
    PENDING,
    /** Docs processed, failed kyc. Not error state. */
    FAILED,
    /** Docs processed under manual review */
    IN_REVIEW,
    /** Docs submitted, no result know from server yet */
    UNDECIDED,
    /** Docs uploaded, processed and kyc passed. Eligible for simple buy */
    VERIFIED_AND_ELIGIBLE,
    /** Docs uploaded, processed and kyc passed. User is NOT eligible for simple buy. */
    VERIFIED_BUT_NOT_ELIGIBLE;

    fun verified() = this == VERIFIED_AND_ELIGIBLE || this == VERIFIED_BUT_NOT_ELIGIBLE

    fun docsSubmitted() = this != PENDING
}

enum class FlowScreen {
    INTRO, CURRENCY_SELECTOR, ENTER_AMOUNT, KYC, KYC_VERIFICATION, CHECKOUT, BANK_DETAILS
}

enum class InputError {
    BELOW_MIN, ABOVE_MAX
}

sealed class ErrorState {
    object GenericError : ErrorState()
    object NoAvailableCurrenciesToTrade : ErrorState()
}

data class SimpleBuyOrder(
    val orderState: OrderState = OrderState.UNINITIALISED,
    val amount: FiatValue? = null,
    val expirationDate: Date? = null,
    val quote: Quote? = null
)
