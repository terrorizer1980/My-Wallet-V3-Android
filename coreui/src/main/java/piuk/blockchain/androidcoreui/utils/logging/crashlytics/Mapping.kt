package piuk.blockchain.androidcoreui.utils.logging.crashlytics

import com.blockchain.logging.CustomEventBuilder

fun CustomEventBuilder.buildCrashlyticsEvent() =
    CustomEvent(eventName)
        .apply {
            build { key, value ->
                putCustomAttribute(key, value)
            }
        }
