package piuk.blockchain.android.ui.transactions

import androidx.annotation.VisibleForTesting
import com.blockchain.data.TransactionHash
import com.blockchain.sunriver.XlmDataManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.format
import info.blockchain.balance.formatWithUnit
import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.multiaddress.TransactionSummary
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.old.TransactionListDataManager
import piuk.blockchain.android.ui.transactions.adapter.formatting
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.math.BigInteger
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.collections.HashMap

class TransactionDetailPresenter constructor(
    private val transactionHelper: TransactionHelper,
    prefs: PersistentPrefs,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val transactionListDataManager: TransactionListDataManager,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val environmentSettings: EnvironmentConfig,
    private val xlmDataManager: XlmDataManager
) : BasePresenter<TransactionDetailView>() {

    private val fiatType = prefs.selectedFiatCurrency

    @VisibleForTesting
    lateinit var activitySummaryItem: ActivitySummaryItem

    // Currently no available notes for bch
    // Only BTC and ETHER currently supported
    var transactionNote: String?
        get() = when (activitySummaryItem.cryptoCurrency) {
            CryptoCurrency.BTC -> payloadDataManager.getTransactionNotes(activitySummaryItem.hash)
            CryptoCurrency.PAX -> ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes[activitySummaryItem.hash]
            CryptoCurrency.ETHER -> ethDataManager.getTransactionNotes(activitySummaryItem.hash)
            else -> ""
        }
        private set(txHash) {
            val notes: String? = when (activitySummaryItem.cryptoCurrency) {
                CryptoCurrency.BTC -> payloadDataManager.getTransactionNotes(txHash!!)
                CryptoCurrency.PAX -> ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes[activitySummaryItem.hash]
                CryptoCurrency.ETHER -> ethDataManager.getTransactionNotes(activitySummaryItem.hash)
                else -> {
                    view?.hideDescriptionField()
                    ""
                }
            }
            view?.setDescription(notes)
        }

    val transactionHash: TransactionHash
        get() = TransactionHash(activitySummaryItem.cryptoCurrency, activitySummaryItem.hash)

    override fun onViewReady() {
        val pos = view?.positionDetailLookup() ?: -1
        val txHash = view?.txHashDetailLookup()

        when {
            pos != -1 -> {
                val transactionList = transactionListDataManager.getTransactionList()
                if (pos < transactionList.size) {
                    activitySummaryItem = transactionListDataManager.getTransactionList()[pos]
                    updateUiFromTransaction(activitySummaryItem)
                } else {
                    view?.pageFinish()
                }
            }
            txHash != null -> {
                compositeDisposable +=
                    transactionListDataManager.getTxFromHash(txHash)
                        .doOnSuccess { activitySummaryItem = it }
                        .subscribe(
                            { this.updateUiFromTransaction(it) },
                            { view?.pageFinish() })
            }
            else -> {
                Timber.e("Transaction hash not found")
                view?.pageFinish()
            }
        }
    }

    fun updateTransactionNote(description: String) {
        val completable: Completable = when (activitySummaryItem.cryptoCurrency) {
            CryptoCurrency.BTC -> payloadDataManager.updateTransactionNotes(
                activitySummaryItem.hash,
                description
            )
            CryptoCurrency.PAX -> ethDataManager.updateErc20TransactionNotes(activitySummaryItem.hash,
                description)
            CryptoCurrency.ETHER -> ethDataManager.updateTransactionNotes(
                activitySummaryItem.hash,
                description
            )
            else -> throw IllegalArgumentException("Only BTC, ETHER and PAX currently supported")
        }

        compositeDisposable +=
            completable.subscribe(
                {
                    view?.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
                    view?.setDescription(description)
                },
                { view?.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) }
            )
    }

    private fun updateUiFromTransaction(activitySummaryItem: ActivitySummaryItem) {
        with(activitySummaryItem) {
            view?.setTransactionType(direction, activitySummaryItem.isFeeTransaction)
            view?.updateFeeFieldVisibility(direction != TransactionSummary.Direction.RECEIVED &&
                    !activitySummaryItem.isFeeTransaction)
            setTransactionColor(this)
            setTransactionValue(cryptoCurrency, total)
            setConfirmationStatus(cryptoCurrency, hash, confirmations.toLong())
            transactionNote = hash
            setDate(timeStamp)
            setTransactionFee(cryptoCurrency, fee)

            when (cryptoCurrency) {
                CryptoCurrency.BTC -> handleBtcToAndFrom(this)
                CryptoCurrency.ETHER -> handleEthToAndFrom(this)
                CryptoCurrency.BCH -> handleBchToAndFrom(this)
                CryptoCurrency.XLM -> handleXlmToAndFrom(this)
                CryptoCurrency.PAX -> handlePaxToAndFrom(this)
                else -> throw IllegalArgumentException("$cryptoCurrency is not currently supported")
            }

            compositeDisposable +=
                getTransactionValueString(fiatType, this)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { value -> view?.setTransactionValueFiat(value) },
                        { view?.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) })

            view?.onDataLoaded()
            view?.setIsDoubleSpend(doubleSpend)
        }
    }

    private fun handleXlmToAndFrom(activitySummaryItem: ActivitySummaryItem) {
        compositeDisposable +=
            xlmDataManager.defaultAccount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { account ->
                        var fromAddress = activitySummaryItem.inputsMap.keys.first()
                        var toAddress = activitySummaryItem.outputsMap.keys.first()
                        if (fromAddress == account.accountId) {
                            fromAddress = account.label
                        }
                        if (toAddress == account.accountId) {
                            toAddress = account.label
                        }

                        view?.let {
                            it.setFromAddress(listOf(TransactionDetailModel(fromAddress, "", "")))
                            it.setToAddresses(listOf(TransactionDetailModel(toAddress, "", "")))
                        }
                    })
    }

    private fun handleEthToAndFrom(activitySummaryItem: ActivitySummaryItem) {
        var fromAddress = activitySummaryItem.inputsMap.keys.first()
        var toAddress = activitySummaryItem.outputsMap.keys.first()

        val ethAddress = ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account
        if (fromAddress == ethAddress) {
            fromAddress = stringUtils.getString(R.string.eth_default_account_label)
        }
        if (toAddress == ethAddress) {
            toAddress = stringUtils.getString(R.string.eth_default_account_label)
        }
        view?.let {
            it.setFromAddress(listOf(TransactionDetailModel(fromAddress, "", "")))
            it.setToAddresses(listOf(TransactionDetailModel(toAddress, "", "")))
        }
    }

    private fun handlePaxToAndFrom(activitySummaryItem: ActivitySummaryItem) {
        var fromAddress = activitySummaryItem.inputsMap.keys.first()
        var toAddress = activitySummaryItem.outputsMap.keys.first()

        val ethAddress = ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account
        if (fromAddress == ethAddress) {
            fromAddress = stringUtils.getString(R.string.pax_default_account_label)
        }
        if (toAddress == ethAddress) {
            toAddress = stringUtils.getString(R.string.pax_default_account_label)
        }
        view?.let {
            it.setFromAddress(listOf(TransactionDetailModel(fromAddress, "", "")))
            it.setToAddresses(listOf(TransactionDetailModel(toAddress, "", "")))
        }
    }

    private fun handleBtcToAndFrom(activitySummaryItem: ActivitySummaryItem) {
        val (inputs, outputs) = transactionHelper.filterNonChangeAddresses(activitySummaryItem)
        setToAndFrom(activitySummaryItem, inputs, outputs)
    }

    private fun handleBchToAndFrom(activitySummaryItem: ActivitySummaryItem) {
        val (inputs, outputs) = transactionHelper.filterNonChangeAddressesBch(activitySummaryItem)
        setToAndFrom(activitySummaryItem, inputs, outputs)
    }

    private fun setToAndFrom(
        activitySummaryItem: ActivitySummaryItem,
        inputs: HashMap<String, BigInteger?>,
        outputs: HashMap<String, BigInteger?>
    ) {
        // From Addresses
        val fromList = getFromList(activitySummaryItem.cryptoCurrency, inputs)
        view?.setFromAddress(fromList)
        // To Addresses
        val recipients = getToList(activitySummaryItem.cryptoCurrency, outputs)
        view?.setToAddresses(recipients)
    }

    private fun getFromList(
        currency: CryptoCurrency,
        inputMap: HashMap<String, BigInteger?>
    ): List<TransactionDetailModel> {
        val inputs = handleTransactionMap(inputMap, currency)
        // No inputs = coinbase transaction
        if (inputs.isEmpty()) {
            val coinbase = TransactionDetailModel(
                stringUtils.getString(R.string.transaction_detail_coinbase),
                "",
                currency.symbol
            )

            inputs.add(coinbase)
        }

        return inputs.toList()
    }

    private fun getToList(
        currency: CryptoCurrency,
        outputMap: HashMap<String, BigInteger?>
    ): List<TransactionDetailModel> = handleTransactionMap(outputMap, currency)

    private fun handleTransactionMap(
        inputMap: HashMap<String, BigInteger?>,
        currency: CryptoCurrency
    ): MutableList<TransactionDetailModel> {
        val inputs = mutableListOf<TransactionDetailModel>()
        for ((key, value) in inputMap) {
            var label: String?
            if (currency == CryptoCurrency.BTC) {
                label = payloadDataManager.addressToLabel(key)
            } else {
                label = bchDataManager.getLabelFromBchAddress(key)
                if (label == null)
                    label = FormatsUtil.toShortCashAddress(
                        environmentSettings.bitcoinCashNetworkParameters,
                        key
                    )
            }

            val transactionDetailModel = buildTransactionDetailModel(label, currency, value, currency.symbol)
            inputs.add(transactionDetailModel)
        }
        return inputs
    }

    private fun buildTransactionDetailModel(
        label: String?,
        currency: CryptoCurrency,
        value: BigInteger?,
        unit: String
    ): TransactionDetailModel = TransactionDetailModel(
        label,
        if (currency == CryptoCurrency.BTC) {
            CryptoValue.bitcoinFromSatoshis(value ?: BigInteger.ZERO).format()
        } else {
            CryptoValue.bitcoinCashFromSatoshis(value ?: BigInteger.ZERO).format()
        },
        unit
    ).apply {
        if (address == MultiAddressFactory.ADDRESS_DECODE_ERROR) {
            address = stringUtils.getString(R.string.tx_decode_error)
            setAddressDecodeError(true)
        }
    }

    private fun setTransactionFee(currency: CryptoCurrency, fee: Observable<BigInteger>) {
        fee.map {
            when (currency) {
                CryptoCurrency.BTC -> CryptoValue.bitcoinFromSatoshis(it)
                CryptoCurrency.ETHER -> CryptoValue.etherFromWei(it)
                CryptoCurrency.BCH -> CryptoValue.bitcoinCashFromSatoshis(it)
                CryptoCurrency.XLM -> CryptoValue.lumensFromStroop(it)
                CryptoCurrency.PAX -> CryptoValue.etherFromWei(it)
                else -> CryptoValue.usdPaxFromMinor(it)
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                view?.setFee("")
            }
            .subscribe { view?.setFee(it.formatWithUnit()) }
            .addTo(compositeDisposable)
    }

    private fun setTransactionValue(currency: CryptoCurrency, total: BigInteger) {
        when (currency) {
            CryptoCurrency.ETHER -> CryptoValue.etherFromWei(total)
            CryptoCurrency.BTC -> CryptoValue.bitcoinFromSatoshis(total)
            CryptoCurrency.BCH -> CryptoValue.bitcoinCashFromSatoshis(total)
            CryptoCurrency.XLM -> CryptoValue.lumensFromStroop(total)
            CryptoCurrency.PAX -> CryptoValue.usdPaxFromMinor(total)
            else -> throw IllegalArgumentException("$currency is not currently supported")
        }.run { view?.setTransactionValue(this.formatWithUnit()) }
    }

    @VisibleForTesting
    internal fun setConfirmationStatus(cryptoCurrency: CryptoCurrency, txHash: String, confirmations: Long) {
        if (confirmations >= cryptoCurrency.requiredConfirmations) {
            view?.setStatus(cryptoCurrency, stringUtils.getString(R.string.transaction_detail_confirmed), txHash)
        } else {
            var pending = stringUtils.getString(R.string.transaction_detail_pending)
            pending =
                String.format(Locale.getDefault(), pending, confirmations, cryptoCurrency.requiredConfirmations)
            view?.setStatus(cryptoCurrency, pending, txHash)
        }
    }

    private fun setDate(time: Long) {
        val epochTime = time * 1000

        val date = Date(epochTime)
        val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateText = dateFormat.format(date)
        val timeText = timeFormat.format(date)

        view?.setDate("$dateText @ $timeText")
    }

    @VisibleForTesting
    internal fun setTransactionColor(transaction: ActivitySummaryItem) {
        view?.setTransactionColour(transaction.formatting().directionColour)
    }

    @VisibleForTesting
    internal fun getTransactionValueString(currency: String, transaction: ActivitySummaryItem): Single<String> =
        exchangeRateDataManager.getHistoricPrice(
            CryptoValue(transaction.cryptoCurrency, transaction.total),
            currency,
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
