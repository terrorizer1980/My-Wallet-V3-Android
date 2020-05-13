package piuk.blockchain.android.ui.send.strategy

import com.blockchain.logging.CustomEventBuilder
import info.blockchain.balance.CryptoValue
import piuk.blockchain.androidcoreui.utils.extensions.getBoundary

class PaymentSentEvent : CustomEventBuilder("Payment Sent") {

    fun putSuccess(successful: Boolean): PaymentSentEvent {
        putCustomAttribute("Success", if (successful) "true" else "false")
        return this
    }

    fun putAmountForRange(
        amountSent: CryptoValue
    ): PaymentSentEvent {
        val amountRange =
            amountSent.toBigDecimal().getBoundary() + " " + amountSent.currency.networkTicker

        putCustomAttribute("Amount", amountRange)
        putCustomAttribute("Currency", amountSent.currency.networkTicker)
        return this
    }
}
