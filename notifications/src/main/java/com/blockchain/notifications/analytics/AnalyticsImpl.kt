package com.blockchain.notifications.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsImpl internal constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : Analytics {

    override fun logEvent(analyticsEvent: AnalyticsEvent) {
        firebaseAnalytics.logEvent(analyticsEvent.event, toBundle(analyticsEvent.params))
    }

    private fun toBundle(params: Map<String, String>): Bundle? {
        if (params.isEmpty()) return null

        return Bundle().apply {
            params.forEach { (k, v) -> putString(k, v) }
        }
    }
}