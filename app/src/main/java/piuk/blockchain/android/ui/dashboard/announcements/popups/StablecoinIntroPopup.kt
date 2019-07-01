package piuk.blockchain.android.ui.dashboard.announcements.popups

import android.content.DialogInterface
import android.os.Bundle
import com.blockchain.notifications.analytics.AnalyticsEvent
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost

class StablecoinIntroPopup : BaseAnnouncementBottomDialog(
    Content(
        title = R.string.pax_intro_popup_title,
        description = R.string.pax_intro_popup_text,
        ctaButtonText = R.string.pax_intro_popup_cta_btn,
        dismissText = 0,
        iconDrawable = R.drawable.vector_pax_colored
    )
) {

    private val analyticsEvent = PaxPopupAnalyticsEvent()
    private var host: AnnouncementHost? = null

    override fun iconClick() {
        startSwapAndDismiss()
    }

    override fun ctaButtonClick() {
        startSwapAndDismiss()
    }

    private fun startSwapAndDismiss() {
        host?.startSwapOrKyc(CryptoCurrency.PAX)
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

        fun show(host: AnnouncementHost, dismissKey: String) {
            val popup = StablecoinIntroPopup().apply {
                arguments = Bundle().also {
                    it.putString(ARG_DISMISS_KEY, dismissKey)
                }
            }
            popup.host = host
            host.showAnnouncmentPopup(popup)
        }
    }
}