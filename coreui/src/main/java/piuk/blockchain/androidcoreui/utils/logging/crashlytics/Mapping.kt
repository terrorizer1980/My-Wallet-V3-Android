package piuk.blockchain.androidcoreui.utils.logging.crashlytics

import com.blockchain.logging.CustomEventBuilder
import com.google.firebase.events.Event

fun CustomEventBuilder.buildCrashlyticsEvent() =
    Event(eventName)
        .apply {
            build { key, value ->
                putCustomAttribute(key, value)
            }
        }
