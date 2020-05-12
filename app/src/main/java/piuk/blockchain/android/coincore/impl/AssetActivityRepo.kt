package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataManager
import io.reactivex.Observable
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber
import java.util.Date

private const val CACHE_LIFETIME = 2 * 60 * 1000

class AssetActivityRepo(
    private val coincore: Coincore,
    private val swapHistory: MorphTradeDataManager,
    private val exchangeRates: ExchangeRateDataManager
) {
    private val transactionCache = mutableMapOf<CryptoAccount, List<ActivitySummaryItem>>()
    private var lastUpdatedTimestamp: Long = -1L

    fun fetch(
        account: CryptoAccount,
        isRefreshRequested: Boolean
    ): Observable<ActivitySummaryList> {
        Timber.e("---- fetch acc data isRefreshRequested $isRefreshRequested")
        return if (transactionCache.isNotEmpty() &&
            System.currentTimeMillis() - lastUpdatedTimestamp <= CACHE_LIFETIME &&
            !isRefreshRequested
        ) {
            Observable.just(
                when (account) {
                    is AllWalletsAccount -> {
                        Timber.e(
                            "----- returning from cache for all wallets :${transactionCache.size}")
                        transactionCache.values.flatten()
                            .sortedByDescending { Date(it.timeStampMs) }
                    }
                    else -> {
                        Timber.e(
                            "----- returning from cache for account ${account.label} - ${transactionCache[account]}")
                        transactionCache[account]?.sortedByDescending {
                            Date(it.timeStampMs)
                        } as ActivitySummaryList
                    }
                }
            )
        } else {
            if (account is AllWalletsAccount) {
                Timber.e("----- getting all accounts")

                account.allActivities().map { list ->
                    list.groupBy { it.account }.map {
                        Timber.e("---- saving to cache: ${it.key} - ${it.value}")
                        transactionCache[it.key] = it.value
                    }
                    lastUpdatedTimestamp = System.currentTimeMillis()
                    list.sortedByDescending { Date(it.timeStampMs) }
                }.toObservable()
            } else {
                // fixme temp workaround for swap
                account.activity.map { list ->
                    transactionCache[account] = list
                    lastUpdatedTimestamp = System.currentTimeMillis()
                    Timber.e("----- got details for account: ${account.label} - $list")
                    Timber.e("----- saved to cache ${transactionCache[account]}")

                    list.sortedByDescending { Date(it.timeStampMs) }
                }.toObservable()
            }
            /* Singles.zip(
                 fetchSwapHistory(),
                 account.activity
             ) { swapList, accountList ->
                 //val swaps =
                 //    swapList.filter { account.cryptoCurrencies.contains(it.cryptoCurrency) }
                 //val txSet = buildTxLookupSet(swaps as List<SwapActivitySummaryItem>)

                 //val sortedList = removeSendWhereSwapped(txSet, accountList).plus(swaps).sorted()
                 transactionCache[account] = accountList

                 Timber.e("----- got details for account: ${account.label} - $accountList")
                 Timber.e("----- saved to cache ${transactionCache[account]}")

                 accountList as ActivitySummaryList
             }.toObservable()*/
        }

        /*  return Observable.merge(
              cache,
              network)*/

        /* return Singles.zip(
             fetchSwapHistory(),
             account.activity
         ) { swapList, accountList ->
             val swaps = swapList.filter { account.cryptoCurrencies.contains(it.cryptoCurrency) }
             val txSet = buildTxLookupSet(swaps as List<SwapActivitySummaryItem>)

             val sortedList = removeSendWhereSwapped(txSet, accountList).plus(swaps).sorted()
             transactionCache[account]?.addAll(sortedList)
             sortedList
         }*/
    }

    private fun buildTxLookupSet(swapList: List<SwapActivitySummaryItem>): Set<String> =
        swapList
            .map { it.targetAddress }
            .toSet()

    private fun removeSendWhereSwapped(swapTx: Set<String>, activityList: ActivitySummaryList) =
        activityList.filter { !it.isSwapTransaction(swapTx) }

    private fun fetchSwapHistory(): Single<ActivitySummaryList> {
        return swapHistory.getTrades()
            .flattenAsObservable { it }
            .map { it.map(exchangeRates) }
            .toList()
    }
}

private fun ActivitySummaryItem.isSwapTransaction(swapTx: Set<String>): Boolean {
//    val i = this as? NonCustodialActivitySummaryItem
//    if(i != null) {
//        if (i.direction == TransactionSummary.Direction.SENT) {
//            i.outputsMap.keys.forEach {
//                if (it in swapTx) {
//                    return true
//                }
//            }
//        }
//    }
    return false
}

private fun MorphTrade.map(exchangeRates: ExchangeRateDataManager): ActivitySummaryItem =
    SwapActivitySummaryItem(
        exchangeRates = exchangeRates,
        cryptoCurrency = quote.pair.from,
        txId = this.hashOut ?: "",
        timeStampMs = timestamp * 1000,
        cryptoValue = quote.depositAmount,
        targetValue = quote.withdrawalAmount,
        fee = quote.minerFee,
        transactionHash = hashOut ?: "",
        sourceAddress = withdrawlAddress,
        targetAddress = depositAddress,
        account = TODO()
    )