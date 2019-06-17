package piuk.blockchain.androidcore.data.currency

import com.blockchain.annotations.BurnCandidate
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency

/**
 * Singleton class to store user's preferred crypto currency state.
 * (ie is Wallet currently showing FIAT, ETH, BTC ot BCH)
 */
@Deprecated("Remove")
@BurnCandidate("Global state is bad.")
class CurrencyState(private val prefs: CurrencyPrefs) {

    enum class DisplayMode {
        Crypto,
        Fiat;

        fun toggle() =
            when (this) {
                Crypto -> Fiat
                Fiat -> Crypto
            }
    }

    var displayMode = DisplayMode.Crypto

    @Deprecated("Use displayMode")
    var isDisplayingCryptoCurrency
        get() = displayMode == DisplayMode.Crypto
        set(value) {
            displayMode = if (value) DisplayMode.Crypto else DisplayMode.Fiat
        }

    val fiatUnit: String
        get() = prefs.selectedFiatCurrency

    var cryptoCurrency: CryptoCurrency
        get() = prefs.selectedCryptoCurrency
        set(value) { prefs.selectedCryptoCurrency = value }
}
