package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule

class SwapAnnouncement(
    private val tierService: TierService,
    private val dataManager: MorphTradeDataHistoryList,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
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

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            AnnouncementCard(
                name = name,
                titleText = R.string.swap_announcement_title,
                bodyText = R.string.swap_announcement_description,
                ctaText = R.string.swap_announcement_introducing_link,
                iconImage = R.drawable.ic_announce_swap,
                dismissFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                ctaFunction = {
                    host.startSwapOrKyc()
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                dismissEntry = dismissEntry,
                dismissRule = DismissRule.CardPeriodic
            )
        )
    }

    override val name = "swap"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "SwapAnnouncementCard_DISMISSED"
    }
}
