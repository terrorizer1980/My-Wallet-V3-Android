package piuk.blockchain.android.ui.dashboard.announcements.popups

import android.content.DialogInterface
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.sunriver.ui.BaseAirdropBottomDialog
import info.blockchain.balance.CryptoCurrency
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost

class StablecoinIntroPopup : BaseAirdropBottomDialog(
    Content(
        title = R.string.pax_intro_popup_title,
        description = R.string.pax_intro_popup_text,
        ctaButtonText = R.string.pax_intro_popup_cta_btn,
        dismissText = 0,
        iconDrawable = R.drawable.vector_pax_colored
    )
) {
    private val analytics: Analytics by inject()
    private val analyticsEvent = PaxPopupAnalyticsEvent()

    override fun iconClick() {
        startSwapAndDismiss()
    }

    override fun ctaButtonClick() {
        startSwapAndDismiss()
    }

    private fun startSwapAndDismiss() {
        val host: AnnouncementHost = get()
        host.exchangeRequested(CryptoCurrency.PAX)
        analyticsEvent.dismissBy = ANALYTICS_DISMISS_CTA_CLICK
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        analytics.logEvent(analyticsEvent)
    }

    private class PaxPopupAnalyticsEvent : AnalyticsEvent {
        var dismissBy: String = ANALYTICS_DISMISS_CLOSED

        override val event: String
            get() = ANALYTICS_EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_DISMISS_PARAM to dismissBy)
    }

    companion object {
        private const val ANALYTICS_EVENT_NAME = "pax_popup_seen"
        private const val ANALYTICS_DISMISS_PARAM = "Dismissed_by"
        private const val ANALYTICS_DISMISS_CTA_CLICK = "CTA_CLICK"
        private const val ANALYTICS_DISMISS_CLOSED = "CANCEL_CLOSE"
    }
}