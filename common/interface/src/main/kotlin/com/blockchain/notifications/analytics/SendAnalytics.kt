package com.blockchain.notifications.analytics

sealed class SendAnalytics(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {
    object SendTabItem : SendAnalytics("send_tab_item_click")
    class SendFormClicked(asset: String) : SendAnalytics("send_tab_item_click")
    class SendSpendableBalanceClicked(asset: String) : SendAnalytics("send_form_use_balance_click")
    class PitButtonClicked(asset: String) : SendAnalytics("send_form_pit_button_click")
    object QRButtonClicked : SendAnalytics("send_form_qr_button_click")
    class SummarySendClick(asset: String) : SendAnalytics("send_summary_confirm_click")
    class SummarySendSuccess(asset: String) : SendAnalytics("send_summary_confirm_success")
    class SummarySendFailure(asset: String) : SendAnalytics("send_summary_confirm_success")
    class BitpaySendFailure(asset: String) : SendAnalytics("send_bitpay_payment_failure")
    class BitpaySendSuccess(asset: String) : SendAnalytics("send_bitpay_payment_success")
}