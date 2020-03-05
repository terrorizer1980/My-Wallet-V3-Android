package piuk.blockchain.android.ui.send.strategy

import android.annotation.SuppressLint
import android.content.res.Resources
import com.blockchain.fees.FeeType
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.NabuApiException
import com.blockchain.swap.nabu.models.nabu.NabuErrorCodes
import com.blockchain.swap.nabu.models.nabu.State
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.sunriver.fromStellarUri
import com.blockchain.transactions.Memo
import com.blockchain.transactions.SendDetails
import com.blockchain.transactions.SendFundsResult
import com.blockchain.transactions.TransactionSender
import com.blockchain.transactions.sendFundsOrThrow
import com.blockchain.ui.extensions.sampleThrottledClicks
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.withMajorValueOrZero
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.account.PitAccount
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.SendConfirmationDetails
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class XlmSendStrategy(
    currencyState: CurrencyState,
    private val xlmDataManager: XlmDataManager,
    private val xlmFeesFetcher: XlmFeesFetcher,
    private val xlmTransactionSender: TransactionSender,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val fiatExchangeRates: FiatExchangeRates,
    private val sendFundsResultLocalizer: SendFundsResultLocalizer,
    private val stringUtils: StringUtils,
    private val nabuToken: NabuToken,
    private val pitLinking: PitLinking,
    private val analytics: Analytics,
    private val nabuDataManager: NabuDataManager,
    prefs: CurrencyPrefs
) : SendStrategy<SendView>(currencyState, prefs) {

    override fun onViewAttached() { }
    override fun onViewDetached() { }

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = false

    private var pitAccount: PitAccount? = null

    override fun onPitAddressSelected() {
        pitAccount?.let {
            view?.updateReceivingAddress(it.label)
            addressSubject.onNext(it.address)
            addressToLabel.onNext(it.label)
            it.memo?.let { memo ->
                onMemoChange(Memo(value = memo, type = "text"))
                view?.enableMemo(false)
            }
        }
    }

    override fun onPitAddressCleared() {
        addressSubject.onNext("")
        view?.updateReceivingAddress("")
        addressToLabel.onNext("")
        onMemoChange(Memo.None)
        view?.enableMemo(true)
    }

    private val currency: CryptoCurrency by lazy { currencyState.cryptoCurrency }
    private var addressSubject = BehaviorSubject.create<String>()
    private var addressToLabel = BehaviorSubject.createDefault("")
    private var memoSubject = BehaviorSubject.create<Memo>().apply {
        onNext(Memo.None)
    }

    private val memoValid: Observable<Boolean>
        get() = Observables.combineLatest(memoSubject, memoIsRequired) { memo, memoIsRequired ->
            if (!memoIsRequired) {
                return@combineLatest true
            }
            if (memo == Memo.None) {
                false
            } else isValid(memo)
        }

    private fun isValid(memo: Memo): Boolean {
        return when {
            memo.type == "text" -> memo.value.length in 1..28
            memo.type == "id" -> memo.value.toLongOrNull() != null
            else -> false
        }
    }

    private var memoIsRequired = BehaviorSubject.createDefault<Boolean>(false)
    private var cryptoTextSubject = BehaviorSubject.create<CryptoValue>()
    private var continueClick = PublishSubject.create<Unit>()
    private var submitPaymentClick = PublishSubject.create<Unit>()

    private val allSendRequests: Observable<SendDetails> =
        Observables.combineLatest(
            xlmDataManager.defaultAccount().toObservable(),
            cryptoTextSubject,
            addressSubject,
            addressToLabel,
            xlmFeesFetcher.operationFee(FeeType.Regular).toObservable(),
            memoSubject
        ) { accountReference, value, address, addressToLabel, fee, memo ->
            SendDetails(
                from = accountReference,
                toAddress = address,
                toLabel = addressToLabel,
                value = value,
                fee = fee,
                memo = memo
            )
        }

    private val confirmationDetails: Observable<SendConfirmationDetails> =
        allSendRequests.sampleThrottledClicks(continueClick)
            .flatMapSingle { sendDetails ->
                xlmFeesFetcher.operationFee(FeeType.Regular)
                    .map { sendDetails to it }
            }
            .map { (sendDetails, fees) ->
                SendConfirmationDetails(
                    sendDetails = sendDetails,
                    fees = fees,
                    fiatAmount = sendDetails.value.toFiat(fiatExchangeRates),
                    fiatFees = fees.toFiat(fiatExchangeRates)
                )
            }

    private val submitConfirmationDetails: Observable<SendConfirmationDetails> =
        confirmationDetails.sampleThrottledClicks(submitPaymentClick)

    override fun onContinueClicked() {
        continueClick.onNext(Unit)
    }

    private var max: CryptoValue = CryptoValue.ZeroXlm
    private var autoClickAmount: CryptoValue? = null

    override fun onSpendMaxClicked() {
        view?.updateCryptoAmount(autoClickAmount ?: max)
    }

    override fun onBroadcastReceived() {}

    override fun onResume() {}

    override fun isAddressValid(address: String): Boolean = xlmDataManager.isAddressValid(address)

    override fun onCurrencySelected() {
        currencyState.cryptoCurrency = CryptoCurrency.XLM
        xlmSelected()
    }

    private fun xlmSelected() {
        view?.let {
            it.hideFeePriority()
            it.setFeePrioritySelection(0)
            it.disableFeeDropdown()
            it.setCryptoMaxLength(15)
            it.showInfoLink()
            it.showMemo()

            calculateMax()
            selectDefaultOrFirstFundedSendingAccount()
        }
    }

    @SuppressLint("CheckResult")
    private fun calculateMax() {
        compositeDisposable += xlmDataManager.getMaxSpendableAfterFees(FeeType.Regular)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                updateMaxAvailable(it)
            }
    }

    private fun updateMaxAvailable(balanceAfterFee: CryptoValue) {
        max = balanceAfterFee
        view?.updateMaxAvailable(balanceAfterFee, CryptoValue.ZeroXlm)
    }

    override fun processURIScanAddress(address: String) {
        val (public, cryptoValue, memo) = address.fromStellarUri()
        val fiatValue = cryptoValue.toFiat(fiatExchangeRates)
        view?.updateCryptoAmount(cryptoValue)
        view?.updateFiatAmount(fiatValue)
        cryptoTextSubject.onNext(cryptoValue)
        addressSubject.onNext(public.accountId)
        onMemoChange(memo)
        view?.updateReceivingAddress(public.accountId)
    }

    override fun handlePrivxScan(scanData: String?) {}

    override fun clearReceivingObject() {}

    override fun selectSendingAccount(account: JsonSerializableAccount?) {}

    override fun selectReceivingAccount(account: JsonSerializableAccount?) {}

    override fun selectDefaultOrFirstFundedSendingAccount() {
        compositeDisposable += xlmDataManager.defaultAccount()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { view?.updateSendingAddress(it.label) },
                onError = { Timber.e(it) }
            )
        compositeDisposable += xlmFeesFetcher.operationFee(FeeType.Regular)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { view?.updateFeeAmount(it, it.toFiat(fiatExchangeRates)) },
                onError = { Timber.e(it) }
            )
    }

    override fun submitPayment() {
        submitPaymentClick.onNext(Unit)
    }

    override fun onCryptoTextChange(cryptoText: String) {
        cryptoTextSubject.onNext(currency.withMajorValueOrZero(cryptoText))
    }

    override fun onAddressTextChange(address: String) {
        addressSubject.onNext(address)
    }

    override fun onMemoChange(memo: Memo) {
        memoSubject.onNext(memo)
        view?.displayMemo(memo)
    }

    override fun memoRequired(): Observable<Boolean> =
        addressSubject.map {
            walletOptionsDataManager.isXlmAddressExchange(it)
        }.doOnNext {
            memoIsRequired.onNext(it)
        }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {}

    override fun onNoSecondPassword() {}

    override fun onSecondPasswordValidated(secondPassword: String) {}

    override fun getFeeOptions(): FeeOptions? = null

    @SuppressLint("CheckResult")
    override fun onViewReady() {
        view?.setSendButtonEnabled(false)
        resetAccountList()

        compositeDisposable += confirmationDetails
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { Timber.e(it) }) { details ->
                view?.showPaymentDetails(details)
            }

        compositeDisposable += memoIsRequired
            .subscribe {
                if (it) {
                    view?.hideInfoLink()
                } else {
                    view?.showInfoLink()
                }
            }

        compositeDisposable += allSendRequests
            .debounce(200, TimeUnit.MILLISECONDS)
            .withLatestFrom(memoValid)
            .flatMapSingle { (sendDetails, validMemo) ->
                xlmTransactionSender.dryRunSendFunds(sendDetails)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess {
                        view?.setSendButtonEnabled(it.success && validMemo)
                        if (!it.success && sendDetails.toAddress.isNotEmpty()) {
                            autoClickAmount = it.errorValue
                            view?.updateWarning(sendFundsResultLocalizer.localize(it))
                        } else {
                            autoClickAmount = null
                            view?.clearWarning()
                        }
                    }
                    .doOnError {
                        view?.hideMaxAvailable()
                    }
                    .onErrorReturnItem(
                        SendFundsResult(
                            errorCode = 1,
                            sendDetails = sendDetails,
                            hash = null,
                            confirmationDetails = null
                        )
                    )
            }
            .subscribeBy(onError =
            { Timber.e(it) })

        compositeDisposable += submitConfirmationDetails
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { confirmationDetails ->
                val sendDetails = confirmationDetails.sendDetails
                xlmTransactionSender.sendFundsOrThrow(
                    sendDetails
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        view?.showProgressDialog(R.string.app_name)
                    }
                    .doFinally {
                        view?.dismissProgressDialog()
                        view?.dismissConfirmationDialog()
                    }
                    .doOnSuccess {
                        analytics.logEvent(SendAnalytics.SummarySendSuccess(CryptoCurrency.XLM.toString()))

                        view?.showTransactionSuccess(confirmationDetails.amount.currency)
                    }
                    .doOnError {
                        view?.showTransactionFailed()
                        analytics.logEvent(SendAnalytics.SummarySendFailure(CryptoCurrency.XLM.toString()))
                    }
                    .ignoreElement()
                    .onErrorComplete()
            }
            .subscribeBy(onError =
            { Timber.e(it) })
    }

    private fun resetAccountList() {
        compositeDisposable += pitLinking.isPitLinked().filter { it }.flatMapSingle { nabuToken.fetchNabuToken() }
            .flatMap {
                nabuDataManager.fetchCryptoAddressFromThePit(it, CryptoCurrency.XLM.symbol)
            }.applySchedulers().doOnSubscribe {
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.XLM, 1, false)
            }.subscribeBy(onError = {
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.XLM,
                    1,
                    it is NabuApiException && it.getErrorCode() == NabuErrorCodes.Bad2fa
                ) { view?.show2FANotAvailableError() }
            }) {
                val components = it.address.split(":")
                pitAccount = PitAccount(
                    label = stringUtils.getFormattedString(
                        R.string.exchange_default_account_label,
                        CryptoCurrency.XLM.symbol
                    ),
                    address = components[0],
                    memo = components[1]
                )
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.XLM, 1,
                    it.state == State.ACTIVE && it.address.isNotEmpty()) { view?.fillOrClearAddress() }
            }
    }
}

interface SendFundsResultLocalizer {
    fun localize(sendFundsResult: SendFundsResult): String
}

internal class ResourceSendFundsResultLocalizer(private val resources: Resources) : SendFundsResultLocalizer {

    override fun localize(sendFundsResult: SendFundsResult): String =
        if (sendFundsResult.success) {
            resources.getString(R.string.transaction_submitted)
        } else
            localizeXlmSend(sendFundsResult) ?: resources.getString(R.string.transaction_failed)

    private fun localizeXlmSend(sendFundsResult: SendFundsResult): String? {
        val cryptoCurrency = sendFundsResult.sendDetails.from.cryptoCurrency
        if (cryptoCurrency != CryptoCurrency.XLM) return null
        return when (sendFundsResult.errorCode) {
            2 -> resources.getString(
                R.string.transaction_failed_min_send,
                sendFundsResult.errorValue?.toStringWithSymbol()
            )
            3 -> resources.getString(
                R.string.xlm_transaction_failed_min_balance_new_account,
                sendFundsResult.errorValue?.toStringWithSymbol()
            )
            4 -> resources.getString(R.string.not_enough_funds_with_currency, cryptoCurrency)
            5 -> resources.getString(R.string.invalid_address)
            else -> null
        }
    }
}