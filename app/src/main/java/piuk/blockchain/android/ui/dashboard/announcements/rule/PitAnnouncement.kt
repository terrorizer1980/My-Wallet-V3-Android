package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.support.annotation.VisibleForTesting
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule

class PitAnnouncement(
    private val pitLink: PitLinking,
    dismissRecorder: DismissRecorder,
    private val featureFlag: FeatureFlag
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey =
        DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return pitLink.isPitLinked().zipWith(featureFlag.enabled).map { (linked, enabled) ->
            !linked && enabled
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = AnnouncementCard(
                name = name,
                titleText = R.string.pit_announcement_title,
                bodyText = R.string.pit_announcement_body,
                ctaText = R.string.pit_announcement_cta_text,
                iconImage = R.drawable.ic_announce_the_pit,
                dismissFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                ctaFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                    host.startPitLinking()
                },
                dismissEntry = dismissEntry,
                dismissRule = DismissRule.CardOneTime
            )
        )
    }

    override val name = "pit_linking"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "PitAnnouncement_DISMISSED"
    }
}
