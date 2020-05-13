package com.blockchain.notifications.analytics

import info.blockchain.balance.CryptoCurrency

sealed class TransactionsAnalyticsEvents(
    override val event: String,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent {

    object TabItemClick : TransactionsAnalyticsEvents("transactions_tab_item_click")
    class ItemClick(currency: CryptoCurrency) :
        TransactionsAnalyticsEvents("transactions_list_item_click", mapOf("asset" to currency.networkTicker))

    class ItemShare(currency: CryptoCurrency) :
        TransactionsAnalyticsEvents("transactions_item_share_click", mapOf("asset" to currency.networkTicker))

    class ViewOnWeb(currency: CryptoCurrency) :
        TransactionsAnalyticsEvents("transactions_item_web_view_click", mapOf("asset" to currency.networkTicker))
}