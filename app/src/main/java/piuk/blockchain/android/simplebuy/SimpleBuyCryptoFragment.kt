package piuk.blockchain.android.simplebuy

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
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
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
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
        model.process(SimpleBuyIntent.FetchPredefinedAmounts(currencyPrefs.selectedFiatCurrency))

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
        input_amount.clearFocus()
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
            up_to_amount.visible()
            up_to_amount.text = getString(R.string.simple_buy_up_to_amount, newState.maxAmount.formatOrSymbolForZero())
        }

        newState.predefinedAmounts.takeIf { it.isNotEmpty() }?.let { values ->
            predefined_amount_1.asPredefinedAmountText(values[0])
            predefined_amount_2.asPredefinedAmountText(values[1])
            predefined_amount_3.asPredefinedAmountText(values[2])
            predefined_amount_4.asPredefinedAmountText(values[3])
        } ?: kotlin.run {
            predefined_amount_1.gone()
            predefined_amount_2.gone()
            predefined_amount_3.gone()
            predefined_amount_4.gone()
        }

        btn_continue.isEnabled = newState.isAmountValid()
        error_icon.goneIf(newState.error == null)
        input_layout_amount.error = if (newState.error != null) " " else null
        newState.error?.let {
            handleError(it, newState)
        } ?: kotlin.run {
            error_action.gone()
        }

        if (input_amount.text.toString() != newState.enteredAmount) {
            input_amount.setText(newState.enteredAmount)
        }
    }

    private fun handleError(error: InputError, state: SimpleBuyState) {
        when (error) {
            InputError.ABOVE_MAX -> {
                error_action.apply {
                    text = resources.getString(R.string.use_max)
                    visible()
                    setOnClickListener {
                        input_amount.setText(state.maxAmount?.asInputAmount() ?: "")
                    }
                }
                up_to_amount.text = resources.getString(R.string.too_high)
            }
            InputError.BELOW_MIN -> {
                error_action.apply {
                    text = resources.getString(R.string.use_min)
                    visible()
                    setOnClickListener { input_amount.setText(state.minAmount?.asInputAmount() ?: "") }
                }
                up_to_amount.text = resources.getString(R.string.too_low)
            }
        }
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

    private fun FiatValue.asInputAmount(): String =
        this.toStringWithoutSymbol().replace(",", "")

    private fun AppCompatTextView.asPredefinedAmountText(amount: FiatValue) {
        text = amount.formatOrSymbolForZero().removeSuffix(".00")
        visible()
        setOnClickListener { input_amount.setText(amount.toStringWithoutSymbol()) }
    }
}

interface CurrencyChangeListener {
    fun onCurrencyChanged(currency: CryptoCurrency)
}