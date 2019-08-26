package piuk.blockchain.androidcoreui.utils

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.MotionEvent
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.R

class OverlayDetection constructor(private val prefs: PersistentPrefs) {

    private var alertDialog: AlertDialog? = null

    fun detectObscuredWindow(activity: Activity, event: MotionEvent): Boolean {
        // Detect if touch events are being obscured by hidden overlays - These could be used for tapjacking
        if (!prefs.getValue(PersistentPrefs.KEY_OVERLAY_TRUSTED, false) &&
            event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0
        ) {
            if (!activity.isDestroyed) {
                // Prevent Not Attached To Window crash
                alertDialog?.dismiss() // Prevent multiple popups

                alertDialog = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(R.string.screen_overlay_warning)
                    .setMessage(R.string.screen_overlay_note)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue) { _, _ ->
                        prefs.setValue(PersistentPrefs.KEY_OVERLAY_TRUSTED, true)
                    }
                    .setNegativeButton(R.string.exit) { _, _ -> activity.finish() }
                    .show()
            }
            return true
        }
        return false
    }
}