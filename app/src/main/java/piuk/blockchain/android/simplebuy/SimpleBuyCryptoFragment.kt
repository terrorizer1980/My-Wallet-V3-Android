package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.fragment_simple_buy_buy_crypto.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.util.Locale
import java.util.Currency

class SimpleBuyCryptoFragment : Fragment(), SimpleBuyScreen, CurrencyChangeListener {

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    private val currencyPrefs: CurrencyPrefs by inject()

    override fun onBackPressed(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_buy_crypto)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.simple_buy_buy_crypto_title)
        onCurrencyChanged(CryptoCurrency.BTC)

        arrow.setOnClickListener {
            // TODO will move this, once we extend MVIFragment
            CryptoCurrencyChooserBottomSheet().show(childFragmentManager, "BOTTOM_SHEET")
        }

        fiat_currency_symbol.text =
            Currency.getInstance(currencyPrefs.selectedFiatCurrency).getSymbol(Locale.getDefault())
    }

    override fun onCurrencyChanged(currency: CryptoCurrency) {
        crypto_icon.setImageResource(currency.drawableResFilled())
        crypto_text.text = currency.unit
    }
}

interface CurrencyChangeListener {
    fun onCurrencyChanged(currency: CryptoCurrency)
}