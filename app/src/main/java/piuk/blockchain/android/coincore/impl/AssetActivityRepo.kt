package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe
import io.reactivex.Observable
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount

private const val CACHE_LIFETIME = 60 * 1000

class AssetActivityRepo(
    private val coincore: Coincore
) {
    private val transactionCache = mutableListOf<ActivitySummaryItem>()
    private var lastUpdatedTimestamp: Long = -1L

    fun fetch(
        account: CryptoAccount,
        isRefreshRequested: Boolean
    ): Observable<ActivitySummaryList> {
        return Maybe.concat(
            getFromCache(),
            getFromNetwork(isRefreshRequested)
        )
        .toObservable()
        .map { list ->
            list.filter { item -> account.includes(item.account) }.sorted()
        }
    }

    private fun getFromNetwork(refreshRequested: Boolean): Maybe<ActivitySummaryList> {
        return if (refreshRequested || isCacheExpired()) {
            coincore.allWallets.activity.toMaybe().doOnSuccess { activityList ->
                // on error of activity returns onSuccess with empty list
                if (activityList.isNotEmpty()) {
                    transactionCache.clear()
                    transactionCache.addAll(activityList)
                }
                lastUpdatedTimestamp = System.currentTimeMillis()
            }.map { list ->
                // if network comes empty, but we have cache, return cache instead
                if (list.isEmpty() && transactionCache.isNotEmpty()) {
                    transactionCache
                } else {
                    list
                }
            }
        } else {
            Maybe.empty()
        }
    }

    private fun getFromCache(): Maybe<ActivitySummaryList> {
        return if (transactionCache.isNotEmpty()) {
            Maybe.just(transactionCache)
        } else {
            Maybe.empty()
        }
    }

    private fun isCacheExpired() =
        System.currentTimeMillis() - lastUpdatedTimestamp >= CACHE_LIFETIME

    fun findCachedItem(cryptoCurrency: CryptoCurrency, txHash: String): ActivitySummaryItem? =
        transactionCache.find {
            it.cryptoCurrency == cryptoCurrency && it.txId == txHash
        }
}
