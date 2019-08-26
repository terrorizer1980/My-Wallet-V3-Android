package piuk.blockchain.android.ui.recover

import android.support.annotation.StringRes

import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface RecoverFundsView : View {

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showProgressDialog(@StringRes messageId: Int)

    fun dismissProgressDialog()

    fun gotoCredentialsActivity(recoveryPhrase: String)
}
