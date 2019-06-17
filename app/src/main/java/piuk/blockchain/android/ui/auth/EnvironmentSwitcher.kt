package piuk.blockchain.android.ui.auth

import android.app.LauncherActivity
import android.content.Context
import android.support.v7.app.AlertDialog
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.android.util.AppRate
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.toast

internal class EnvironmentSwitcher(
    private val context: Context,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val loginState: AccessState
) {

    fun showDebugMenu() {
        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle("Debug settings")
            .setMessage(
                "Select 'Reset Prefs' to reset various device timers and saved states, such as warning " +
                    "dialogs, onboarding etc.\n\nSelect 'Wipe Wallet' to log out and completely reset this app."
            )
            .setPositiveButton("Reset Prefs") { _, _ -> resetPrefs() }
            .setNegativeButton("Reset Wallet") { _, _ ->
                appUtil.clearCredentialsAndRestart(
                    LauncherActivity::class.java
                )
            }
            .setPositiveButton("Randomise Device Id") { _, _ -> randomiseDeviceId() }
            .setNeutralButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    private fun resetPrefs() {
        prefs.clear()

        AppRate.reset(context)
        loginState.pin = null

        context.toast("Prefs Reset")
    }

    private fun randomiseDeviceId() {
        prefs.qaRandomiseDeviceId = true
        context.toast("Device ID randomisation enabled")
    }
}
