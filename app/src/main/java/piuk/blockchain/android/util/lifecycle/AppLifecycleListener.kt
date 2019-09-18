package piuk.blockchain.android.util.lifecycle

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent

class AppLifecycleListener(private val lifecycleInterestedComponent: LifecycleInterestedComponent) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.FOREGROUNDED)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        lifecycleInterestedComponent.appStateUpdated.onNext(AppState.BACKGROUNDED)
    }
}

enum class AppState {
    FOREGROUNDED, BACKGROUNDED
}