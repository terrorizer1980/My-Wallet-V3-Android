package piuk.blockchain.android.ui.dashboard.announcements

import piuk.blockchain.androidcore.utils.PrefsUtil

/**
 * Maintains a boolean flag for recording if a dialog has been dismissed.
 */
class DismissRecorder(private val prefsUtil: PrefsUtil) {

    operator fun get(key: String) = DismissEntry("${key}_DISMISSED")

    inner class DismissEntry(
        private val prefsKey: String
    ) {
        var isDismissed: Boolean
            get() = prefsUtil.getValue(prefsKey, false)
            set(value) = prefsUtil.setValue(prefsKey, value)
    }
}
