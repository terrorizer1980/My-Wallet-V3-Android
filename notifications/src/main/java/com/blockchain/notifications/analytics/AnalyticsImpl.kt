package com.blockchain.notifications.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import piuk.blockchain.androidcore.utils.PersistentPrefs

class AnalyticsImpl internal constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val prefs: PersistentPrefs
) : Analytics {

    override fun logEvent(analyticsEvent: AnalyticsEvent) {
        firebaseAnalytics.logEvent(analyticsEvent.event, toBundle(analyticsEvent.params))
    }

    override fun logEventOnce(analyticsEvent: AnalyticsEvent) {
        if (!prefs.hasSentMetric(analyticsEvent.event)) {
            prefs.setMetricAsSent(analyticsEvent.event)
            logEvent(analyticsEvent)
        }
    }

    private fun toBundle(params: Map<String, String>): Bundle? {
        if (params.isEmpty()) return null

        return Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        }
    }
}