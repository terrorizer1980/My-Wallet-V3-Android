package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.sunriver.ui.CoinifyKycBottomDialog
import io.reactivex.Single
import piuk.blockchain.android.ui.dashboard.DashboardPresenter
import piuk.blockchain.androidbuysell.api.CoinifyWalletService

internal class CoinifyKycModalPopupAnnouncement(
    private val tierService: TierService,
    private val coinifyWalletService: CoinifyWalletService,
    private val showPopupFeatureFlag: FeatureFlag
) : Announcement<DashboardPresenter> {

    private var didShowPopup = false

    override fun shouldShow(context: DashboardPresenter): Single<Boolean> {
        if (didShowPopup) {
            return Single.just(false)
        }

        // TODO: AND-1980 use firebase remote config
        return Single.merge(
            didNotStartGoldLevelKyc(),
            isCoinifyTrader()
        ).all { it }
            .doOnSuccess { didShowPopup = it }
    }

    override fun show(context: DashboardPresenter) {
        context.view.showBottomSheetDialog(CoinifyKycBottomDialog())
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