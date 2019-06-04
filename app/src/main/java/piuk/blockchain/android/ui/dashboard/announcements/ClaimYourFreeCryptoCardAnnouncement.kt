package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.balance.ImageRightAnnouncementCard
import piuk.blockchain.android.ui.dashboard.DashboardPresenter

internal class ClaimYourFreeCryptoCardAnnouncement(
    private val tierService: TierService,
    private val sunriverCampaignHelper: SunriverCampaignHelper,
    dismissRecorder: DismissRecorder
) : Announcement<DashboardPresenter> {

    private val dismissEntry = dismissRecorder["ClaimYourFreeCryptoCard_DISMISSED"]

    override fun shouldShow(context: DashboardPresenter): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return sunriverCampaignHelper.userIsInSunRiverCampaign()
            .flatMap { inCampaign ->
                if (inCampaign) {
                    Single.just(false)
                } else {
                    tierService.tiers().map {
                        it.combinedState == Kyc2TierState.Tier2Approved
                    }
                }
            }
    }

    override fun show(context: DashboardPresenter) {
        context.showAnnouncement(
            index = 0,
            announcementData = ImageRightAnnouncementCard(
                title = R.string.airdrop_program_card_title,
                description = R.string.airdrop_program_card_body,
                link = R.string.airdrop_program_card_button,
                image = R.drawable.vector_xlm_colored,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    context.dismissAnnouncement(dismissEntry.prefsKey)
                },
                linkFunction = {
                    context.signupToSunRiverCampaign()
                    context.dismissAnnouncement(dismissEntry.prefsKey)
                },
                prefsKey = dismissEntry.prefsKey
            )
        )
    }
}
