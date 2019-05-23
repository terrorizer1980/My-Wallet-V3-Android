package piuk.blockchain.android.ui.send.strategy

import android.support.annotation.CallSuper
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.transactions.Memo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.PaymentSentEvent
import java.math.BigInteger
import java.text.DecimalFormatSymbols

abstract class SendStrategy<View : SendView>(
    protected val currencyState: CurrencyState
) : BasePresenter<View>() {

    @CallSuper
    open fun reset() {
        compositeDisposable.clear()

        view?.let {
            it.setSendButtonEnabled(true)
            it.clearAmount()
            it.clearFeeAmount()
            it.hideMaxAvailable()
            it.updateReceivingAddress("")
            it.hideMemo()
            it.hideInfoLink()
        }
    }

    abstract fun onContinueClicked()

    abstract fun onSpendMaxClicked()

    abstract fun onBroadcastReceived()

    abstract fun onResume()

    abstract fun onCurrencySelected()

    abstract fun processURIScanAddress(address: String)

    abstract fun handlePrivxScan(scanData: String?)

    abstract fun clearReceivingObject()

    abstract fun selectSendingAccount(account: JsonSerializableAccount?)

    abstract fun selectReceivingAccount(account: JsonSerializableAccount?)

    abstract fun selectDefaultOrFirstFundedSendingAccount()

    abstract fun submitPayment()

    abstract fun onAddressTextChange(address: String)

    open fun onMemoChange(memo: Memo) {}

    abstract fun onCryptoTextChange(cryptoText: String)

    abstract fun spendFromWatchOnlyBIP38(pw: String, scanData: String)

    abstract fun onNoSecondPassword()

    abstract fun onSecondPasswordValidated(secondPassword: String)

    abstract fun getFeeOptions(): FeeOptions?

    fun getDefaultDecimalSeparator() = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

    protected fun logPaymentSentEvent(success: Boolean, currency: CryptoCurrency, amount: BigInteger) {
        Logging.logCustom(
            PaymentSentEvent()
                .putSuccess(success)
                .putAmountForRange(CryptoValue(currency, amount))
        )
    }
}
