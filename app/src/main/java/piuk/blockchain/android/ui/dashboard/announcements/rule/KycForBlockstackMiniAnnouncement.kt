package piuk.blockchain.android.ui.dashboard.announcements.rule

import io.reactivex.Single

import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.MiniAnnouncementCard

class KycForBlockstackMiniAnnouncement(
    dismissRecorder: DismissRecorder,
    private val queries: AnnouncementQueries,
    private val kycForBlockstackAnnouncement: AnnouncementRule
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey: String
        get() = ""

    override val name = "kyc_stx_airdrop_mini"

    override fun shouldShow(): Single<Boolean> {
        val shouldUpgradeProfileToGold = queries.isGoldComplete().map { !it }.onErrorReturn { false }

        return shouldUpgradeProfileToGold.map {
            it && kycForBlockstackAnnouncement.isDismissed()
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = MiniAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPersistent,
                dismissEntry = dismissEntry,
                titleText = R.string.stacks_airdrop_mini_card_title,
                bodyText = R.string.stacks_airdrop_mini_card_body,
                iconImage = R.drawable.ic_airdrop_parachute_green,
                background = R.drawable.blockstack_announcement_mini_card_background,
                ctaFunction = {
                    host.startBlockstackIntro()
                },
                hasCta = true
            )
        )
    }
}