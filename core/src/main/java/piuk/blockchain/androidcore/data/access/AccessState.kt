package piuk.blockchain.androidcore.data.access

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PersistentPrefs

interface AccessState {

    var canAutoLogout: Boolean

    var pin: String?

    var isLoggedIn: Boolean

    var isNewlyCreated: Boolean

    fun startLogoutTimer()

    fun setLogoutActivity(logoutActivity: Class<*>)

    fun stopLogoutTimer()

    fun logout()

    fun logIn()

    fun unpairWallet()

    fun forgetWallet()

    companion object {
        const val LOGOUT_ACTION = "info.blockchain.wallet.LOGOUT"
    }
}

internal class AccessStateImpl(
    val context: Context,
    val prefs: PersistentPrefs,
    val rxBus: RxBus
) : AccessState {

    override var canAutoLogout = true

    private var logoutActivity: Class<*>? = null
    private var logoutPendingIntent: PendingIntent? = null

    override var pin: String? = null

    override var isLoggedIn = false
        set(loggedIn) {
            logIn()
            field = loggedIn
            if (this.isLoggedIn) {
                rxBus.emitEvent(AuthEvent::class.java, AuthEvent.LOGIN)
            } else {
                rxBus.emitEvent(AuthEvent::class.java, AuthEvent.LOGOUT)
            }
        }

    override var isNewlyCreated: Boolean
        get() = prefs.getValue(PersistentPrefs.KEY_NEWLY_CREATED_WALLET, false)
        set(newlyCreated) = prefs.setValue(PersistentPrefs.KEY_NEWLY_CREATED_WALLET, newlyCreated)

    /**
     * Called from BaseAuthActivity#onPause()
     */
    override fun startLogoutTimer() {
        if (canAutoLogout) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + LOGOUT_TIMEOUT_MILLIS,
                logoutPendingIntent
            )
        }
    }

    override fun setLogoutActivity(logoutActivity: Class<*>) {
        this.logoutActivity = logoutActivity

        val intent = Intent(context, logoutActivity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.action = AccessState.LOGOUT_ACTION
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
    override fun stopLogoutTimer() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(logoutPendingIntent)
    }

    override fun logout() {
        pin = null
        val intent = Intent(context, logoutActivity!!)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.action = AccessState.LOGOUT_ACTION
        context.startActivity(intent)
    }

    override fun logIn() = prefs.logIn()

    override fun unpairWallet() {
        pin = null
        prefs.logOut()
        rxBus.emitEvent(AuthEvent::class.java, AuthEvent.UNPAIR)
    }

    override fun forgetWallet() = rxBus.emitEvent(AuthEvent::class.java, AuthEvent.FORGET)

    companion object {
        private const val LOGOUT_TIMEOUT_MILLIS = 1000L * 30L
    }
}