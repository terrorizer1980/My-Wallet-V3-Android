package piuk.blockchain.android.ui.kyc.logging

import piuk.blockchain.android.ui.kyc.reentry.ReentryPoint
import com.blockchain.logging.CustomEventBuilder

internal class KycResumedEvent(entryPoint: ReentryPoint) : CustomEventBuilder("User Resumed KYC flow") {

    init {
        putCustomAttribute("User resumed KYC", entryPoint.entryPoint)
    }
}
