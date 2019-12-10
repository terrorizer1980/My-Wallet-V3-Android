package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.ABTestExperiment
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.android.thepit.PitAnalyticsEvent
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class PitAnnouncement(
    private val pitLink: PitLinking,
    dismissRecorder: DismissRecorder,
    private val featureFlag: FeatureFlag,
    private val analytics: Analytics,
    private val abTestExperiment: ABTestExperiment
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
        val compositeDisposable = CompositeDisposable()
        compositeDisposable += abTestExperiment.getABVariant(ABTestExperiment.AB_THE_PIT_ANNOUNCEMENT_VARIANT)
            .subscribeBy {
                host.showAnnouncementCard(
                    card = StandardAnnouncementCard(
                        name = name,
                        titleText = R.string.pit_announcement_title,
                        bodyText = if (it == "B") R.string.pit_announcement_body_variant_b else
                            R.string.pit_announcement_body_variant_a,
                        ctaText = R.string.pit_announcement_cta_text,
                        iconImage = R.drawable.ic_announce_the_pit,
                        dismissFunction = {
                            host.dismissAnnouncementCard()
                            compositeDisposable.clear()
                        },
                        ctaFunction = {
                        analytics.logEvent(PitAnalyticsEvent.AnnouncementTappedEvent)
                            host.dismissAnnouncementCard()
                            host.startPitLinking()
                            compositeDisposable.clear()
                        },
                        dismissEntry = dismissEntry,
                        dismissRule = DismissRule.CardOneTime
                    )
                )
            }
    }

    override val name = "pit_linking"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "PitAnnouncement_DISMISSED"
    }
}
