package com.blockchain.notifications.analytics

sealed class SendAnalytics(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {
    object SendTabItem : SendAnalytics("send_tab_item_click")
    class SendFormClicked(asset: String) : SendAnalytics("send_tab_item_click", mapOf("asset" to asset))
    class SendSpendableBalanceClicked(asset: String) :
        SendAnalytics("send_form_use_balance_click", mapOf("asset" to asset))

    class SendFormErrorAppears(asset: String) : SendAnalytics("send_form_error_appear", mapOf("asset" to asset))
    class PitButtonClicked(asset: String) : SendAnalytics("send_form_pit_button_click", mapOf("asset" to asset))
    object QRButtonClicked : SendAnalytics("send_form_qr_button_click")
    class SummarySendClick(asset: String) : SendAnalytics("send_summary_confirm_click",
        mapOf("asset" to asset))

    class SummarySendSuccess(asset: String) : SendAnalytics("send_summary_confirm_success",
        mapOf("asset" to asset))

    class SummarySendFailure(asset: String) : SendAnalytics("send_summary_confirm_failure",
        mapOf("asset" to asset))
}