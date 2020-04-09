package piuk.blockchain.android.coincore.impl

import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.TxCache

internal class TxCacheImpl : TxCache {
    private val txCache = mutableMapOf<String, ActivitySummaryItem>()

    override fun addToCache(txList: ActivitySummaryList) =
        txList.forEach { txCache[it.txId] = it }

    operator fun get(txHash: String): ActivitySummaryItem? =
        txCache[txHash]

    override fun asActivityList(): List<ActivitySummaryItem> =
        txCache.values.sorted()

    override val hasTransactions: Boolean
        get() = txCache.isNotEmpty()
}