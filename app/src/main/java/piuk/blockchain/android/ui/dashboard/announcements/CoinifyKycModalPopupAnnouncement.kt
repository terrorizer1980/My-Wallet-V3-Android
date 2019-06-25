package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.remoteconfig.FeatureFlag
import piuk.blockchain.android.ui.dashboard.announcements.popups.CoinifyKycPopup
import io.reactivex.Single
import piuk.blockchain.androidbuysell.api.CoinifyWalletService

internal class CoinifyKycModalPopupAnnouncement(
    private val tierService: TierService,
    private val coinifyWalletService: CoinifyWalletService,
    private val showPopupFeatureFlag: FeatureFlag
) : Announcement {

    private var didShowPopup = false

    override fun shouldShow(): Single<Boolean> {
        if (didShowPopup) {
            return Single.just(false)
        }

        return Single.merge(
            didNotStartGoldLevelKyc(),
            isCoinifyTrader(),
            showPopupFeatureFlag.enabled
        ).all { it }
    }

    override fun show(host: AnnouncementHost) {
        didShowPopup = true
        host.showAnnouncmentPopup(CoinifyKycPopup())
    }

    private fun isCoinifyTrader(): Single<Boolean> =
        coinifyWalletService.getCoinifyData()
            .isEmpty
            .map { !it }

    private fun didNotStartGoldLevelKyc(): Single<Boolean> =
        tierService.tiers().map {
            it.combinedState !in listOf(
                Kyc2TierState.Tier2InReview,
                Kyc2TierState.Tier2Approved,
                Kyc2TierState.Tier2Failed
            )
        }
}