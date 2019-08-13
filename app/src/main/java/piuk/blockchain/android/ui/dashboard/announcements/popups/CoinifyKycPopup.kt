package piuk.blockchain.android.ui.dashboard.announcements.popups

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.blockchain.swap.nabu.StartKycAirdrop
import com.blockchain.notifications.analytics.AnalyticsEvents
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost

class CoinifyKycPopup : BaseAnnouncementBottomDialog(
    Content(
        title = R.string.more_info_needed,
        description = R.string.coinify_kyc_body,
        ctaButtonText = R.string.coinify_kyc_cta,
        dismissText = R.string.coinify_kyc_dismiss,
        iconDrawable = R.drawable.vector_buy_shopping_cart
    )
) {
    private val startKyc: StartKycAirdrop by inject()

    override fun onStart() {
        super.onStart()
        analytics.logEvent(AnalyticsEvents.CoinifyKycBottomDialog)
    }

    override fun iconClick() {
        analytics.logEvent(AnalyticsEvents.CoinifyKycBottomDialogClickedRocket)
        startKycAndDismiss()
    }

    override fun ctaButtonClick() {
        analytics.logEvent(AnalyticsEvents.CoinifyKycBottomDialogClicked)
        startKycAndDismiss()
    }

    override fun dismissButtonClick() {
        dismiss()
        analytics.logEvent(AnalyticsEvents.CoinifyKycBottomDialogLearnMoreClicked)
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.coinify_kyc_learn_more_url))))
    }

    private fun startKycAndDismiss() {
        dismiss()
        startKyc.startKycActivity(activity!!)
    }

    companion object {
        fun show(host: AnnouncementHost, dismissKey: String) {
            val popup = CoinifyKycPopup().apply {
                arguments = Bundle().also {
                    it.putString(ARG_DISMISS_KEY, dismissKey)
                }
            }
            host.showAnnouncmentPopup(popup)
        }
    }
}