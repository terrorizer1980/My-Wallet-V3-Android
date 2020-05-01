package piuk.blockchain.android.ui.activity.detail

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.OrderState
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CustodialActivitySummaryItem
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import piuk.blockchain.android.coincore.btc.BtcActivitySummaryItem
import piuk.blockchain.android.coincore.eth.EthActivitySummaryItem
import piuk.blockchain.android.coincore.pax.PaxActivitySummaryItem
import java.util.Date

class ActivityDetailsInteractor(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val transactionInputOutputMapper: TransactionInOutMapper
) {

    fun loadCustodialItems(
        custodialActivitySummaryItem: CustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf(
            BuyTransactionId(custodialActivitySummaryItem.txId),
            Created(Date(custodialActivitySummaryItem.timeStampMs)),
            BuyPurchaseAmount(custodialActivitySummaryItem.fundedFiat),
            BuyCryptoWallet(custodialActivitySummaryItem.cryptoCurrency),
            BuyFee(custodialActivitySummaryItem.fee),
            // TODO this will change when we add cards, but for now it's the only supported type
            BuyPaymentMethod("Bank Wire Transfer")
        )
        if (custodialActivitySummaryItem.status == OrderState.AWAITING_FUNDS ||
            custodialActivitySummaryItem.status == OrderState.PENDING_EXECUTION) {
            list.add(CancelAction())
        }

        return Single.just(list.toList())
    }

    fun getCustodialActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): CustodialActivitySummaryItem? = coincore[cryptoCurrency].findCachedActivityItem(
        txHash
    ) as? CustodialActivitySummaryItem

    fun getNonCustodialActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): NonCustodialActivitySummaryItem? = coincore[cryptoCurrency].findCachedActivityItem(
        txHash
    ) as? NonCustodialActivitySummaryItem

    fun loadCreationDate(
        activitySummaryItem: ActivitySummaryItem
    ): Single<Date> = Single.just(Date(activitySummaryItem.timeStampMs))

    fun loadFeeItems(
        item: NonCustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf<ActivityDetailsType>()
        list.add(Amount(item.totalCrypto))
        return item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency).flatMap { fiatValue ->
            list.add(Value(fiatValue))
            transactionInputOutputMapper.transformInputAndOutputs(item).map {
                if (it.inputs.size == 1) {
                    list.add(From(it.inputs[0].address))
                } else {
                    list.add(From(it.inputs.joinToString("\n")))
                }
                list.add(FeeForTransaction("TODO"))
                shouldContainDescription(list, item)
                list.add(Action())
                list
            }
        }
    }

    fun loadReceivedItems(
        item: NonCustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf<ActivityDetailsType>()
        list.add(Amount(item.totalCrypto))
        return item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency).flatMap { fiatValue ->
            list.add(Value(fiatValue))
            transactionInputOutputMapper.transformInputAndOutputs(item).map {
                addSingleOrMultipleAddresses(it, list)
                shouldContainDescription(list, item)
                list.add(Action())
                list
            }
        }
    }

    fun loadTransferItems(
        item: NonCustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf<ActivityDetailsType>()
        list.add(Amount(item.totalCrypto))
        return item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency).flatMap { fiatValue ->
            list.add(Value(fiatValue))
            transactionInputOutputMapper.transformInputAndOutputs(item).map {
                addSingleOrMultipleAddresses(it, list)
                shouldContainDescription(list, item)
                list.add(Action())
                list
            }
        }
    }

    fun loadConfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf<ActivityDetailsType>()
        list.add(Amount(item.totalCrypto))
        return item.fee.singleOrError().flatMap { cryptoValue ->
            list.add(Fee(cryptoValue))
            item.totalFiatWhenExecuted(currencyPrefs.selectedFiatCurrency).flatMap { fiatValue ->
                list.add(Value(fiatValue))
                transactionInputOutputMapper.transformInputAndOutputs(item).map {
                    addSingleOrMultipleAddresses(it, list)
                    shouldContainDescription(list, item)
                    list.add(Action())
                    list
                }
            }
        }
    }

    fun loadUnconfirmedSentItems(
        item: NonCustodialActivitySummaryItem
    ): Single<List<ActivityDetailsType>> {
        val list = mutableListOf<ActivityDetailsType>()
        list.add(Amount(item.totalCrypto))
        return item.fee.singleOrError().flatMap { cryptoValue ->
            list.add(Fee(cryptoValue))
            transactionInputOutputMapper.transformInputAndOutputs(item).map {
                addSingleOrMultipleAddresses(it, list)
                shouldContainDescription(list, item)
                list.add(Action())
                list
            }
        }
    }

    fun updateItemDescription(
        txId: String,
        cryptoCurrency: CryptoCurrency,
        description: String
    ): Completable {
        val activityItem = coincore[cryptoCurrency].findCachedActivityItem(
            txId
        )
        return when (activityItem) {
            is BtcActivitySummaryItem -> activityItem.updateDescription(description)
            is EthActivitySummaryItem -> activityItem.updateDescription(description)
            is PaxActivitySummaryItem -> activityItem.updateDescription(description)
            else -> {
                Completable.error(UnsupportedOperationException(
                    "This type of currency doesn't support descriptions"))
            }
        }
    }

    private fun addSingleOrMultipleAddresses(
        it: TransactionInOutDetails,
        list: MutableList<ActivityDetailsType>
    ) {
        if (it.inputs.size == 1) {
            list.add(From(it.inputs[0].address))
        } else {
            list.add(From(it.inputs.joinToString("\n")))
        }
        if (it.outputs.size == 1) {
            list.add(To(it.outputs[0].address))
        } else {
            list.add(To(it.outputs.joinToString("\n")))
        }
    }

    private fun shouldContainDescription(
        list: MutableList<ActivityDetailsType>,
        item: NonCustodialActivitySummaryItem
    ) {
        when (item) {
            is BtcActivitySummaryItem,
            is EthActivitySummaryItem,
            is PaxActivitySummaryItem -> list.add(Description(item.description))
            else -> {
            } // do nothing
        }
    }
}