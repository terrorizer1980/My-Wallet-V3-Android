package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.support.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule

internal class KycForBlockstackAnnouncement(
    dismissRecorder: DismissRecorder,
    private val queries: AnnouncementQueries
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return queries.isGoldComplete().map { !it }
    }

    override fun show(host: AnnouncementHost) {

        val card = AnnouncementCard(
            name = name,
            titleText = R.string.stacks_airdrop_card_title,
            bodyText = R.string.stacks_airdrop_card_description,
            ctaText = R.string.stacks_airdrop_card_button,
            iconImage = R.drawable.ic_announce_stacks,
            dismissFunction = {
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
            },
            ctaFunction = {
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
                host.startCampaignIntro(CampaignType.Blockstack)
            },
            dismissEntry = dismissEntry,
            dismissRule = DismissRule.CardPeriodic
        )
        host.showAnnouncementCard(card)
    }

    override val name = "kyc_stx_airdrop"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "KYC_STX_AIRDROP_DISMISSED"
    }
}
