package piuk.blockchain.android.ui.dashboard.sheets

import android.view.View
import com.blockchain.notifications.analytics.SimpleBuyAnalytics
import kotlinx.android.synthetic.main.dialog_custodial_intro.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class CustodyWalletIntroSheet : SlidingModalBottomDialog() {

    override val layoutResource: Int = R.layout.dialog_custodial_intro

    override fun initControls(view: View) {
        analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_SHOWN)
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaClick() {
        analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_CLICKED)
        dismiss()
    }

    companion object {
        fun newInstance(): CustodyWalletIntroSheet =
            CustodyWalletIntroSheet()
    }
}
