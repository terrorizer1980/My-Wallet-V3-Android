package com.blockchain.sunriver.ui

import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R

class ClaimFreeCryptoSuccessDialog : BaseAirdropBottomDialog(
    Content(
        title = R.string.claim_crypto_success_title,
        description = R.string.claim_crypto_success_body,
        ctaButtonText = android.R.string.ok
    )
) {

    private val eventLogger: EventLogger by inject()

    override fun onStart() {
        super.onStart()
        eventLogger.logEvent(LoggableEvent.ClaimFreeCryptoSuccessDialog)
    }

    override fun rocketShipClick() {
        eventLogger.logEvent(LoggableEvent.ClaimFreeCryptoSuccessDialogClickedRocket)
        dismiss()
    }

    override fun ctaButtonClick() {
        eventLogger.logEvent(LoggableEvent.ClaimFreeCryptoSuccessDialogClicked)
        dismiss()
    }
}
