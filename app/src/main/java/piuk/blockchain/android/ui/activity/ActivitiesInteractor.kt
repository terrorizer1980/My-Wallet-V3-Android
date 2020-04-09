package piuk.blockchain.android.ui.activity

import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount

class ActivitiesInteractor(
    private val coincore: Coincore
) {
    fun getActivityForAccount(account: CryptoAccount): Single<ActivitySummaryList> =
        account.activity

    fun getDefaultAccount(): Single<CryptoAccount> =
        Single.just(coincore.allWallets)
}
