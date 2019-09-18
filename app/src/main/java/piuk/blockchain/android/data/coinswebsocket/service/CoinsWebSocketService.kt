package piuk.blockchain.android.data.coinswebsocket.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import com.blockchain.notifications.NotificationsUtil
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.lifecycle.AppState
import piuk.blockchain.android.util.lifecycle.LifecycleInterestedComponent

class CoinsWebSocketService : Service(), MessagesSocketHandler {

    private val binder = LocalBinder()
    private val compositeDisposable = CompositeDisposable()
    private val notificationManager: NotificationManager by inject()
    private val coinsWebSocketStrategy: CoinsWebSocketStrategy by inject()
    private val lifecycleInterestedComponent: LifecycleInterestedComponent by inject()

    override fun onCreate() {
        super.onCreate()
        coinsWebSocketStrategy.setMessagesHandler(this)
        coinsWebSocketStrategy.open()
        compositeDisposable += lifecycleInterestedComponent.appStateUpdated.subscribe {
            if (it == AppState.FOREGROUNDED) {
                coinsWebSocketStrategy.open()
            } else {
                coinsWebSocketStrategy.close()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun triggerNotification(title: String, marquee: String, text: String) {
        val notifyIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        NotificationsUtil(applicationContext, notificationManager).triggerNotification(
            title,
            marquee,
            text,
            R.drawable.ic_launcher_round,
            pendingIntent,
            1000)
    }

    override fun sendBroadcast(intent: String) {
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(intent))
    }

    override fun onDestroy() {
        super.onDestroy()
        coinsWebSocketStrategy.close()
        compositeDisposable.clear()
    }

    private inner class LocalBinder internal constructor() // Empty constructor
        : Binder() {
        // Necessary for implementing bound Android Service
        val service: CoinsWebSocketService
            get() = this@CoinsWebSocketService
    }
}