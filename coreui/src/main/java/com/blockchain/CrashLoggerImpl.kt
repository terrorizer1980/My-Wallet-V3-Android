package com.blockchain

import android.content.Context
import com.blockchain.logging.CrashLogger
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import io.fabric.sdk.android.Fabric
import piuk.blockchain.androidcoreui.BuildConfig
import timber.log.Timber

@Suppress("ConstantConditionIf")
internal class CrashLoggerImpl(override val isDebugBuild: Boolean) : CrashLogger {

    override fun init(ctx: Any) {
        if (ctx is Context) {
            if (BuildConfig.USE_CRASHLYTICS) {
                // Init crash reporting
                Fabric.with(ctx, Crashlytics(), Answers())
            } else {
                Fabric.with(ctx, Answers())
            }
        } else {
            throw IllegalStateException("Unable to init Crashlytics. No context provided")
        }
    }

    override fun logEvent(msg: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            Crashlytics.log(msg)
        }
    }

    override fun logState(name: String, data: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            Crashlytics.setString(name, data)
        }
    }

    override fun onlineState(isOnline: Boolean) {
        if (BuildConfig.USE_CRASHLYTICS) {
            Crashlytics.setBool(KEY_ONLINE_STATE, isOnline)
        }
    }

    override fun userLanguageLocale(locale: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            Crashlytics.setString(KEY_LOCALE_LANGUAGE, locale)
        }
    }

    override fun logException(throwable: Throwable, logMsg: String) {
        if (BuildConfig.USE_CRASHLYTICS) {
            Crashlytics.logException(throwable)
        }
        Timber.e(throwable, logMsg)
    }

    companion object {
        const val KEY_ONLINE_STATE = "online status"
        const val KEY_LOCALE_LANGUAGE = "user language"
    }
}