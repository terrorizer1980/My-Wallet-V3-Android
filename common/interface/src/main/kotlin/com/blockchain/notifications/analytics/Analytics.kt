package com.blockchain.notifications.analytics

interface Analytics {
    fun logEvent(analyticsEvent: AnalyticsEvent)
}

interface AnalyticsEvent {
    val event: String
    val params: Map<String, String>
}