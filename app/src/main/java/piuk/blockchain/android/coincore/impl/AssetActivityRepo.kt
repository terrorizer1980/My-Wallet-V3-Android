package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataManager
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class AssetActivityRepo(
    private val coincore: Coincore,
    private val swapHistory: MorphTradeDataManager,
    private val exchangeRates: ExchangeRateDataManager
) {
    fun fetch(account: CryptoAccount): Single<ActivitySummaryList> {
        return Singles.zip(
            fetchSwapHistory(),
            account.activity
        ) { swapList, accountList ->
            val swaps = swapList.filter { account.cryptoCurrencies.contains(it.cryptoCurrency) }
            val txSet = buildTxLookupSet(swaps as List<SwapActivitySummaryItem>)

            removeSendWhereSwapped(txSet, accountList).plus(swaps).sorted()
        }
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
        targetAddress = depositAddress
    )