package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kycui.navhost.models.CampaignType
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.utils.PersistentPrefs

internal class GoForGoldAnnouncementRule(
    private val tierService: TierService,
    private val prefs: PersistentPrefs,
    dismissRecorder: DismissRecorder
) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return tierService.tiers().map {
            it.combinedState !in listOf(
                Kyc2TierState.Tier2InReview,
                Kyc2TierState.Tier2Approved,
                Kyc2TierState.Tier2Failed
            ) && !prefs.devicePreIDVCheckFailed
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = AnnouncementCard(
                style = AnnouncementStyle.ImageRight,
                title = R.string.complete_your_profile_card_title,
                description = R.string.complete_your_profile_card_body,
                link = R.string.complete_your_profile_card_button,
                image = R.drawable.vector_gold_checkmark,
                closeFunction = {
                    dismissEntry.dismiss(DismissRule.DismissForSession)
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                linkFunction = {
                    dismissEntry.dismiss(DismissRule.DismissForever)
                    host.startKyc(CampaignType.Sunriver)
                },
                prefsKey = dismissEntry.prefsKey
            )
        )
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "CompleteYourProfileCard_DISMISSED"
    }
}
