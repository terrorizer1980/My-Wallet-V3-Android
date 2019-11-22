package piuk.blockchain.android.ui.kyc.status

import android.support.annotation.StringRes
import com.blockchain.swap.nabu.models.nabu.KycState
import piuk.blockchain.androidcoreui.ui.base.View

interface KycStatusView : View {

    fun finishPage()

    fun renderUi(kycState: KycState)

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun startExchange()

    fun showToast(@StringRes message: Int)

    fun showNotificationsEnabledDialog()
}
