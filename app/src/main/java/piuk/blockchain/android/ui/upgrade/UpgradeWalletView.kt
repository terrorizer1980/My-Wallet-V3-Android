package piuk.blockchain.android.ui.upgrade

import androidx.annotation.StringRes

import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

internal interface UpgradeWalletView : View {

    fun showChangePasswordDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun onUpgradeStarted()

    fun onUpgradeCompleted()

    fun onUpgradeFailed()

    fun onBackButtonPressed()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()
}
