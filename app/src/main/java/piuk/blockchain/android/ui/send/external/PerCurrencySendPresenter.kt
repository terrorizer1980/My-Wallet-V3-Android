package piuk.blockchain.android.ui.send.external

import android.annotation.SuppressLint
import android.content.Intent
import android.text.Editable
import android.widget.EditText
import com.blockchain.sunriver.isValidXlmQr
import com.blockchain.transactions.Memo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValueOrZero
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.send.DisplayFeeOptions
import piuk.blockchain.android.util.EditTextFormatUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.exchangerate.toCrypto
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import timber.log.Timber

/**
 * Does some of the basic work, using the [BaseSendView] interface.
 * Delegates the rest of the work to one of the [SendPresenterStrategy] supplied to it depending on currency.
 */
internal class PerCurrencySendPresenter<View : BaseSendView>(
    private val originalStrategy: SendPresenterStrategy<View>,
    private val xlmStrategy: SendPresenterStrategy<View>,
    private val erc20Strategy: SendPresenterStrategy<View>,
    private val currencyState: CurrencyState,
    private val exchangeRates: FiatExchangeRates,
    private val stringUtils: StringUtils,
    private val exchangeRateFactory: ExchangeRateDataManager
) : SendPresenter<View>() {

    override fun getFeeOptionsForDropDown(): List<DisplayFeeOptions> {
        val regular = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_regular),
            stringUtils.getString(R.string.fee_options_regular_time)
        )
        val priority = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_priority),
            stringUtils.getString(R.string.fee_options_priority_time)
        )
        val custom = DisplayFeeOptions(
            stringUtils.getString(R.string.fee_options_custom),
            stringUtils.getString(R.string.fee_options_custom_warning)
        )
        return listOf(regular, priority, custom)
    }

    private fun delegate(): SendPresenterStrategy<View> =
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> originalStrategy
            CryptoCurrency.ETHER -> originalStrategy
            CryptoCurrency.BCH -> originalStrategy
            CryptoCurrency.XLM -> xlmStrategy
            CryptoCurrency.PAX -> erc20Strategy
        }

    override fun onContinueClicked() = delegate().onContinueClicked()

    override fun onSpendMaxClicked() = delegate().onSpendMaxClicked()

    override fun onBroadcastReceived() {
        updateTicker()
        delegate().onBroadcastReceived()
    }

    @SuppressLint("CheckResult")
    private fun updateTicker() {
        exchangeRateFactory.updateTickers()
            .addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    override fun onResume() {
        delegate().onResume()
    }

    override fun onCurrencySelected(currency: CryptoCurrency) {
        updateTicker()
        view?.setSelectedCurrency(currency)
        delegate().onCurrencySelected(currency)
    }

    override fun handleURIScan(untrimmedscanData: String?) {
        if (untrimmedscanData?.isValidXlmQr() == true) {
            currencyState.cryptoCurrency = CryptoCurrency.XLM
            onCurrencySelected(CryptoCurrency.XLM)
            xlmStrategy.handleURIScan(untrimmedscanData)
        } else {
            originalStrategy.handleURIScan(untrimmedscanData)
        }
    }

    override fun handlePrivxScan(scanData: String?) = delegate().handlePrivxScan(scanData)

    override fun clearReceivingObject() = delegate().clearReceivingObject()

    override fun selectSendingAccount(data: Intent?, currency: CryptoCurrency) =
        delegate().selectSendingAccount(data, currency)

    override fun selectReceivingAccount(data: Intent?, currency: CryptoCurrency) =
        delegate().selectReceivingAccount(data, currency)

    override fun updateCryptoTextField(editable: Editable, amountFiat: EditText) {
        val maxLength = 2
        val fiat = EditTextFormatUtil.formatEditable(
            editable,
            maxLength,
            amountFiat,
            getDefaultDecimalSeparator()
        ).toString()

        val fiatValue = FiatValue.fromMajorOrZero(exchangeRates.fiatUnit, fiat)
        val cryptoValue = fiatValue.toCrypto(exchangeRates, currencyState.cryptoCurrency)

        view.updateCryptoAmountWithoutTriggeringListener(cryptoValue)
    }

    override fun updateFiatTextField(editable: Editable, amountCrypto: EditText) {
        val crypto = EditTextFormatUtil.formatEditable(
            editable,
            currencyState.cryptoCurrency.dp,
            amountCrypto,
            getDefaultDecimalSeparator()
        ).toString()

        val cryptoValue = currencyState.cryptoCurrency.withMajorValueOrZero(crypto)
        val fiatValue = cryptoValue.toFiat(exchangeRates)

        view.updateFiatAmountWithoutTriggeringListener(fiatValue)
    }

    override fun selectDefaultOrFirstFundedSendingAccount() = delegate().selectDefaultOrFirstFundedSendingAccount()

    override fun submitPayment() = delegate().submitPayment()

    override fun shouldShowAdvancedFeeWarning() = delegate().shouldShowAdvancedFeeWarning()

    override fun onCryptoTextChange(cryptoText: String) = delegate().onCryptoTextChange(cryptoText)

    override fun onAddressTextChange(address: String) = delegate().onAddressTextChange(address)

    override fun onMemoChange(memo: Memo) = delegate().onMemoChange(memo)

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) =
        delegate().spendFromWatchOnlyBIP38(pw, scanData)

    override fun setWarnWatchOnlySpend(warn: Boolean) = delegate().setWarnWatchOnlySpend(warn)

    override fun onNoSecondPassword() = delegate().onNoSecondPassword()

    override fun onSecondPasswordValidated(validateSecondPassword: String) =
        delegate().onSecondPasswordValidated(validateSecondPassword)

    override fun disableAdvancedFeeWarning() = delegate().disableAdvancedFeeWarning()

    override fun getBitcoinFeeOptions() = delegate().getBitcoinFeeOptions()

    override fun onViewReady() {
        updateTicker()
        view?.updateFiatCurrency(currencyState.fiatUnit)
        view?.updateReceivingHintAndAccountDropDowns(currencyState.cryptoCurrency, 1)
        delegate().onViewReady()
    }

    override fun initView(view: View?) {
        super.initView(view)
        xlmStrategy.initView(view)
        originalStrategy.initView(view)
    }
}
