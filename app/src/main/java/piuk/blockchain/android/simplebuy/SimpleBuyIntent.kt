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

    data class FetchBuyLimits(val currency: String) : SimpleBuyIntent() {
        override fun reduce(oldState: SimpleBuyState): SimpleBuyState = oldState.copy(currency = currency)
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