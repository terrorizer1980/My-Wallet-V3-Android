package piuk.blockchain.android.ui.thepit

import piuk.blockchain.androidcoreui.ui.base.View

interface PitPermissionsView : View {
    fun showLoading()
    fun hideLoading()
    fun onLinkSuccess(uuid: String)
    fun onLinkFailed(reason: String)
}