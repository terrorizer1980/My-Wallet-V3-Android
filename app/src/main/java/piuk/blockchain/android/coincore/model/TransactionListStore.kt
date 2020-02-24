package piuk.blockchain.android.coincore.model

import io.reactivex.Observable
import io.reactivex.Single

class TransactionListStore {

    private var data = mutableListOf<ActivitySummaryItem>()

    fun insertTransactions(transactions: List<ActivitySummaryItem>) {
        data.addAll(transactions)
    }

    fun clear() {
        data.clear()
    }

    val list: List<ActivitySummaryItem>
        get() = data.sorted()

    fun getTxFromHash(transactionHash: String): Single<ActivitySummaryItem> =
        Observable.fromIterable(data)
            .filter { it.hash == transactionHash }
            .firstOrError()

//    fun getPendingTransactions(): ActivitySummaryList {
//        val pendingMap = HashMap<String, ActivitySummaryItem>()
//
//        data.filter { it.isPending }
//            .forEach { pendingMap[it.hash] = it }
//
//        if (pendingMap.isNotEmpty()) {
//            filterProcessed(newlyFetchedTxs, pendingMap)
//        }
//
//        return ArrayList(pendingMap.values)
//    }
//
//    private fun filterProcessed(newlyFetchedTxs: ActivitySummaryList, pendingMap: HashMap<String, ActivitySummaryItem>) {
//        newlyFetchedTxs.filter { pendingMap.containsKey(it.hash) }
//            .forEach { pendingMap.remove(it.hash) }
//    }
}