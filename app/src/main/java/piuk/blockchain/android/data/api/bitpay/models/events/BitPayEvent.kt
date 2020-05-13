package piuk.blockchain.android.data.api.bitpay.models.events

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsEvents
import info.blockchain.balance.CryptoCurrency
import java.math.BigInteger

sealed class BitPayEvent(override val event: String, override val params: Map<String, String>) : AnalyticsEvent {

    data class InputEvent(val _event: String, val currency: CryptoCurrency) :
        BitPayEvent(_event, mapOf("currency" to currency.networkTicker))

    data class SuccessEvent(val amount: BigInteger, val currency: CryptoCurrency) :
        BitPayEvent(AnalyticsEvents.BitpayPaymentSucceed.event,
            mapOf("amount" to amount.toString(), "currency" to currency.networkTicker))

    data class FailureEvent(val message: String) :
        BitPayEvent(AnalyticsEvents.BitpayPaymentFailed.event, mapOf("error_message_string" to message))

    object ExpiredEvent :
        BitPayEvent(AnalyticsEvents.BitpayPaymentExpired.event, emptyMap())
}