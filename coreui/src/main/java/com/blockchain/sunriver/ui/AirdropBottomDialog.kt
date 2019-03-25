package com.blockchain.sunriver.ui

import com.blockchain.nabu.StartKycAirdrop
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.LoggableEvent
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcoreui.R

class AirdropBottomDialog : BaseAirdropBottomDialog(
    Content(
        title = R.string.get_free_xlm,
        description = R.string.get_free_xlm_body,
        ctaButtonText = R.string.get_free_xlm
    )
) {

    private val eventLogger: EventLogger by inject()

    private val startKyc: StartKycAirdrop by inject()

    override fun onStart() {
        super.onStart()
        eventLogger.logEvent(LoggableEvent.SunRiverBottomDialog)
    }

    override fun ctaButtonClick() {
        eventLogger.logEvent(LoggableEvent.SunRiverBottomDialogClicked)
        startKycAndDismiss()
    }

    override fun rocketShipClick() {
        eventLogger.logEvent(LoggableEvent.SunRiverBottomDialogClicked)
        eventLogger.logEvent(LoggableEvent.SunRiverBottomDialogClickedRocket)
        startKycAndDismiss()
    }

    private fun startKycAndDismiss() {
        dismiss()
        startKyc.startKycActivity(activity!!)
    }
}
