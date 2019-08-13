package piuk.blockchain.android.ui.dashboard.announcements.popups

import android.os.Bundle
import com.blockchain.swap.nabu.StartKycAirdrop
import com.blockchain.notifications.analytics.AnalyticsEvents
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.androidcoreui.R

class StellarModelPopup : BaseAnnouncementBottomDialog(
    Content(
        title = R.string.get_free_xlm,
        description = R.string.get_free_xlm_body,
        ctaButtonText = R.string.get_free_xlm
    )
) {
    private val startKyc: StartKycAirdrop by inject()

    override fun onStart() {
        super.onStart()
        analytics.logEvent(AnalyticsEvents.SunRiverBottomDialog)
    }

    override fun ctaButtonClick() {
        analytics.logEvent(AnalyticsEvents.SunRiverBottomDialogClicked)
        startKycAndDismiss()
    }

    override fun iconClick() {
        analytics.logEvent(AnalyticsEvents.SunRiverBottomDialogClicked)
        analytics.logEvent(AnalyticsEvents.SunRiverBottomDialogClickedRocket)
        startKycAndDismiss()
    }

    private fun startKycAndDismiss() {
        dismiss()
        startKyc.startKycActivity(activity!!)
    }

    companion object {
        fun show(host: AnnouncementHost, dismissKey: String) {

            val popup = StellarModelPopup().apply {
                arguments = Bundle().also {
                    it.putString(ARG_DISMISS_KEY, dismissKey)
                }
            }
            host.showAnnouncmentPopup(popup)
        }
    }
}
