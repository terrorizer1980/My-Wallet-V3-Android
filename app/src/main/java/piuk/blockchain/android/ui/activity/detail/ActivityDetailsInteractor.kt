package piuk.blockchain.android.ui.activity.detail

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.NonCustodialActivitySummaryItem

class ActivityDetailsInteractor(
    private val coincore: Coincore
) {
    fun loadActivityDetailsData(cryptoCurrency: CryptoCurrency, txHash: String):
            Single<NonCustodialActivitySummaryItem?> =
                Single.just(coincore[cryptoCurrency].findCachedActivityItem(txHash) as? NonCustodialActivitySummaryItem)
}