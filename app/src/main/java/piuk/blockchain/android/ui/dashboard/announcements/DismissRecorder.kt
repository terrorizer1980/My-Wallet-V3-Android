package piuk.blockchain.android.ui.dashboard.announcements

import android.support.annotation.VisibleForTesting
import piuk.blockchain.androidcore.utils.PersistentPrefs

/**
 * Maintains a boolean flag for recording if a dialog has been dismissed.
 */

enum class DismissRule {
    DismissForever,
    DismissForSession,
    DismissForNow
}

class DismissRecorder(private val prefs: PersistentPrefs) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val dismissed = mutableSetOf<String>()

    operator fun get(key: String) = DismissEntry(key)

    inner class DismissEntry(val prefsKey: String) {
        val isDismissed: Boolean
            get() = isDismissed(prefsKey)

        fun dismiss(rule: DismissRule) {
            when (rule) {
                DismissRule.DismissForever -> dismissForever(prefsKey)
                DismissRule.DismissForSession -> dismissForSession(prefsKey)
                DismissRule.DismissForNow -> dismissForNow(prefsKey)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun dismissForNow(prefsKey: String) {
        // Do nothing. No need to remember dismiss status
    }

    fun dismissForSession(prefsKey: String) {
        dismissed += prefsKey
    }

    fun dismissForever(prefsKey: String) {
        dismissed += prefsKey
        prefs.setValue(prefsKey, true)
    }

    fun isDismissed(prefsKey: String): Boolean =
        dismissed.contains(prefsKey) || prefs.getValue(prefsKey, false)

    // For debug/QA
    internal fun undismissAll(announcementList: AnnouncementList) {
        announcementList.dismissKeys().forEach { prefs.removeValue(it) }
        dismissed.clear()
    }
}
