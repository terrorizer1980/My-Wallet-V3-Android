package piuk.blockchain.android.ui.activity.detail

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.OrderState
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.coincore.btc.BtcActivitySummaryItem
import piuk.blockchain.android.coincore.eth.EthActivitySummaryItem
import piuk.blockchain.android.coincore.impl.AssetActivityRepo
import piuk.blockchain.android.coincore.pax.PaxActivitySummaryItem
import java.text.ParseException
import java.util.Date

class ActivityDetailsInteractor(
    private val currencyPrefs: CurrencyPrefs,
    private val transactionInputOutputMapper: TransactionInOutMapper,
    private val assetActivityRepo: AssetActivityRepo,
    private val custodialWalletManager: CustodialWalletManager
) {

    fun loadCustodialItems(
        custodialActivitySummaryItem: CustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            BuyTransactionId(custodialActivitySummaryItem.txId),
            Created(Date(custodialActivitySummaryItem.timeStampMs)),
            BuyPurchaseAmount(custodialActivitySummaryItem.fundedFiat),
            BuyCryptoWallet(custodialActivitySummaryItem.cryptoCurrency),
            BuyFee(custodialActivitySummaryItem.fee)
        )

        return if (custodialActivitySummaryItem.paymentMethodId != PaymentMethod.BANK_PAYMENT_ID) {
            custodialWalletManager.getCardDetails(custodialActivitySummaryItem.paymentMethodId)
                .map { paymentMethod ->
                    addPaymentDetailsToList(list, paymentMethod, custodialActivitySummaryItem)

                    list.toList()
                }.onErrorReturn {
                    addPaymentDetailsToList(list, null, custodialActivitySummaryItem)

                    list.toList()
                }
        } else {
            list.add(BuyPaymentMethod(
                PaymentDetails(custodialActivitySummaryItem.paymentMethodId, label = null,
                    endDigits = null
                )))

            if (custodialActivitySummaryItem.status == OrderState.AWAITING_FUNDS ||
                custodialActivitySummaryItem.status == OrderState.PENDING_EXECUTION) {
                list.add(CancelAction())
            }
            Single.just(list.toList())
        }
    }

    private fun addPaymentDetailsToList(
        list: MutableList<ActivityDetailsType>,
        paymentMethod: PaymentMethod.Card?,
        custodialActivitySummaryItem: CustodialActivitySummaryItem
    ) {
        paymentMethod?.let {
            list.add(BuyPaymentMethod(PaymentDetails(
                it.cardId, it.uiLabel(), it.endDigits
            )))
        } ?: list.add(BuyPaymentMethod(
            PaymentDetails(custodialActivitySummaryItem.paymentMethodId,
                label = null, endDigits = null)
        ))

        if (custodialActivitySummaryItem.status == OrderState.PENDING_CONFIRMATION) {
            list.add(CancelAction())
        }
    }

    fun getCustodialActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): CustodialActivitySummaryItem? =
        assetActivityRepo.findCachedItem(cryptoCurrency, txHash) as? CustodialActivitySummaryItem

    fun getNonCustodialActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): NonCustodialActivitySummaryItem? =
        assetActivityRepo.findCachedItem(cryptoCurrency, txHash) as? NonCustodialActivitySummaryItem

    fun loadCreationDate(
        activitySummaryItem: ActivitySummaryItem
    ): Date? = try {
        Date(activitySummaryItem.timeStampMs)
    } catch (e: ParseException) {
        null
    }

    fun loadFeeItems(
        item: NonCustodialActivitySummaryItem
    ) = item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency)
        .flatMap { fiatValue ->
            getTransactionsMapForFeeItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForFeeItems(item, null)
        }

    private fun getTransactionsMapForFeeItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: FiatValue?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        listOfNotNull(
            Amount(item.cryptoValue),
            Value(fiatValue),
            addSingleOrMultipleFromAddresses(it),
            addFeeForTransaction(item),
            checkIfShouldAddDescription(item),
            Action()
        )
    }.onErrorReturn {
        listOfNotNull(
            Amount(item.cryptoValue),
            Value(fiatValue),
            addSingleOrMultipleFromAddresses(null),
            addFeeForTransaction(item),
            checkIfShouldAddDescription(item),
            Action()
        )
    }

    fun loadReceivedItems(
        item: NonCustodialActivitySummaryItem
    ) = item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency)
        .flatMap { fiatValue ->
            getTransactionsMapForReceivedItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForReceivedItems(item, null)
        }

    private fun getTransactionsMapForReceivedItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: FiatValue?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        listOfNotNull(
            Amount(item.cryptoValue),
            Value(fiatValue),
            addSingleOrMultipleFromAddresses(it),
            addSingleOrMultipleToAddresses(it),
            checkIfShouldAddDescription(item),
            Action()
        )
    }.onErrorReturn {
        listOfNotNull(
            Amount(item.cryptoValue),
            Value(fiatValue),
            addSingleOrMultipleFromAddresses(null),
            addSingleOrMultipleToAddresses(null),
            checkIfShouldAddDescription(item),
            Action()
        )
    }

    fun loadTransferItems(
        item: NonCustodialActivitySummaryItem
    ) = item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency)
        .flatMap { fiatValue ->
            getTransactionsMapForTransferItems(item, fiatValue)
        }.onErrorResumeNext {
            getTransactionsMapForTransferItems(item, null)
        }

    private fun getTransactionsMapForTransferItems(
        item: NonCustodialActivitySummaryItem,
        fiatValue: FiatValue?
    ) =transactionInputOutputMapper.transformInputAndOutputs(item).map {
        listOfNotNull(
            Amount(item.cryptoValue),
            Value(fiatValue),
            addSingleOrMultipleFromAddresses(it),
            addSingleOrMultipleToAddresses(it),
            checkIfShouldAddDescription(item),
            Action()
        )
    }.onErrorReturn {
            listOfNotNull(
                Amount(item.cryptoValue),
                Value(fiatValue),
                addSingleOrMultipleFromAddresses(null),
                addSingleOrMultipleToAddresses(null),
                checkIfShouldAddDescription(item),
                Action()
            )
    }

    fun loadConfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ) = item.fee.single(item.cryptoValue).flatMap { cryptoValue ->
        getTotalFiat(item, cryptoValue, currencyPrefs.selectedFiatCurrency)
    }.onErrorResumeNext {
        getTotalFiat(item, null, currencyPrefs.selectedFiatCurrency)
    }

    private fun getTotalFiat(
        item: NonCustodialActivitySummaryItem,
        cryptoValue: CryptoValue?,
        selectedFiatCurrency: String
    ) = item.totalFiatWhenExecuted(selectedFiatCurrency).flatMap { fiatValue ->
        getTransactionsMapForConfirmedSentItems(cryptoValue, fiatValue, item)
    }.onErrorResumeNext {
        getTransactionsMapForConfirmedSentItems(cryptoValue, null, item)
    }

    private fun getTransactionsMapForConfirmedSentItems(
        cryptoValue: CryptoValue?,
        fiatValue: FiatValue?,
        item: NonCustodialActivitySummaryItem
    ) = transactionInputOutputMapper.transformInputAndOutputs(item)
        .map {
            listOfNotNull(
                Amount(item.cryptoValue),
                Fee(cryptoValue),
                Value(fiatValue),
                addSingleOrMultipleFromAddresses(it),
                addSingleOrMultipleToAddresses(it),
                checkIfShouldAddDescription(item),
                Action()
            )
        }.onErrorReturn {
            listOfNotNull(
                Amount(item.cryptoValue),
                Fee(cryptoValue),
                Value(fiatValue),
                addSingleOrMultipleFromAddresses(null),
                addSingleOrMultipleToAddresses(null),
                checkIfShouldAddDescription(item),
                Action()
            )
        }

    fun loadUnconfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ) = item.fee.singleOrError().flatMap { cryptoValue ->
        getTransactionsMapForUnconfirmedSentItems(item, cryptoValue)
    }.onErrorResumeNext {
        getTransactionsMapForUnconfirmedSentItems(item, null)
    }

    private fun getTransactionsMapForUnconfirmedSentItems(
        item: NonCustodialActivitySummaryItem,
        cryptoValue: CryptoValue?
    ) = transactionInputOutputMapper.transformInputAndOutputs(item).map {
        listOfNotNull(
            Amount(item.cryptoValue),
            Fee(cryptoValue),
            addSingleOrMultipleFromAddresses(it),
            addSingleOrMultipleToAddresses(it),
            checkIfShouldAddDescription(item),
            Action()
        )
    }.onErrorReturn {
        listOfNotNull(
            Amount(item.cryptoValue),
            Fee(cryptoValue),
            addSingleOrMultipleFromAddresses(null),
            addSingleOrMultipleToAddresses(null),
            checkIfShouldAddDescription(item),
            Action()
        )
    }

    fun updateItemDescription(
        txId: String,
        cryptoCurrency: CryptoCurrency,
        description: String
    ): Completable {
        return when (val activityItem = assetActivityRepo.findCachedItem(cryptoCurrency, txId)) {
            is BtcActivitySummaryItem -> activityItem.updateDescription(description)
            is EthActivitySummaryItem -> activityItem.updateDescription(description)
            is PaxActivitySummaryItem -> activityItem.updateDescription(description)
            else -> {
                Completable.error(UnsupportedOperationException(
                    "This type of currency doesn't support descriptions"))
            }
        }
    }

    private fun addFeeForTransaction(item: NonCustodialActivitySummaryItem): FeeForTransaction? {
        return when (item) {
            is EthActivitySummaryItem -> {
                val relatedItem = assetActivityRepo.findCachedItemById(item.ethTransaction.hash)
                relatedItem?.let {
                    FeeForTransaction(
                        item.direction,
                        it.cryptoValue
                    )
                }
            }
            else -> null
        }
    }

    private fun addSingleOrMultipleFromAddresses(
        it: TransactionInOutDetails?
    ) = when {
        it == null -> {
            From(null)
        }
        it.inputs.size == 1 -> {
            From(it.inputs[0].address)
        }
        else -> {
            From(it.inputs.joinToString("\n"))
        }
    }

    private fun addSingleOrMultipleToAddresses(
        it: TransactionInOutDetails?
    ) = when {
        it == null -> {
            To(null)
        }
        it.outputs.size == 1 -> {
            To(it.outputs[0].address)
        }
        else -> {
            To(it.outputs.joinToString("\n"))
        }
    }

    private fun checkIfShouldAddDescription(
        item: NonCustodialActivitySummaryItem
    ): Description? = when (item) {
        is BtcActivitySummaryItem,
        is EthActivitySummaryItem,
        is PaxActivitySummaryItem -> Description(item.description)
        else -> null
    }
}