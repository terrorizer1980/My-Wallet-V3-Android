package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.support.annotation.VisibleForTesting
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.OnBoardingPrefs
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.tour.IntroTourAnalyticsEvent

class IntroTourAnnouncement(
    dismissRecorder: DismissRecorder,
    private val prefs: OnBoardingPrefs,
    private val analytics: Analytics
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }
        return Single.just(!prefs.isTourComplete)
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = AnnouncementCard(
                titleText = R.string.tour_card_title,
                bodyText = R.string.tour_card_body,
                iconImage = R.drawable.ic_blockchain_logo,
                ctaText = R.string.tour_card_cta,
                dismissText = R.string.tour_card_dismiss,
                dismissFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                    analytics.logEvent(IntroTourAnalyticsEvent.IntroDismissedAnalytics)
                },
                ctaFunction = {
                    host.dismissAnnouncementCard(dismissEntry.prefsKey)
                    host.startIntroTourGuide()
                    analytics.logEvent(IntroTourAnalyticsEvent.IntroStartedAnalytics)
                },
                dismissEntry = dismissEntry,
                dismissRule = DismissRule.CardPeriodic
            )
        )
        analytics.logEvent(IntroTourAnalyticsEvent.IntroOfferedAnalytics)
    }

    override val name = "wallet_intro"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "IntroTourAnnouncement_DISMISSED"
    }
}
