package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.morph.trade.MorphTradeDataHistoryList
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.DashboardPresenter
import piuk.blockchain.android.ui.dashboard.adapter.delegates.SwapAnnouncementCard

class SwapAnnouncement(
    private val tierService: TierService,
    private val dataManager: MorphTradeDataHistoryList,
    dismissRecorder: DismissRecorder
) :
    Announcement<DashboardPresenter> {

    private val dismissEntry =
        dismissRecorder["SwapAnnouncementCard_DISMISSED"]

    override fun shouldShow(context: DashboardPresenter): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }
        return hasSwaps().flatMap {
            if (it) {
                Single.just(false)
            } else {
                isTier1Or2Verified()
            }
        }
    }

    private fun hasSwaps(): Single<Boolean> =
        dataManager.getTrades().map {
            it.isNotEmpty()
        }

    private fun isTier1Or2Verified(): Single<Boolean> =
        tierService.tiers().map {
            it.combinedState == Kyc2TierState.Tier1Approved || it.combinedState == Kyc2TierState.Tier2Approved
        }

    override fun show(dashboardPresenter: DashboardPresenter) {
        dashboardPresenter.showSwapAnnouncement(SwapAnnouncementCard(
            title = R.string.swap_announcement_title,
            description = R.string.swap_announcement_description,
            link = R.string.swap_announcement_introducing_link,
            closeFunction = {
                dismissEntry.isDismissed = true
                dashboardPresenter.dismissSwapAnnouncementCard()
            },
            linkFunction = {},
            isNew = true
        ))
    }
}