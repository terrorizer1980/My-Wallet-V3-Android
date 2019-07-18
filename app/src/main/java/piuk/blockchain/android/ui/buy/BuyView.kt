package piuk.blockchain.android.ui.buy

import android.support.annotation.StringRes
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface BuyView : View {

    fun setUiState(@UiState.UiStateDef uiState: Int)

    fun setWebViewLoginDetails(webViewLoginDetails: WebViewLoginDetails)

    fun showSecondPasswordDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)
}
