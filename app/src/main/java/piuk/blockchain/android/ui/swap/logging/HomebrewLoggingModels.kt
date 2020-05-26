package piuk.blockchain.android.ui.swap.logging

import piuk.blockchain.androidcoreui.utils.logging.LoggingEvent

fun fixTypeEvent(fixType: FixType) =
    LoggingEvent("Fix type switched", mapOf(Pair("Input Type", fixType.name)))

enum class FixType(val type: String) {
    BaseFiat("Base fiat"),
    BaseCrypto("Base crypto"),
    CounterFiat("Counter fiat"),
    CounterCrypto("Counter crypto")
}

fun websocketConnectionFailureEvent() =
    LoggingEvent("Websocket connection failure", mapOf(Pair("Websocket connection failed", true)))

fun amountErrorEvent(errorType: AmountErrorType) =
    LoggingEvent("Min/Max error", mapOf(Pair("Min/Max error type", errorType.error)))

enum class AmountErrorType(val error: String) {
    OverBalance("Over user's balance"),
    OverMax("Over max"),
    UnderMin("Under min")
}