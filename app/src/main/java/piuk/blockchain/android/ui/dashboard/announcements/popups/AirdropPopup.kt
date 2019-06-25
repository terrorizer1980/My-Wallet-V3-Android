package piuk.blockchain.android.ui.dashboard.announcements.popups

import com.blockchain.nabu.StartKycAirdrop
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.sunriver.ui.BaseAirdropBottomDialog
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R

class AirdropPopup : BaseAirdropBottomDialog(
    Content(
        title = R.string.get_free_xlm,
        description = R.string.get_free_xlm_body,
        ctaButtonText = R.string.get_free_xlm
    )
) {

    private val analytics: Analytics by inject()

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
}
