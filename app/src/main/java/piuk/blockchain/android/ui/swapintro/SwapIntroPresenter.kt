package piuk.blockchain.android.ui.swapintro

import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.androidcore.utils.PersistentPrefs

interface SwapIntroView : MvpView

class SwapIntroPresenter(private val prefs: PersistentPrefs) : MvpPresenter<SwapIntroView>() {
    override fun onViewAttached() { }
    override fun onViewDetached() { }

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = true

    fun onGetStartedPressed() {
        prefs.swapIntroCompleted = true
    }
}