package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.models.simplebuy.BankAccount
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SimpleBuyIntent : MviIntent<SimpleBuyState> {
    class NewCryptoCurrencySelected(val currency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            if (oldState.selectedCryptoCurrency == currency) oldState else
                oldState.copy(selectedCryptoCurrency = currency, enteredAmount = "")
    }

    class EnteredAmount(private val amount: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(enteredAmount = amount)
    }

    object BuyButtonClicked : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true, orderState = OrderState.INITIALISED)
    }

    data class UpdatedBuyLimitsAndSupportedCryptoCurrencies(val simpleBuyPairs: SimpleBuyPairs) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            val supportedPairsAndLimits = simpleBuyPairs.pairs.filter { it.fiatCurrency == oldState.currency }
            val selectedCryptoCurrency = oldState.selectedCryptoCurrency ?: simpleBuyPairs.pairs.firstOrNull {
                it.fiatCurrency == oldState.currency
            }?.cryptoCurrency

            val minValueForSelectedPair = supportedPairsAndLimits.firstOrNull { pairs ->
                pairs.fiatCurrency == oldState.currency &&
                        pairs.cryptoCurrency == selectedCryptoCurrency
            }?.buyLimits?.minLimit(oldState.currency)?.valueMinor

            val maxValueForSelectedPair = supportedPairsAndLimits.firstOrNull { pairs ->
                pairs.fiatCurrency == oldState.currency &&
                        pairs.cryptoCurrency == selectedCryptoCurrency
            }?.buyLimits?.maxLimit(oldState.currency)?.valueMinor

            return oldState.copy(
                supportedPairsAndLimits = supportedPairsAndLimits,
                selectedCryptoCurrency = selectedCryptoCurrency,
                predefinedAmounts = oldState.predefinedAmounts.filter {
                    it.valueMinor >= (minValueForSelectedPair ?: 0) && it.valueMinor <= (maxValueForSelectedPair ?: 0)
                }
            )
        }
    }

    data class UpdatedPredefinedAmounts(private val amounts: List<FiatValue>) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(predefinedAmounts = amounts)
        }
    }

    data class BankAccountUpdated(private val account: BankAccount) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(bankAccount = account)
        }
    }

    data class FetchBuyLimits(val currency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(currency = currency)
    }

    data class FlowCurrentScreen(val flowScreen: FlowScreen) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(currentScreen = flowScreen)
    }

    data class FetchPredefinedAmounts(val currency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(currency = currency, predefinedAmounts = emptyList())
    }

    object CancelOrder : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState
    }

    object ClearState : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState()
    }

    object ConfirmOrder : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = true)
    }

    object FetchBankAccount : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState // add loading state here
    }

    object ConfirmationHandled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(confirmationActionRequested = false)
    }

    object KycStareted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycStartedButNotCompleted = true, currentScreen = FlowScreen.KYC)
    }

    object KycCompleted : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycStartedButNotCompleted = false)
    }

    object FetchKycState : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycVerificationState = KycState.PENDING)
    }

    class KycStateUpdated(val kycState: KycState) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(kycVerificationState = kycState)
    }

    object OrderCanceled : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            SimpleBuyState(orderState = OrderState.CANCELLED)
    }

    object OrderConfirmed : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(orderState = OrderState.CONFIRMED)
    }
}