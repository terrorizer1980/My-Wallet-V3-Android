package piuk.blockchain.android.ui.activity.detail

import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem
import java.util.Date
import java.util.MissingResourceException

class ActivityDetailsInteractor(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val transactionInputOutputMapper: TransactionInOutMapper
) {

    fun getActivityDetails(
        cryptoCurrency: CryptoCurrency,
        txHash: String
    ): Single<NonCustodialActivitySummaryItem> {
        val item = coincore[cryptoCurrency].findCachedActivityItem(
            txHash) as? NonCustodialActivitySummaryItem
        return item?.run {
            Single.just(this)
        } ?: Single.error(MissingResourceException("Could not find the activity item",
            NonCustodialActivitySummaryItem::class.simpleName, ""))
    }

    fun loadCreationDate(
        nonCustodialActivitySummaryItem: NonCustodialActivitySummaryItem
    ): Single<Date> = Single.just(Date(nonCustodialActivitySummaryItem.timeStampMs))

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
                list.add(Description())
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
                list.add(Description())
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
                    list.add(Description())
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
                list.add(Description())
                list.add(Action())
                list
            }
        }
    }

    private fun addSingleOrMultipleAddresses(
        it: TransactionInOutDetails,
        list: MutableList<ActivityDetailsType>
    ) {
            if (it.outputs.size == 1) {
                list.add(To(it.outputs[0].address))
            } else {
                list.add(To(it.outputs.joinToString("\n")))
            }
            if (it.inputs.size == 1) {
                list.add(From(it.inputs[0].address))
            } else {
                list.add(From(it.inputs.joinToString("\n")))
            }
        }
}