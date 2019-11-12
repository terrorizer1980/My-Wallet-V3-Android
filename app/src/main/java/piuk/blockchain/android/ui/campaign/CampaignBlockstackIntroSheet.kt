package piuk.blockchain.android.ui.campaign

import android.content.DialogInterface
import android.view.View
import com.blockchain.notifications.analytics.Analytics
import piuk.blockchain.android.R
import kotlinx.android.synthetic.main.dialog_stx_campaign_intro.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog

class CampaignBlockstackIntroSheet : SlidingModalBottomDialog() {

    private val analytics: Analytics by inject()

    private var ctaClickHandler: () -> Unit = { }

    override val layoutResource: Int = R.layout.dialog_stx_campaign_intro
    override fun initControls(view: View) {
        view.cta_button.setOnClickListener { onCtaClick() }
    }

    override fun onStart() {
        super.onStart()
        analytics.logEvent(BlockstackAnalyticsEvent.IntroSheetShown)
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        analytics.logEvent(BlockstackAnalyticsEvent.IntroSheetDismissed)
    }

    private fun onCtaClick() {
        analytics.logEvent(BlockstackAnalyticsEvent.IntroSheetActioned)
        dismiss()
        ctaClickHandler.invoke()
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        onCancel(dialog)
    }

    companion object {
        fun newInstance(ctaClick: () -> Unit) =
            CampaignBlockstackIntroSheet().apply {
                ctaClickHandler = ctaClick
        }
    }
}
