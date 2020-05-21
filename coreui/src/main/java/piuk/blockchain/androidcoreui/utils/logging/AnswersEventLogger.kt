package piuk.blockchain.androidcoreui.utils.logging

import com.blockchain.logging.CustomEventBuilder
import com.blockchain.logging.EventLogger
import com.crashlytics.android.answers.Answers
import com.google.firebase.analytics.FirebaseAnalytics
import piuk.blockchain.androidcoreui.utils.logging.crashlytics.buildCrashlyticsEvent

internal class AnswersEventLogger(private val firebaseAnalytics: FirebaseAnalytics) : EventLogger {

    override fun logEvent(customEventBuilder: CustomEventBuilder) {
        firebaseAnalytics.logEvent(customEventBuilder.buildCrashlyticsEvent())
    }
}