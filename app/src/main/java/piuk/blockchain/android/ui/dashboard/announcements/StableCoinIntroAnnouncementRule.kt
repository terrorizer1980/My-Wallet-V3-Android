package piuk.blockchain.android.ui.dashboard.announcements

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.RemoteConfig
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.popups.StablecoinIntroPopup

class StableCoinIntroAnnouncementRule(
    private val featureEnabled: FeatureFlag,
    private val config: RemoteConfig,
    private val analytics: Analytics,
    dismissRecorder: DismissRecorder
) : AnnouncementRule {

    override val dismissKey = DISMISS_KEY
    private val dismissEntry = dismissRecorder[dismissKey]

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }
        return featureEnabled.enabled
    }

    @SuppressLint("CheckResult")
    override fun show(host: AnnouncementHost) {
        config.getABVariant(RemoteConfig.AB_PAX_POPUP)
            .subscribe { enabled ->
                if (enabled) {
                    StablecoinIntroPopup.show(host, DISMISS_KEY)
                } else {
                    host.showAnnouncementCard(createAnnouncementCard(host))
                }
            }
    }

    private fun createAnnouncementCard(host: AnnouncementHost) =
        AnnouncementCard(
            style = AnnouncementStyle.StableCoin,
            title = R.string.stablecoin_announcement_introducing_title,
            description = R.string.stablecoin_announcement_introducing_description,
            link = R.string.stablecoin_announcement_introducing_link,
            closeFunction = {
                dismissEntry.dismiss(DismissRule.DismissForSession)
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
                analytics.logEvent(
                    PaxCardAnalyticsEvent(PaxCardAnalyticsEvent.ANALYTICS_DISMISS_CLOSED)
                )
            },
            linkFunction = {
                dismissEntry.dismiss(DismissRule.DismissForever)
                host.dismissAnnouncementCard(dismissEntry.prefsKey)
                host.startSwapOrKyc(CryptoCurrency.PAX)
                analytics.logEvent(
                    PaxCardAnalyticsEvent(PaxCardAnalyticsEvent.ANALYTICS_DISMISS_CTA_CLICK)
                )
            },
            prefsKey = dismissEntry.prefsKey
        )

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "StableCoinIntroductionCard_DISMISSED"
    }
}

private class PaxCardAnalyticsEvent(val dismissBy: String) : AnalyticsEvent {
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