package piuk.blockchain.android.kyc.morphmethod

import com.blockchain.koin.modules.MorphMethodType
import com.blockchain.koin.modules.MorphMethodTypeSelector
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import io.reactivex.Single

fun MorphMethodTypeSelector.logCalls(analytics: Analytics): MorphMethodTypeSelector =
    LoggingMorphMethodTypeSelectorSelector(this, analytics)

private class LoggingMorphMethodTypeSelectorSelector(
    private val inner: MorphMethodTypeSelector,
    private val analytics: Analytics
) : MorphMethodTypeSelector {

    override fun getMorphMethod(): Single<MorphMethodType> {
        analytics.logEvent(AnalyticsEvents.Exchange)
        return inner.getMorphMethod()
    }
}
