package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.announcement.Announcement
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kycui.navhost.models.CampaignType
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.balance.ImageRightAnnouncementCard
import piuk.blockchain.android.ui.dashboard.DashboardPresenter

internal class CompleteYourProfileCardAnnouncement(
    private val tierService: TierService,
    dismissRecorder: DismissRecorder
) : Announcement<DashboardPresenter> {

    private val dismissEntry = dismissRecorder["CompleteYourProfileCard_DISMISSED"]

    override fun shouldShow(context: DashboardPresenter): Single<Boolean> {
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

    override fun show(context: DashboardPresenter) {
        context.showAnnouncement(
            index = 0,
            announcementData = ImageRightAnnouncementCard(
                title = R.string.complete_your_profile_card_title,
                description = R.string.complete_your_profile_card_body,
                link = R.string.complete_your_profile_card_button,
                image = R.drawable.vector_gold_checkmark,
                closeFunction = {
                    dismissEntry.isDismissed = true
                    context.dismissAnnouncement(dismissEntry.prefsKey)
                },
                linkFunction = {
                    context.view.startKycFlow(CampaignType.Sunriver)
                },
                prefsKey = dismissEntry.prefsKey
            )
        )
    }
}
