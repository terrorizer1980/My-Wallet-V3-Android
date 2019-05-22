package piuk.blockchain.android.ui.send.external

import android.support.annotation.StringRes

interface BaseSendView : piuk.blockchain.androidcoreui.ui.base.View {
    fun showProgressDialog(@StringRes title: Int)
    fun dismissProgressDialog()
}
