package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import io.reactivex.Single
import piuk.blockchain.android.R

internal class ClaimYourFreeCryptoCardAnnouncement(
    private val tierService: TierService,
    private val sunriverCampaignHelper: SunriverCampaignHelper,
    dismissRecorder: DismissRecorder
) : Announcement {

    private val dismissEntry = dismissRecorder["ClaimYourFreeCryptoCard_DISMISSED"]

    override fun shouldShow(): Single<Boolean> {
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

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = ImageRightAnnouncementCard(
                title = R.string.airdrop_program_card_title,
                description = R.string.airdrop_program_card_body,
                link = R.string.airdrop_program_card_button,
                image = R.drawable.vector_gold_checkmark,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                linkFunction = {
                    host.signupToSunRiverCampaign()
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                prefsKey = dismissEntry.prefsKey
            )
        )
    }
}
