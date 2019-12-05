package piuk.blockchain.android.ui.dashboard.announcements.rule

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class PaxAnnouncement(
    dismissRecorder: DismissRecorder,
    private val analytics: Analytics,
    private val walletStatus: WalletStatus
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }
        return Single.just(walletStatus.isWalletFunded)
    }

    @SuppressLint("CheckResult")
    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(createAnnouncementCard(host))
        analytics.logEvent(
            PaxCardShowingAnalyticsEvent(
                PaxCardShowingAnalyticsEvent.ANALYTICS_OPTION_SHOWING_CARD
            )
        )
    }

    private fun createAnnouncementCard(host: AnnouncementHost) =
        StandardAnnouncementCard(
            name = name,
            dismissEntry = dismissEntry,
            dismissRule = DismissRule.CardOneTime,
            titleText = R.string.stablecoin_announcement_introducing_title,
            bodyText = R.string.stablecoin_announcement_introducing_description,
            ctaText = R.string.stablecoin_announcement_introducing_link,
            iconImage = R.drawable.vector_pax_colored,
            buttonColor = R.color.pax,
            dismissFunction = {
                host.dismissAnnouncementCard()
                analytics.logEvent(
                    PaxCardSeenAnalyticsEvent(
                        PaxCardSeenAnalyticsEvent.ANALYTICS_DISMISS_CLOSED
                    )
                )
            },
            ctaFunction = {
                host.dismissAnnouncementCard()
                host.startSwap(CryptoCurrency.PAX)
                analytics.logEvent(
                    PaxCardSeenAnalyticsEvent(
                        PaxCardSeenAnalyticsEvent.ANALYTICS_DISMISS_CTA_CLICK
                    )
                )
            }
        )

    override val name = "pax"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "StableCoinIntroductionCard_DISMISSED"
    }

    // Sent when either the card or the popup is selected and shown.
    private class PaxCardShowingAnalyticsEvent(val optionShowing: String) : AnalyticsEvent {
        override val event: String
            get() = ANALYTICS_EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_OPTION_SHOWING_PARAM to optionShowing)

        companion object {
            private const val ANALYTICS_EVENT_NAME = "pax_card_showing"
            private const val ANALYTICS_OPTION_SHOWING_PARAM = "Showing"
            const val ANALYTICS_OPTION_SHOWING_CARD = "CARD"
        }
    }

    // Fired when the card/popup is dismissed to track actions
    private class PaxCardSeenAnalyticsEvent(val dismissBy: String) : AnalyticsEvent {
        override val event: String
            get() = ANALYTICS_EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_DISMISS_PARAM to dismissBy)

        companion object {
            private const val ANALYTICS_EVENT_NAME = "pax_card_seen"
            private const val ANALYTICS_DISMISS_PARAM = "Dismissed_by"
            const val ANALYTICS_DISMISS_CTA_CLICK = "CTA_CLICK"
            const val ANALYTICS_DISMISS_CLOSED = "CANCEL_CLOSE"
        }
    }
}
