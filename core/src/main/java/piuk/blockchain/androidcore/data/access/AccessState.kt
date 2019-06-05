package piuk.blockchain.androidcore.data.access

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrefsUtil

class AccessState(
    val context: Context,
    val prefs: PersistentPrefs,
    val rxBus: RxBus
) {

    var canAutoLogout = true

    private var logoutActivity: Class<*>? = null
    private var logoutPendingIntent: PendingIntent? = null

    var pin: String? = null

    var isLoggedIn = false
        set(loggedIn) {
            logIn()
            field = loggedIn
            if (this.isLoggedIn) {
                rxBus.emitEvent(AuthEvent::class.java, AuthEvent.LOGIN)
            } else {
                rxBus.emitEvent(AuthEvent::class.java, AuthEvent.LOGOUT)
            }
        }

    var isNewlyCreated: Boolean
        get() = prefs.getValue(PrefsUtil.KEY_NEWLY_CREATED_WALLET, false)
        set(newlyCreated) = prefs.setValue(PrefsUtil.KEY_NEWLY_CREATED_WALLET, newlyCreated)

    /**
     * Called from BaseAuthActivity#onPause()
     */
    fun startLogoutTimer() {
        if (canAutoLogout) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT_MILLIS,
                logoutPendingIntent
            )
        }
    }

    fun setLogoutActivity(logoutActivity: Class<*>) {
        this.logoutActivity = logoutActivity

        val intent = Intent(context, logoutActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.action = LOGOUT_ACTION
        logoutPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
    }

    /**
     * Called from BaseAuthActivity#onResume()
     */
    fun stopLogoutTimer() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(logoutPendingIntent)
    }

    fun logout() {
        pin = null
        val intent = Intent(context, logoutActivity!!)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.action = LOGOUT_ACTION
        context.startActivity(intent)
    }

    fun logIn() = prefs.logIn()

    fun unpairWallet() {
        pin = null
        prefs.logOut()
        rxBus.emitEvent(AuthEvent::class.java, AuthEvent.UNPAIR)
    }

    fun forgetWallet() = rxBus.emitEvent(AuthEvent::class.java, AuthEvent.FORGET)

    companion object {
        private const val LOGOUT_TIMEOUT_MILLIS = 1000L * 30L

        const val LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT"
    }
}