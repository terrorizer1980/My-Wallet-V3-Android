package piuk.blockchain.android.ui.buysell.launcher

import androidx.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View

interface BuySellLauncherView : View {

    fun onStartCoinifyOptIn()

    fun onStartCoinifySignUp()

    fun onStartCoinifyOverview()

    fun showPendingVerificationView()

    fun finishPage()

    fun showErrorToast(@StringRes message: Int)

    fun displayProgressDialog()

    fun dismissProgressDialog()
}