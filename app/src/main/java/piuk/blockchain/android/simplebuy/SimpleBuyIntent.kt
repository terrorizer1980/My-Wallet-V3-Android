package piuk.blockchain.android.simplebuy

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class SimpleBuyIntent : MviIntent<SimpleBuyState> {
    class NewCryptoCurrencySelected(val currency: CryptoCurrency) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(selectedCryptoCurrency = currency, enteredAmount = "")
    }

    class PriceUpdate(private val fiatValue: FiatValue) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(exchangePriceState = ExchangePriceState(price = fiatValue))
    }

    class EnteredAmount(private val amount: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(enteredAmount = amount)
    }

    data class BuyLimits(private val min: FiatValue, private val max: FiatValue) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState {
            return oldState.copy(minAmount = min, maxAmount = max)
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

    data class FetchPredefinedAmounts(val currency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(currency = currency, predefinedAmounts = emptyList())
    }

    object CancelOrder : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState
    }

    object ConfirmOrder : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState
    }

    object FetchBankAccount : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState // add loading state here
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

    object PriceLoading : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(exchangePriceState = ExchangePriceState(isLoading = true))
    }

    object PriceError : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState =
            oldState.copy(exchangePriceState = ExchangePriceState(hasError = true))
    }
}