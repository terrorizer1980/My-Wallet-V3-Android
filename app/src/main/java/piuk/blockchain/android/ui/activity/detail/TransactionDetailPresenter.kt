package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.util.Locale

class TransactionDetailPresenter constructor(
    private val coincore: Coincore,
    private val inputOutputMapper: TransactionInOutMapper,
    private val stringUtils: StringUtils,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    prefs: PersistentPrefs
) : BasePresenter<TransactionDetailView>() {

    private val fiatType = prefs.selectedFiatCurrency

    private lateinit var activityItem: NonCustodialActivitySummaryItem

    fun showDetailsForTransaction(crypto: CryptoCurrency, txId: String) {
        view?.let {
            if (txId.isEmpty()) {
                it.pageFinish()
            } else {
                coincore[crypto].findCachedActivityItem(txId)?.let { item ->
                    if (item is NonCustodialActivitySummaryItem) {
                        activityItem = item
                        updateUiFromTransaction(item)
                    } else {
                        Timber.e("TransactionDetailView only displays non-custodial events")
                        it.pageFinish()
                    }
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

    private fun updateUiFromTransaction(summaryItem: NonCustodialActivitySummaryItem) {
        with(summaryItem) {
            view?.setTransactionType(direction, isFeeTransaction)
            view?.updateFeeFieldVisibility(
                direction != TransactionSummary.Direction.RECEIVED && !isFeeTransaction
            )

            view?.setTransactionColour(this.formatting().directionColour)
            view?.setTransactionValue(cryptoValue)
            view?.setDescription(description)
            view?.setDate(timeStampMs)

            setConfirmationStatus(cryptoCurrency, txId, confirmations)

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
            .subscribe { view?.setFee(it.toStringWithSymbol()) }
    }

    private fun setConfirmationStatus(cryptoCurrency: CryptoCurrency, txHash: String, confirmations: Int) {
        if (confirmations >= cryptoCurrency.requiredConfirmations) {
            view?.setStatus(cryptoCurrency, stringUtils.getString(R.string.transaction_detail_confirmed), txHash)
        } else {
            var pending = stringUtils.getString(R.string.transaction_detail_pending)
            pending =
                String.format(Locale.getDefault(), pending, confirmations, cryptoCurrency.requiredConfirmations)
            view?.setStatus(cryptoCurrency, pending, txHash)
        }
    }

    private fun getTransactionValueString(fiat: String, transaction: NonCustodialActivitySummaryItem): Single<String> =
        exchangeRateDataManager.getHistoricPrice(
            transaction.cryptoValue,
            fiat,
            transaction.timeStampMs / 1000
        ).map { getTransactionString(transaction, it) }

    private fun getTransactionString(transaction: NonCustodialActivitySummaryItem, value: FiatValue): String {
        val stringId = when (transaction.direction) {
            TransactionSummary.Direction.TRANSFERRED -> R.string.transaction_detail_value_at_time_transferred
            TransactionSummary.Direction.SENT -> R.string.transaction_detail_value_at_time_sent
            TransactionSummary.Direction.RECEIVED -> R.string.transaction_detail_value_at_time_received
            TransactionSummary.Direction.BUY -> R.string.transaction_detail_value_at_time_bought
            TransactionSummary.Direction.SELL -> R.string.transaction_detail_value_at_time_sold
            TransactionSummary.Direction.SWAP -> R.string.transaction_detail_value_at_time_swapped
        }
        return stringUtils.getString(stringId) + value.toStringWithSymbol()
    }
}
