package piuk.blockchain.android.ui.swapintro

import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter

class SwapIntroPresenter(private val prefs: PersistentPrefs) : BasePresenter<SwapIntroView>() {

    override fun onViewReady() {
    }

    fun onGetStartedPressed() {
        prefs.swapIntroCompleted = true
    }
}