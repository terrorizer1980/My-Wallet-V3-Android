package com.blockchain.notifications.analytics

sealed class AddressAnalytics(
    override val event: String,
    override val params: Map<String, String> = mapOf()
) : AnalyticsEvent {

    object DeleteAddressLabel : AddressAnalytics(DELETE_ADDRESS_EVENT)
    object ImportBTCAddress : AddressAnalytics(IMPORT_BTC_ADDRESS)

    companion object {
        private const val DELETE_ADDRESS_EVENT = "delete_label"
        private const val IMPORT_BTC_ADDRESS = "import"
    }
}