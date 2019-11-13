package piuk.blockchain.android.ui.campaign

import android.content.DialogInterface
import android.content.Intent
import android.view.View
import com.blockchain.notifications.analytics.Analytics
import kotlinx.android.synthetic.main.dialog_stx_campaign_complete.view.cta_button
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class CampaignBlockstackCompleteSheet : SlidingModalBottomDialog() {

    private val analytics: Analytics by inject()

    override val layoutResource: Int = R.layout.dialog_stx_campaign_complete

    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    override fun onStart() {
        super.onStart()
        analytics.logEvent(BlockstackAnalyticsEvent.CompletionSheetShown)
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        analytics.logEvent(BlockstackAnalyticsEvent.CompletionSheetDismissed)
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        onCancel(dialog)
    }

    private fun onCtaClick() {
        analytics.logEvent(BlockstackAnalyticsEvent.CompletionSheetActioned)
        showShareSheet()
        dismiss()
    }

    private fun showShareSheet() {
        val shareMessage = getString(R.string.stacks_airdrop_share_msg)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }
}
