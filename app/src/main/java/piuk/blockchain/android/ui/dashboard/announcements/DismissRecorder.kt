package piuk.blockchain.android.ui.dashboard.announcements

import piuk.blockchain.androidcore.utils.PersistentPrefs

/**
 * Maintains a boolean flag for recording if a dialog has been dismissed.
 */
class DismissRecorder(private val prefs: PersistentPrefs) {

    operator fun get(key: String) = DismissEntry(key)

    inner class DismissEntry(val prefsKey: String) {
        var isDismissed: Boolean
            get() = prefs.getValue(prefsKey, false)
            set(value) = prefs.setValue(prefsKey, value)
    }
}
