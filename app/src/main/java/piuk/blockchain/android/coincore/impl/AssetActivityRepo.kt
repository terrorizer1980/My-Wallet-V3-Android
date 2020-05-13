package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe
import io.reactivex.Observable
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import timber.log.Timber

private const val CACHE_LIFETIME = 60 * 1000

class AssetActivityRepo(
    val coincore: Coincore
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
            .onErrorResumeNext { _: Throwable ->
                getFromCache()
            }
        ).switchIfEmpty { emptyList<ActivitySummaryItem>() }
            .toObservable()
            .map { list ->
                list.filter { item -> account.includes(item.account) }.sorted()
            }


        /* Timber.e("---- fetch isRefreshRequested: $isRefreshRequested")
         return Observable.concat(
             if (transactionCache.isNotEmpty() && !isCacheExpired()) {
                 Timber.e("---- returning cache")
                 Observable.just(
                     when (account) {
                         is AllWalletsAccount -> {
                             transactionCache.values.flatten()
                                 .sortedByDescending { Date(it.timeStampMs) }
                         }
                         else -> {
                             transactionCache[account]?.sortedByDescending {
                                 Date(it.timeStampMs)
                             } ?: emptyList()
                         }
                     }
                 )
             } else {

             }

             if (isRefreshRequested || isCacheExpired()) {
                 Timber.e("---- returning network")
                 if (account is AllWalletsAccount) {
                     account.allActivities().toObservable().map { list ->
                         list.groupBy { it.account }.map {
                             transactionCache[it.key] = it.value
                         }
                         lastUpdatedTimestamp = System.currentTimeMillis()
                         Timber.e("--- all accounts network return")
                         list.sortedByDescending { Date(it.timeStampMs) }
                     }
                 } else {
                     account.activity.toObservable().map { activityList ->
                         transactionCache[account] = activityList
                         lastUpdatedTimestamp = System.currentTimeMillis()
                         Timber.e("--- one account network return")

                         activityList.sortedByDescending { Date(it.timeStampMs) }
                     }
                 }
             }
         )*/
    }

    private fun getFromNetwork(refreshRequested: Boolean) : Maybe<ActivitySummaryList> {
        return if (refreshRequested || isCacheExpired()) {
            coincore.allWallets.activity.toMaybe().doOnSuccess { activityList ->
                transactionCache.clear()
                transactionCache.addAll(activityList)
                lastUpdatedTimestamp = System.currentTimeMillis()
                Timber.e("---  network return")
            }
        } else {
            Maybe.empty()
        }
    }

    private fun getFromCache(): Maybe<ActivitySummaryList> {
        return if (transactionCache.isNotEmpty()) {
            Timber.e("--- return from cache ${transactionCache.size}")
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
