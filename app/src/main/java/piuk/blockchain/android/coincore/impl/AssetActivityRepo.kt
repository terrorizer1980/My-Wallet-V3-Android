package piuk.blockchain.android.coincore.impl

import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.CryptoAccount
import java.util.Date

private const val CACHE_LIFETIME = 2 * 60 * 1000

class AssetActivityRepo {
    private val transactionCache = mutableMapOf<CryptoAccount, List<ActivitySummaryItem>>()
    private var lastUpdatedTimestamp: Long = -1L

    fun fetch(
        account: CryptoAccount,
        isRefreshRequested: Boolean
    ): Single<ActivitySummaryList> {
        return if (transactionCache.isNotEmpty() &&
            System.currentTimeMillis() - lastUpdatedTimestamp <= CACHE_LIFETIME &&
            !isRefreshRequested
        ) {
            Single.just(
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
            if (account is AllWalletsAccount) {
                account.allActivities().map { list ->
                    list.groupBy { it.account }.map {
                        transactionCache[it.key] = it.value
                    }
                    lastUpdatedTimestamp = System.currentTimeMillis()
                    list.sortedByDescending { Date(it.timeStampMs) }
                }
            } else {
                account.activity.map { activityList ->
                    transactionCache[account] = activityList
                    lastUpdatedTimestamp = System.currentTimeMillis()

                    activityList.sortedByDescending { Date(it.timeStampMs) }
                }
            }
        }
    }
}