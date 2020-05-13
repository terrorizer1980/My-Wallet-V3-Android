package com.blockchain.notifications.analytics

import info.blockchain.balance.CryptoCurrency

sealed class SendAnalytics(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {
    object SendTabItem : SendAnalytics("send_tab_item_click")
    class SendFormClicked(cryptoCurrency: CryptoCurrency) :
        SendAnalytics("send_tab_item_click", mapOf("asset" to cryptoCurrency.networkTicker))

    class SendSpendableBalanceClicked(cryptoCurrency: CryptoCurrency) :
        SendAnalytics("send_form_use_balance_click", mapOf("asset" to cryptoCurrency.networkTicker))

    class SendFormErrorAppears(cryptoCurrency: CryptoCurrency) :
        SendAnalytics("send_form_error_appear", mapOf("asset" to cryptoCurrency.networkTicker))

    class PitButtonClicked(cryptoCurrency: CryptoCurrency) :
        SendAnalytics("send_form_pit_button_click", mapOf("asset" to cryptoCurrency.networkTicker))

    class SummarySendClick(cryptoCurrency: CryptoCurrency) :
        SendAnalytics("send_summary_confirm_click", mapOf("asset" to cryptoCurrency.networkTicker))

    class SummarySendSuccess(cryptoCurrency: CryptoCurrency) :
        SendAnalytics("send_summary_confirm_success", mapOf("asset" to cryptoCurrency.networkTicker))

    class SummarySendFailure(cryptoCurrency: CryptoCurrency) :
        SendAnalytics("send_summary_confirm_failure", mapOf("asset" to cryptoCurrency.networkTicker))

    object QRButtonClicked : SendAnalytics("send_form_qr_button_click")
}