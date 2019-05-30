package piuk.blockchain.android.ui.swap.homebrew.exchange.confirmation

import android.annotation.SuppressLint
import com.blockchain.datamanagers.TransactionExecutorWithoutFees
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.TradeExecutionService
import com.blockchain.morph.exchange.service.TradeTransaction
import com.blockchain.payload.PayloadDecrypt
import com.blockchain.transactions.Memo
import com.blockchain.transactions.SendException
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.exceptions.TransactionHashApiException
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.ethereum.exceptions.TransactionInProgressException
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.math.BigInteger

class ExchangeConfirmationPresenter internal constructor(
    private val transactionExecutor: TransactionExecutorWithoutFees,
    private val tradeExecutionService: TradeExecutionService,
    private val payloadDecrypt: PayloadDecrypt
) : BasePresenter<ExchangeConfirmationView>() {

    private var showPaxAirdropBottomDialog: Boolean = false
    private var executeTradeSingle: Single<String>? = null

    override fun onViewReady() {
        // Ensure user hasn't got a double encrypted wallet
        subscribeToViewState()
    }

    private fun subscribeToViewState() {
        compositeDisposable +=
            view.exchangeViewState
                .flatMapSingle { state ->

                    // State is latest value of behaviour subject.
                    // NOT the state when confirm displayed
                    showPaxAirdropBottomDialog = state.isPowerPaxTagged
                    if (!payloadDecrypt.isDoubleEncrypted) {
                        executeTrade(state.latestQuote!!, state.fromAccount, state.toAccount)
                    } else {
                        view.showSecondPasswordDialog()
                        executeTradeSingle = executeTrade(state.latestQuote!!, state.fromAccount, state.toAccount)
                        Single.never()
                    }
                }
                .retry()
                .subscribeBy(onError = { Timber.e(it) })
    }

    internal fun updateFee(
        amount: CryptoValue,
        sendingAccount: AccountReference
    ) {
        compositeDisposable +=
            transactionExecutor.getFeeForTransaction(amount, sendingAccount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { view.updateFee(it) },
                    onError = {
                        Timber.e(it)
                        view.showToast(R.string.homebrew_confirmation_error_fetching_fee, ToastCustom.TYPE_ERROR)
                    }
                )
    }

    private fun executeTrade(
        quote: Quote,
        sendingAccount: AccountReference,
        receivingAccount: AccountReference
    ): Single<String> {
        return deriveAddressPair(sendingAccount, receivingAccount)
            .subscribeOn(Schedulers.io())
            .flatMap { (destination, refund) ->
                tradeExecutionService.executeTrade(quote, destination, refund)
                    .subscribeOn(Schedulers.io())
                    .flatMap { transaction ->
                        sendFundsForTrade(transaction, sendingAccount)
                            .subscribeOn(Schedulers.io())
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError {
                Timber.e(it)
                if (it is TransactionInProgressException) {
                    view.displayErrorDialog(R.string.morph_confirmation_eth_pending)
                } else {
                    view.displayErrorDialog(R.string.execution_error_message)
                }
            }
            .doOnSuccess {
                view.showExchangeCompleteDialog(
                    showPaxAirdropBottomDialog && receivingAccount.cryptoCurrency == CryptoCurrency.PAX
                )
            }
    }

    private fun sendFundsForTrade(
        transaction: TradeTransaction,
        sendingAccount: AccountReference
    ): Single<String> {
        updateDiagnostics()
        return transactionExecutor.executeTransaction(
            transaction.deposit,
            transaction.depositAddress,
            sendingAccount,
            memo = transaction.memo()
        ).onErrorResumeNext {
            Timber.e(it, "Transaction execution error, telling nabu")
            val hash = (it as? TransactionHashApiException)?.hashString ?: (it as? SendException)?.hash
            tradeExecutionService.putTradeFailureReason(transaction, hash, it.message)
                .andThen(Single.error(it))
        }
    }

    @SuppressLint("CheckResult")
    private fun updateDiagnostics() {
        view.exchangeViewState
            .observeOn(AndroidSchedulers.mainThread())
            .addToCompositeDisposable(this)
            .subscribeBy(onNext = {
                tradeExecutionService.updateDiagnotics(
                    maxAvailable = it.maxSpendable?.amount ?: BigInteger.valueOf(-1),
                    tradeValueCrypto = it.fromCrypto.amount,
                    tradeValueFiat = it.fromFiat.toBigDecimal()
                )
            })
    }

    private fun TradeTransaction.memo() = depositTextMemo?.let {
        Memo(
            value = it,
            type = "text"
        )
    }

    private fun deriveAddressPair(
        sendingAccount: AccountReference,
        receivingAccount: AccountReference
    ): Single<Pair<String, String>> = Singles.zip(
        transactionExecutor.getReceiveAddress(
            receivingAccount
        ),
        transactionExecutor.getReceiveAddress(
            sendingAccount
        )
    )

    internal fun onSecondPasswordValidated(validatedSecondPassword: String) {
        compositeDisposable +=
            decryptPayload(validatedSecondPassword)
                .andThen(decryptBch())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(executeTradeSingle?.ignoreElement())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError { Timber.e(it) }
                .subscribe()
    }

    private fun decryptPayload(validatedSecondPassword: String): Completable =
        Completable.fromCallable {
            payloadDecrypt.decryptHDWallet(
                validatedSecondPassword
            )
        }

    private fun decryptBch(): Completable = Completable.fromCallable {
        payloadDecrypt.decryptWatchOnlyWallet()
    }
}
