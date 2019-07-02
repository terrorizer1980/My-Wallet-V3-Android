package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R

class PitAnnouncementRule(dismissRecorder: DismissRecorder) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

    override fun shouldShow(): Single<Boolean> {
        return if (dismissEntry.isDismissed) {
            Single.just(false)
        } else {
            Single.just(true)
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = AnnouncementCard(
                style = AnnouncementStyle.ThePit,
                description = R.string.pit_announcement_description,
                link = R.string.pit_announcement_introducing_link,
                closeFunction = {
                    dismissEntry.dismiss(DismissRule.DismissForSession)
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                },
                linkFunction = {
                    dismissEntry.dismiss(DismissRule.DismissForever)
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                    host.startPitLinking()
                },
                prefsKey = dismissEntry.prefsKey
            )
        )
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "PitAnnouncement_DISMISSED"
    }
}
