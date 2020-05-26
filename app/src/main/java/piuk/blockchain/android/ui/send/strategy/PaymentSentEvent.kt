package piuk.blockchain.android.ui.send.strategy

import info.blockchain.balance.CryptoValue
import piuk.blockchain.androidcoreui.utils.extensions.getBoundary
import piuk.blockchain.androidcoreui.utils.logging.LoggingEvent

fun paymentSentEvent(success: Boolean, amountSent: CryptoValue) =
    LoggingEvent("Payment Sent", mapOf(
        Pair("Success", success),
        Pair("Amount", amountSent.toBigDecimal().getBoundary() + " " + amountSent.currency.networkTicker),
        Pair("Currency", amountSent.currency.networkTicker)
    ))
