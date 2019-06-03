package piuk.blockchain.android.ui.login

import android.support.annotation.StringRes

import org.json.JSONObject

import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface ManualPairingView : View {

    val guid: String

    val password: String

    fun goToPinPage()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun updateWaitingForAuthDialog(secondsRemaining: Int)

    fun showProgressDialog(@StringRes messageId: Int, suffix: String?, cancellable: Boolean)

    fun dismissProgressDialog()

    fun resetPasswordField()

    fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    )
}
