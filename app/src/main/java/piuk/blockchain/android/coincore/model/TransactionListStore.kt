package piuk.blockchain.android.coincore.model

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
}