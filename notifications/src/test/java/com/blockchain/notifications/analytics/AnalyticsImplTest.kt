package com.blockchain.notifications.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test

class AnalyticsImplTest {

    @Test
    fun `should log custom event`() {
        val mockFirebase = mock<FirebaseAnalytics>()
        val event = object : AnalyticsEvent {
            override val event: String
                get() = "name"
            override val params: Map<String, String> = emptyMap()
        }

        AnalyticsImpl(mockFirebase).logEvent(event)

        verify(mockFirebase).logEvent(event.event, null)
    }
}