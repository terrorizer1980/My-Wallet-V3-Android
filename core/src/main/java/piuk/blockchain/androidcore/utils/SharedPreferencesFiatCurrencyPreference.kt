package piuk.blockchain.androidcore.utils

import com.blockchain.annotations.BurnCandidate
import com.blockchain.preferences.FiatCurrencyPreference

@BurnCandidate("This does nothing")
internal class SharedPreferencesFiatCurrencyPreference(
    private val prefs: PersistentPrefs
) : FiatCurrencyPreference {

    override val fiatCurrencyPreference: String
        get() = prefs.selectedFiatCurrency
}
