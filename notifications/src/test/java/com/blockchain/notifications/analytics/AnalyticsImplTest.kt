package com.blockchain.notifications.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import piuk.blockchain.androidcore.utils.PersistentPrefs

class AnalyticsImplTest {

    private val mockFirebase = mock<FirebaseAnalytics>()
    private val event = object : AnalyticsEvent {
        override val event: String
            get() = "name"
        override val params: Map<String, String> = emptyMap()
    }

    @Test
    fun `should log custom event`() {
        val mockStore = mock<PersistentPrefs>()

        AnalyticsImpl(mockFirebase, mockStore).logEvent(event)

        verify(mockFirebase).logEvent(event.event, null)
    }

    @Test
    fun `should log once event once`() {
        val mockStore = mock<PersistentPrefs> {
            on { hasSentMetric(any()) } doReturn false
        }

        AnalyticsImpl(mockFirebase, mockStore).logEventOnce(event)
        verify(mockFirebase).logEvent(event.event, null)
    }

    @Test
    fun `should not log once event again`() {
        val mockStore = mock<PersistentPrefs> {
            on { hasSentMetric(any()) } doReturn true
        }

        AnalyticsImpl(mockFirebase, mockStore).logEventOnce(event)
        verify(mockFirebase, never()).logEvent(event.event, null)
    }
}