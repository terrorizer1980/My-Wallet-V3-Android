package piuk.blockchain.android.timber

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        when (t) {
            null -> when (priority) {
                Log.WARN, Log.ERROR -> Crashlytics.log(priority, tag, message)
            }
            else -> Crashlytics.logException(t)
        }
    }
}
