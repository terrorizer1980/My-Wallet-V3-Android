package piuk.blockchain.androidcore.data.currency

import com.blockchain.annotations.BurnCandidate
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.utils.PersistentPrefs

/**
 * Singleton class to store user's preferred crypto currency state.
 * (ie is Wallet currently showing FIAT, ETH, BTC ot BCH)
 */
@Deprecated("Remove")
@BurnCandidate("Global state is bad.")
class CurrencyState(private val prefs: PersistentPrefs) {

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

    var cryptoCurrency: CryptoCurrency by CurrencyPreference(
        prefs,
        PersistentPrefs.KEY_CURRENCY_CRYPTO_STATE,
        defaultCurrency = CryptoCurrency.BTC
    )
}
