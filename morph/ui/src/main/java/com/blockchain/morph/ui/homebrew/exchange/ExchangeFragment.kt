package com.blockchain.morph.ui.homebrew.exchange

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.coinIconWhite
import com.blockchain.balance.colorRes
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.ExchangeViewState
import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.Maximums
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.mvi.QuoteValidity
import com.blockchain.morph.exchange.mvi.SimpleFieldUpdateIntent
import com.blockchain.morph.exchange.mvi.ToggleFiatCryptoIntent
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.customviews.CurrencyTextView
import com.blockchain.morph.ui.customviews.ThreePartText
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewHostActivityListener
import com.blockchain.morph.ui.logging.AmountErrorEvent
import com.blockchain.morph.ui.logging.AmountErrorType
import com.blockchain.morph.ui.logging.FixType
import com.blockchain.morph.ui.logging.FixTypeEvent
import com.blockchain.nabu.StartKyc
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.ui.chooserdialog.AccountChooserBottomDialog
import com.blockchain.ui.dialoglinks.URL_BLOCKCHAIN_PAX_NEEDS_ETH_FAQ
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.logging.Logging
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

internal class ExchangeFragment : Fragment() {

    companion object {

        private const val ARGUMENT_CURRENCY = "ARGUMENT_CURRENCY"
        fun bundleArgs(fiatCurrency: String): Bundle =
            Bundle().apply {
                putString(ARGUMENT_CURRENCY, fiatCurrency)
            }
    }

    private val compositeDisposable = CompositeDisposable()
    private val inputTypeRelay = PublishSubject.create<Fix>()
    private val activityListener: HomebrewHostActivityListener by ParentActivityDelegate(this)

    private lateinit var currency: String

    private lateinit var largeValue: CurrencyTextView
    private lateinit var smallValue: TextView
    private lateinit var keyboard: FloatKeyboardView
    private lateinit var selectSendAccountButton: Button
    private lateinit var selectReceiveAccountButton: Button
    private lateinit var exchangeButton: Button
    private lateinit var textViewBalanceTitle: TextView
    private lateinit var textViewBalance: TextView
    private lateinit var textViewBaseRate: TextView
    private lateinit var textViewCounterRate: TextView
    private lateinit var root: ConstraintLayout
    private lateinit var keyboardGroup: ConstraintLayout

    private lateinit var exchangeModel: ExchangeModel

    private lateinit var exchangeLimitState: ExchangeLimitState
    private lateinit var exchangeMenuState: ExchangeMenuState

    private val startKyc: StartKyc by inject()
    private val stringUtils: StringUtils by inject()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val provider = (context as? ExchangeViewModelProvider)
            ?: throw Exception("Host activity must support ExchangeViewModelProvider")
        exchangeLimitState = (context as? ExchangeLimitState)
            ?: throw Exception("Host activity must support ExchangeLimitState")
        exchangeMenuState = (context as? ExchangeMenuState)
            ?: throw Exception("Host activity must support ExchangeMenuState")
        exchangeModel = provider.exchangeViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_homebrew_exchange)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityListener.setToolbarTitle(R.string.morph_new_exchange)
        logEvent(AnalyticsEvents.ExchangeCreate)

        currency = arguments?.getString(ARGUMENT_CURRENCY) ?: "USD"

        largeValue = view.findViewById(R.id.largeValue)
        smallValue = view.findViewById(R.id.smallValue)
        keyboard = view.findViewById(R.id.numericKeyboard)
        selectSendAccountButton = view.findViewById(R.id.select_from_account_button)
        selectReceiveAccountButton = view.findViewById(R.id.select_to_account_button)
        exchangeButton = view.findViewById(R.id.exchange_action_button)
        textViewBalanceTitle = view.findViewById(R.id.text_view_balance_title)
        textViewBalance = view.findViewById(R.id.text_view_balance_value)
        textViewBaseRate = view.findViewById(R.id.text_view_base_rate)
        textViewCounterRate = view.findViewById(R.id.text_view_counter_rate)
        root = view.findViewById(R.id.constraint_layout_exchange)
        keyboardGroup = view.findViewById(R.id.layout_keyboard_group)

        selectSendAccountButton.setOnClickListener {
            AccountChooserBottomDialog.create(
                title = getString(R.string.dialog_title_exchange),
                resultId = REQUEST_CODE_CHOOSE_SENDING_ACCOUNT
            ).show(fragmentManager, "BottomDialog")
        }
        selectReceiveAccountButton.setOnClickListener {
            AccountChooserBottomDialog.create(
                title = getString(R.string.dialog_title_receive),
                resultId = REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT
            ).show(fragmentManager, "BottomDialog")
        }
        exchangeButton.setOnClickListener {
            exchangeModel.fixAsCrypto()
            activityListener.launchConfirmation()
        }
        largeValue.setOnClickListener(toggleOnClickListener)
        smallValue.setOnClickListener(toggleOnClickListener)
    }

    val toggleOnClickListener = View.OnClickListener {
        exchangeModel.inputEventSink.onNext(ToggleFiatCryptoIntent())
    }

    override fun onResume() {
        super.onResume()

        keyboard.setMaximums(
            Maximums(
                maxDigits = 11,
                maxIntLength = 6
            )
        )

        compositeDisposable += allTextUpdates().distinctUntilChanged()
            .subscribeBy { exchangeModel.inputEventSink.onNext(it) }

        compositeDisposable += exchangeModel
            .exchangeViewStates
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                when (it.fix) {
                    Fix.BASE_FIAT -> displayFiatLarge(it.fromFiat, it.fromCrypto, it.decimalCursor)
                    Fix.BASE_CRYPTO -> displayCryptoLarge(it.fromCrypto, it.fromFiat, it.decimalCursor)
                    Fix.COUNTER_FIAT -> displayFiatLarge(it.toFiat, it.toCrypto, it.decimalCursor)
                    Fix.COUNTER_CRYPTO -> displayCryptoLarge(it.toCrypto, it.toFiat, it.decimalCursor)
                }

                inputTypeRelay.onNext(it.fix)

                selectSendAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.fromCrypto)
                selectReceiveAccountButton.setButtonGraphicsAndTextFromCryptoValue(it.toCrypto)

                keyboard.setValue(it.lastUserValue.userDecimalPlaces, it.lastUserValue.toBigDecimal())
                exchangeButton.isEnabled = it.isValid()

                updateUserFeedBack(it)
                updateExchangeRate(it)
                updateBalance(it)
            }

        compositeDisposable += inputTypeRelay.map { it.toLoggingFixType() }
            .distinctUntilChanged()
            .subscribeBy {
                Logging.logCustom(FixTypeEvent(it))
            }

        exchangeModel.fixAsFiat()
    }

    private fun updateBalance(exchangeViewState: ExchangeViewState) {
        exchangeViewState.apply {
            textViewBalanceTitle.text =
                getString(R.string.morph_balance_title, fromCrypto.currencyCode)
            textViewBalance.text = formatSpendableString()
        }
    }

    private fun updateExchangeRate(exchangeViewState: ExchangeViewState) {
        textViewBaseRate.text = exchangeViewState.formatBase()
        textViewCounterRate.text = exchangeViewState.latestQuote?.let {
            exchangeViewState.formatCounter(it)
        } ?: ""
    }

    private fun updateUserFeedBack(exchangeViewState: ExchangeViewState) {
        val error = exchangeViewState.exchangeError()

        // Set menu state
        val errorState = error?.let {
            ExchangeMenuState.ExchangeMenu.Error(it)
        } ?: ExchangeMenuState.ExchangeMenu.Help

        exchangeMenuState.setMenuState(errorState)
    }

    private fun displayFiatLarge(fiatValue: FiatValue, cryptoValue: CryptoValue, decimalCursor: Int) {
        val parts = fiatValue.toStringParts()
        largeValue.setText(ThreePartText(parts.symbol,
            parts.major,
            if (decimalCursor != 0) parts.minor else ""))

        val fromCryptoString = cryptoValue.toStringWithSymbol()
        smallValue.text = fromCryptoString
    }

    @SuppressLint("SetTextI18n")
    private fun displayCryptoLarge(cryptoValue: CryptoValue, fiatValue: FiatValue, decimalCursor: Int) {
        largeValue.setText(ThreePartText("",
            cryptoValue.formatExactly(decimalCursor) + " " + cryptoValue.symbol(),
            ""))

        val fromFiatString = fiatValue.toStringWithSymbol()
        smallValue.text = fromFiatString
    }

    private fun allTextUpdates(): Observable<ExchangeIntent> {
        return keyboard.viewStates
            .doOnNext {
                if (it.shake) {
                    val animShake = AnimationUtils.loadAnimation(
                        requireContext(),
                        R.anim.fingerprint_failed_shake
                    )
                    largeValue.startAnimation(animShake)
                }
                view!!.findViewById<View>(R.id.numberBackSpace).isEnabled = it.previous != null
            }
            .map {
                SimpleFieldUpdateIntent(it.userDecimal, it.decimalCursor)
            }
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private val customCryptoEntryFormat: DecimalFormat =
        (NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat)

    private fun CryptoValue.formatExactly(decimalPlacesForCrypto: Int): String {
        val show = when (decimalPlacesForCrypto) {
            0, 1 -> decimalPlacesForCrypto
            else -> decimalPlacesForCrypto - 1
        }
        return customCryptoEntryFormat
            .apply {
                minimumFractionDigits = show
                maximumFractionDigits = decimalPlacesForCrypto
            }.format(toMajorUnitDouble())
    }

    private fun ExchangeViewState.exchangeError(): ExchangeMenuState.ExchangeMenuError? {
        logMinMaxErrors()
        val validity = validity()
        exchangeLimitState.setOverTierLimit(validity == QuoteValidity.OverTierLimit)
        return when (validity) {
            QuoteValidity.Valid,
            QuoteValidity.NoQuote,
            QuoteValidity.MissMatch -> null
            QuoteValidity.NotEnoughFees -> {
                val linksMap = mapOf<String, Uri>(
                    "pax_faq" to Uri.parse(URL_BLOCKCHAIN_PAX_NEEDS_ETH_FAQ)
                )
                val body = stringUtils.getStringWithMappedLinks(R.string.pax_need_more_eth_error_body, linksMap)
                return ExchangeMenuState.ExchangeMenuError(
                    CryptoCurrency.ETHER,
                    userTier,
                    getString(R.string.pax_need_more_eth_error_title),
                    body,
                    ExchangeMenuState.ErrorType.TRADE
                )
            }
            QuoteValidity.UnderMinTrade -> ExchangeMenuState.ExchangeMenuError(
                fromCrypto.currency,
                userTier,
                getString(R.string.below_trading_limit),
                getString(R.string.under_min, minTradeLimit?.formatOrSymbolForZero()),
                ExchangeMenuState.ErrorType.TRADE
            )
            QuoteValidity.OverMaxTrade -> ExchangeMenuState.ExchangeMenuError(
                fromCrypto.currency,
                userTier,
                getString(R.string.above_trading_limit),
                getString(R.string.over_max, maxTradeLimit?.formatOrSymbolForZero()),
                ExchangeMenuState.ErrorType.TRADE
            )
            QuoteValidity.OverTierLimit -> ExchangeMenuState.ExchangeMenuError(
                fromCrypto.currency,
                userTier,
                getString(R.string.above_trading_limit),
                getString(R.string.over_max, maxTierLimit?.formatOrSymbolForZero()),
                ExchangeMenuState.ErrorType.TIER
            )
            QuoteValidity.OverUserBalance -> {
                val maxSpendableFiat = maxSpendableFiatBalance()
                ExchangeMenuState.ExchangeMenuError(
                    fromCrypto.currency,
                    userTier,
                    getString(R.string.not_enough_balance_for_coin, fromCrypto.currency.symbol),
                    getString(R.string.not_enough_balance,
                        maxSpendable?.toStringWithSymbol(),
                        maxSpendableFiat),
                    ExchangeMenuState.ErrorType.BALANCE
                )
            }
        }
    }

    private fun ExchangeViewState.logMinMaxErrors() {
        val errorType = when (validity()) {
            QuoteValidity.Valid,
            QuoteValidity.NotEnoughFees,
            QuoteValidity.NoQuote,
            QuoteValidity.MissMatch -> null
            QuoteValidity.UnderMinTrade -> AmountErrorType.UnderMin
            QuoteValidity.OverMaxTrade -> AmountErrorType.OverMax
            QuoteValidity.OverTierLimit -> AmountErrorType.OverMax
            QuoteValidity.OverUserBalance -> AmountErrorType.OverBalance
        }

        errorType?.let { Logging.logCustom(AmountErrorEvent(it)) }
    }

    private fun ExchangeViewState.formatSpendableString(): CharSequence {
        val cryptoCurrency = fromCrypto.currency
        val fiatCode = fromFiat.currencyCode
        val spendable = maxSpendable ?: CryptoValue.zero(cryptoCurrency)

        val spendableString = SpannableStringBuilder()

        latestQuote?.baseToFiatRate?.let { baseToFiatRate ->
            val fiatSpendable = ExchangeRate.CryptoToFiat(cryptoCurrency, fiatCode, baseToFiatRate)
                .applyRate(spendable)

            fiatSpendable?.let {
                val fiatString = SpannableString(it.toStringWithSymbol())
                fiatString.setSpan(
                    ForegroundColorSpan(getResolvedColor(R.color.product_green_medium)),
                    0,
                    fiatString.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spendableString.append(fiatString)
                spendableString.append(" ")
            }
        }
        spendableString.append(spendable.toStringWithSymbol())
        return spendableString
    }

    private fun ExchangeViewState.maxSpendableFiatBalance(): CharSequence {
        val cryptoCurrency = fromCrypto.currency
        val fiatCode = fromFiat.currencyCode
        val spendable = maxSpendable ?: CryptoValue.zero(cryptoCurrency)

        val fiat = latestQuote?.baseToFiatRate?.let { baseToFiatRate ->
            ExchangeRate.CryptoToFiat(cryptoCurrency, fiatCode, baseToFiatRate)
                .applyRate(spendable)
        } ?: FiatValue.zero(fiatCode)

        return fiat.toStringWithSymbol()
    }

    private fun ExchangeViewState.formatBase(): String =
        "1 ${fromCrypto.currencyCode} ="

    private fun ExchangeViewState.formatCounter(quote: Quote): String =
        "${quote.baseToCounterRate} ${toCrypto.currencyCode}"

    private fun Fix.toLoggingFixType(): FixType = when (this) {
        Fix.BASE_FIAT -> FixType.BaseFiat
        Fix.BASE_CRYPTO -> FixType.BaseCrypto
        Fix.COUNTER_FIAT -> FixType.CounterFiat
        Fix.COUNTER_CRYPTO -> FixType.CounterCrypto
    }

    private fun Button.setButtonGraphicsAndTextFromCryptoValue(cryptoValue: CryptoValue) {
        val fromCryptoString = cryptoValue.formatOrSymbolForZero()
        setBackgroundResource(cryptoValue.currency.colorRes())
        setCryptoLeftImageIfZero(cryptoValue)
        text = fromCryptoString
    }

    private fun Button.setCryptoLeftImageIfZero(cryptoValue: CryptoValue) {
        if (cryptoValue.isZero) {
            val drawable = ContextCompat.getDrawable(this.context, cryptoValue.currency.coinIconWhite())
            setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        } else {
            setCompoundDrawables(null, null, null, null)
        }
    }

    private fun addLink(prefixText: String, link: String): Pair<CharSequence, TextView.BufferType> {
        val finalString = "$prefixText. $link"
        val spannableString = SpannableString(finalString)

        val span = object : ClickableSpan() {
            override fun onClick(widget: View?) {
                startKyc.startKycActivity(requireContext())
            }
        }

        val startIndexOfLink = finalString.indexOf(link)
        spannableString.setSpan(
            span,
            startIndexOfLink,
            startIndexOfLink + link.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString to TextView.BufferType.SPANNABLE
    }
}

private fun Money.formatOrSymbolForZero() =
    if (isZero) {
        symbol()
    } else {
        toStringWithSymbol()
    }

internal const val REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT = 800
internal const val REQUEST_CODE_CHOOSE_SENDING_ACCOUNT = 801
