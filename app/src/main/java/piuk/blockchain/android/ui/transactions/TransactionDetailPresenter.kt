package piuk.blockchain.android.ui.transactions

import androidx.annotation.VisibleForTesting
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.formatWithUnit
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.ui.transactions.adapter.formatting
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.android.ui.transactions.mapping.TransactionInOutMapper
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

class TransactionDetailPresenter constructor(
    private val assetLookup: AssetTokenLookup,
    private val inputOutputMapper: TransactionInOutMapper,
    private val stringUtils: StringUtils,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    prefs: PersistentPrefs
) : BasePresenter<TransactionDetailView>() {

    private val fiatType = prefs.selectedFiatCurrency

    private lateinit var activityItem: ActivitySummaryItem

    fun showDetailsForTransaction(crypto: CryptoCurrency, txHash: String) {
        view?.let {
            if (txHash.isEmpty()) {
                it.pageFinish()
            } else {
                assetLookup[crypto].findCachedActivityItem(txHash)?.let { item ->
                    activityItem = item
                    updateUiFromTransaction(item)
                } ?: it.pageFinish()
            }
        }
    }

    fun updateTransactionNote(description: String) {
        compositeDisposable += activityItem.updateDescription(description)
            .subscribeBy(
                onComplete = {
                    view?.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
                    view?.setDescription(description)
                },
                onError = {
                    view?.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                }
            )
        }

    private fun updateUiFromTransaction(summaryItem: ActivitySummaryItem) {
        with(summaryItem) {
            view?.setTransactionType(direction, isFeeTransaction)
            view?.updateFeeFieldVisibility(
                direction != TransactionSummary.Direction.RECEIVED && !isFeeTransaction
            )

            view?.setTransactionColour(this.formatting().directionColour)
            view?.setTransactionValue(totalCrypto)
            view?.setDescription(description)
            view?.setDate(timeStamp * 1000)

            setConfirmationStatus(cryptoCurrency, hash, confirmations)

            setTransactionFee(fee)

            compositeDisposable += inputOutputMapper.transformInputAndOutputs(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { io ->
                        view?.let {
                            it.setFromAddress(io.inputs)
                            it.setToAddresses(io.outputs)
                        }
                    }
                )

            compositeDisposable += getTransactionValueString(fiatType, this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { value -> view?.setTransactionValueFiat(value) },
                    { view?.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })

            view?.onDataLoaded()
            view?.setIsDoubleSpend(doubleSpend)
        }
    }

    private fun setTransactionFee(fee: Observable<CryptoValue>) {
        compositeDisposable += fee
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                view?.setFee("")
            }
            .subscribe { view?.setFee(it.formatWithUnit()) }
    }

    @VisibleForTesting
    internal fun setConfirmationStatus(cryptoCurrency: CryptoCurrency, txHash: String, confirmations: Int) {
        if (confirmations >= cryptoCurrency.requiredConfirmations) {
            view?.setStatus(cryptoCurrency, stringUtils.getString(R.string.transaction_detail_confirmed), txHash)
        } else {
            var pending = stringUtils.getString(R.string.transaction_detail_pending)
            pending =
                String.format(Locale.getDefault(), pending, confirmations, cryptoCurrency.requiredConfirmations)
            view?.setStatus(cryptoCurrency, pending, txHash)
        }
    }

    @VisibleForTesting
    internal fun getTransactionValueString(fiat: String, transaction: ActivitySummaryItem): Single<String> =
        exchangeRateDataManager.getHistoricPrice(
            transaction.totalCrypto,
            fiat,
            transaction.timeStamp
        ).map { getTransactionString(transaction, it) }

    private fun getTransactionString(transaction: ActivitySummaryItem, value: FiatValue): String {
        val stringId = when (transaction.direction) {
            TransactionSummary.Direction.TRANSFERRED -> R.string.transaction_detail_value_at_time_transferred
            TransactionSummary.Direction.SENT -> R.string.transaction_detail_value_at_time_sent
            TransactionSummary.Direction.RECEIVED -> R.string.transaction_detail_value_at_time_received
        }
        return stringUtils.getString(stringId) + value.toStringWithSymbol()
    }
}
