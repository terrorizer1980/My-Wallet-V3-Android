package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.synthetic.main.fragment_simple_buy_buy_crypto.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.ui.dashboard.format
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.util.drawableResFilled
import piuk.blockchain.androidcoreui.utils.DecimalDigitsInputFilter
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import java.util.Locale
import java.util.Currency

class SimpleBuyCryptoFragment : MviFragment<SimpleBuyModel, SimpleBuyIntent, SimpleBuyState>(), SimpleBuyScreen,
    CurrencyChangeListener {

    override val model: SimpleBuyModel by inject()

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator) ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    private val currencyPrefs: CurrencyPrefs by inject()

    private val fiatSymbol: String
        get() = Currency.getInstance(currencyPrefs.selectedFiatCurrency).getSymbol(Locale.getDefault())

    override fun onBackPressed(): Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_simple_buy_buy_crypto)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.setupToolbar(R.string.simple_buy_buy_crypto_title)
        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(CryptoCurrency.BTC))
        model.process(SimpleBuyIntent.FetchBuyLimits(currencyPrefs.selectedFiatCurrency))

        crypto_text.setOnClickListener {
            showBottomSheet(CryptoCurrencyChooserBottomSheet())
        }

        fiat_currency_symbol.text = fiatSymbol

        input_amount.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(s: Editable?) {
                model.process(SimpleBuyIntent.EnteredAmount(s.toString()))
            }
        })

        btn_continue.setOnClickListener {
            KycNavHostActivity.startForResult(requireActivity(), CampaignType.SimpleBuy, SimpleBuyActivity.KYC_STARTED)
        }
    }

    override fun onCurrencyChanged(currency: CryptoCurrency) {

        model.process(SimpleBuyIntent.NewCryptoCurrencySelected(currency))
    }

    override fun render(newState: SimpleBuyState) {
        newState.exchangePriceState?.let {
            renderExchangePrice(newState.selectedCryptoCurrency ?: return@let, it)
        }
        newState.selectedCryptoCurrency?.let {
            crypto_icon.setImageResource(it.drawableResFilled())
            crypto_text.text = it.unit
        }
        if (newState.maxAmount != null && newState.minAmount != null) {
            input_amount.filters =
                arrayOf(DecimalDigitsInputFilter(newState.maxIntegerDigitsForAmount(),
                    newState.maxDecimalDigitsForAmount()))
        }
        btn_continue.isEnabled = newState.isAmountValid()
    }

    private fun renderExchangePrice(currency: CryptoCurrency, exchangePriceState: ExchangePriceState) {
        prices_loading.goneIf(!exchangePriceState.isLoading)
        exchange_price.goneIf(exchangePriceState.isLoading)
        exchange_price.text = exchangePriceState.price?.let {
            "1 ${currency.symbol} =  ${it.format(fiatSymbol)}"
        }

        if (exchangePriceState.hasError) {
            // todo Discuss with design what text exactly should be here and colors?
            exchange_price.text = resources.getString(R.string.simple_buy_buy_crypto_title)
        }
    }
}

interface CurrencyChangeListener {
    fun onCurrencyChanged(currency: CryptoCurrency)
}