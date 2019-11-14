package piuk.blockchain.android.ui.campaign

import android.content.DialogInterface
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.View
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.ui.urllinks.URL_STX_AIRDROP_UNAVAILABLE_SUPPORT
import piuk.blockchain.android.R
import kotlinx.android.synthetic.main.dialog_stx_campaign_intro.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.util.StringUtils

class CampaignBlockstackIntroSheet : SlidingModalBottomDialog() {

    private val stringUtils: StringUtils by inject()
    private val analytics: Analytics by inject()

    private var ctaClickHandler: () -> Unit = { }

    override val layoutResource: Int = R.layout.dialog_stx_campaign_intro

    override fun initControls(view: View) {

        val smallPrintText = stringUtils.getStringWithMappedLinks(
            R.string.campaign_stx_intro_small_print,
            mapOf("stx_airdrop_learn_more" to Uri.parse(URL_STX_AIRDROP_UNAVAILABLE_SUPPORT)),
            requireActivity()
        )

        view.small_print.text = smallPrintText
        view.small_print.movementMethod = LinkMovementMethod.getInstance()

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
