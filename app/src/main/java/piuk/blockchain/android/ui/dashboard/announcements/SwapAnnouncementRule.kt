package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.morph.trade.MorphTradeDataHistoryList
import io.reactivex.Single
import piuk.blockchain.android.R

class SwapAnnouncementRule(
    private val tierService: TierService,
    private val dataManager: MorphTradeDataHistoryList,
    dismissRecorder: DismissRecorder
) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

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
            SwapAnnouncementCard(
                title = R.string.swap_announcement_title,
                description = R.string.swap_announcement_description,
                link = R.string.swap_announcement_introducing_link,
                closeFunction = {
                    dismissEntry.dismiss(DismissRule.DismissForSession)
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                linkFunction = {
                    host.startSwapOrKyc()
                    dismissEntry.dismiss(DismissRule.DismissForever)
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                prefsKey = dismissEntry.prefsKey
            )
        )
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "SwapAnnouncementCard_DISMISSED"
    }
}