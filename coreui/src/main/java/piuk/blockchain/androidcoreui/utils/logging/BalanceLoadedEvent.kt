package piuk.blockchain.androidcoreui.utils.logging

import com.blockchain.logging.CustomEventBuilder

class BalanceLoadedEvent(
    hasBtcBalance: Boolean,
    hasBchBalance: Boolean,
    hasEthBalance: Boolean,
    hasXlmBalance: Boolean,
    hasPaxBalance: Boolean
) : CustomEventBuilder("Balances loaded") {

    init {
        putCustomAttribute("Has BTC balance", hasBtcBalance)
        putCustomAttribute("Has BCH balance", hasBchBalance)
        putCustomAttribute("Has ETH balance", hasEthBalance)
        putCustomAttribute("Has XLM balance", hasXlmBalance)
        putCustomAttribute("Has PAX balance", hasPaxBalance)

        val hasAnyBalance = hasBtcBalance || hasBchBalance || hasEthBalance || hasXlmBalance || hasPaxBalance

        putCustomAttribute("Has any balance", hasAnyBalance)
    }
}
