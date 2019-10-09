package com.blockchain.notifications.analytics

sealed class TransactionsAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent {

    object TabItemClick : TransactionsAnalyticsEvents("transactions_tab_item_click")
    class ItemClick(currency: String) :
        TransactionsAnalyticsEvents("transactions_list_item_click", mapOf("asset" to currency))

    class ItemShare(currency: String) :
        TransactionsAnalyticsEvents("transactions_item_share_click", mapOf("asset" to currency))

    class ViewOnWeb(currency: String) :
        TransactionsAnalyticsEvents("transactions_item_web_view_click", mapOf("asset" to currency))
}