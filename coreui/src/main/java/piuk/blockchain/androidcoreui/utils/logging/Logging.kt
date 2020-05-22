package piuk.blockchain.androidcoreui.utils.logging

import android.content.Context
import android.os.Bundle
import com.blockchain.logging.CustomEventBuilder
import com.crashlytics.android.answers.AddToCartEvent
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.AnswersEvent
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.LoginEvent
import com.crashlytics.android.answers.PurchaseEvent
import com.crashlytics.android.answers.ShareEvent
import com.crashlytics.android.answers.SignUpEvent
import com.crashlytics.android.answers.StartCheckoutEvent
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcoreui.ApplicationLifeCycle
import piuk.blockchain.androidcoreui.BuildConfig
import piuk.blockchain.androidcoreui.utils.logging.crashlytics.buildCrashlyticsEvent


class Logging1 private constructor() {
    private val shouldLog = BuildConfig.USE_CRASHLYTICS
    private lateinit var analytics: FirebaseAnalytics

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(event: LoggingEvent) {
        if(shouldLog) {
            val b = Bundle()
            when(event.param.second) {
                is String -> b.putString(event.param.first, event.param.second as String)
                is Int -> b.putInt(event.param.first, event.param.second as Int)
                is Boolean -> b.putBoolean(event.param.first, event.param.second as Boolean)
            }

            analytics.logEvent(event.identifier, b)
        }
    }

    fun logSignUp(success: Boolean) {
        if(shouldLog) {
            val b = Bundle()
            b.putBoolean(FirebaseAnalytics.Param.METHOD, success)
            analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, b)
        }
    }




    private object HOLDER {
        val INSTANCE = Logging1()
    }

    companion object {
        val instance: Logging1 by lazy { HOLDER.INSTANCE }
    }
}

class LoggingEvent(val identifier: String, val param: Pair<String, Any>)


/**
 * A singleton wrapper for the [Answers] client. All events will only be logged for release.
 *
 * Note: absolutely no identifying information should be included in an [AnswersEvent], ever.
 * These should be used to get a feel for how often features are used, but that's it.
 */
@Suppress("ConstantConditionIf")
object Logging {

    const val ITEM_TYPE_FIAT = "Fiat Currency"
    const val ITEM_TYPE_CRYPTO = "Cryptocurrency"

    private const val shouldLog = BuildConfig.USE_CRASHLYTICS

    fun logCustom(customEvent: CustomEvent) {

        if (shouldLog) Answers.getInstance().logCustom(customEvent)
    }

    fun logCustom(customEvent: CustomEventBuilder) {
        if (shouldLog) logCustom(customEvent.buildCrashlyticsEvent())
    }

    fun logContentView(contentViewEvent: ContentViewEvent) {
        if (shouldLog) Answers.getInstance().logContentView(contentViewEvent)
    }

    fun logLogin(loginEvent: LoginEvent) {
        if (shouldLog) Answers.getInstance().logLogin(loginEvent)
    }

    fun logSignUp(signUpEvent: SignUpEvent) {
        if (shouldLog) Answers.getInstance().logSignUp(signUpEvent)
    }

    fun logShare(shareEvent: ShareEvent) {
        if (shouldLog) Answers.getInstance().logShare(shareEvent)
    }

    fun logPurchase(purchaseEvent: PurchaseEvent) {
        if (shouldLog) Answers.getInstance().logPurchase(purchaseEvent)
    }

    fun logAddToCart(addToCartEvent: AddToCartEvent) {
        if (shouldLog) Answers.getInstance().logAddToCart(addToCartEvent)
    }

    fun logStartCheckout(startCheckoutEvent: StartCheckoutEvent) {
        if (shouldLog) Answers.getInstance().logStartCheckout(startCheckoutEvent)
    }
}