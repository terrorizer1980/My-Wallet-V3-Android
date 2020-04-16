package piuk.blockchain.android.ui.transactions

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import piuk.blockchain.android.ui.transactions.mapping.TransactionDetailModel
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface TransactionDetailView : View {

    fun pageFinish()

    fun setTransactionType(type: TransactionSummary.Direction, isFeeTransaction: Boolean)

    fun updateFeeFieldVisibility(isVisible: Boolean)

    fun setTransactionValue(value: CryptoValue)

    fun setTransactionValueFiat(fiat: String?)

    fun setToAddresses(addresses: List<TransactionDetailModel>)

    fun setFromAddress(addresses: List<TransactionDetailModel>)

    fun setStatus(cryptoCurrency: CryptoCurrency, status: String?, hash: String)

    fun setFee(fee: String?)

    fun setDate(datetimeMillis: Long)

    fun setDescription(description: String?)

    fun setIsDoubleSpend(isDoubleSpend: Boolean)

    fun setTransactionNote(note: String?)

    fun setTransactionColour(@ColorRes colour: Int)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun onDataLoaded()

    fun showTransactionAsPaid()
}