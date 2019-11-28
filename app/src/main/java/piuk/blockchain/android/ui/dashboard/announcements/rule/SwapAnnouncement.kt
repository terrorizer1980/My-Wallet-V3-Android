package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule

class SwapAnnouncement(
    private val dataManager: MorphTradeDataHistoryList,
    private val queries: AnnouncementQueries,
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
                queries.isTier1Or2Verified()
            }
        }
    }

    private fun hasSwaps(): Single<Boolean> =
        dataManager.getTrades().map {
            it.isNotEmpty()
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
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                    host.startSwapOrKyc()
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
