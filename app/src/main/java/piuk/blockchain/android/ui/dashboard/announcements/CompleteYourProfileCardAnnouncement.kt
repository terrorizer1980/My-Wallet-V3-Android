package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kycui.navhost.models.CampaignType
import io.reactivex.Single
import piuk.blockchain.android.R

internal class CompleteYourProfileCardAnnouncement(
    private val tierService: TierService,
    dismissRecorder: DismissRecorder
) : Announcement {

    private val dismissEntry = dismissRecorder["CompleteYourProfileCard_DISMISSED"]

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return tierService.tiers().map {
            it.combinedState !in listOf(
                Kyc2TierState.Tier2InReview,
                Kyc2TierState.Tier2Approved,
                Kyc2TierState.Tier2Failed
            )
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = ImageRightAnnouncementCard(
                title = R.string.complete_your_profile_card_title,
                description = R.string.complete_your_profile_card_body,
                link = R.string.complete_your_profile_card_button,
                image = R.drawable.vector_gold_checkmark,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                linkFunction = {
                    host.startKyc(CampaignType.Sunriver)
                },
                prefsKey = dismissEntry.prefsKey
            )
        )
    }
}
