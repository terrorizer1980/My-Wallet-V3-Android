package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.android.thepit.PitLinking

class PitAnnouncementRule(
    private val pitLink: PitLinking,
    dismissRecorder: DismissRecorder,
    private val featureFlag: FeatureFlag
) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

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
