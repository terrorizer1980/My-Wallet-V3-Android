package com.blockchain.sunriver.ui

import android.content.Intent
import android.net.Uri
import com.blockchain.nabu.StartKyc
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R

class CoinifyKycBottomDialog : BaseAirdropBottomDialog(
    Content(
        title = R.string.more_info_needed,
        description = R.string.coinify_kyc_body,
        ctaButtonText = R.string.coinify_kyc_cta,
        dismissText = R.string.coinify_kyc_dismiss
    )
) {

    private val eventLogger: EventLogger by inject()

    // TODO: should register for campaign before starting KYC
    private val startKyc: StartKyc by inject()

    override fun onStart() {
        super.onStart()
        eventLogger.logEvent(LoggableEvent.CoinifyKycBottomDialog)
    }

    override fun rocketShipClick() {
        eventLogger.logEvent(LoggableEvent.CoinifyKycBottomDialogClickedRocket)
        startKycAndDismiss()
    }

    override fun ctaButtonClick() {
        eventLogger.logEvent(LoggableEvent.CoinifyKycBottomDialogClicked)
        startKycAndDismiss()
    }

    override fun dismissButtonClick() {
        dismiss()
        eventLogger.logEvent(LoggableEvent.CoinifyKycBottomDialogLearnMoreClicked)
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.coinify_kyc_learn_more_url))))
    }

    private fun startKycAndDismiss() {
        dismiss()
        startKyc.startKycActivity(activity!!)
    }
}