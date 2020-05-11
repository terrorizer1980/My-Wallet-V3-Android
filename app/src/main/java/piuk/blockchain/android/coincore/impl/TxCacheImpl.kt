package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.TxCache

class TxCacheImpl : TxCache {
    private val txCache = mutableMapOf<String, ActivitySummaryItem>()

    override fun addToCache(txList: ActivitySummaryList) =
        txList.forEach { txCache[it.txId] = it }

    operator fun get(txHash: String): ActivitySummaryItem? =
        txCache[txHash]

    fun getWithIdAndType(txHash: String, cryptoCurrency: CryptoCurrency) =
        txCache.values.filter { it.txId == txHash && it.cryptoCurrency == cryptoCurrency }

    override fun asActivityList(): List<ActivitySummaryItem> =
        txCache.values.toList()

    override fun clear() {
        txCache.clear()
    }

    override val hasTransactions: Boolean
        get() = txCache.isNotEmpty()
}