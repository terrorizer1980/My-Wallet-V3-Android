package piuk.blockchain.android.ui.kyc.logging

import piuk.blockchain.android.ui.kyc.reentry.ReentryPoint
import piuk.blockchain.androidcoreui.utils.logging.LoggingEvent

fun KycResumedEvent1(entryPoint: ReentryPoint) =
    LoggingEvent("User Resumed KYC flow", mapOf(Pair("User resumed KYC", entryPoint.entryPoint)))
