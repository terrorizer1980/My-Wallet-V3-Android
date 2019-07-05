package piuk.blockchain.android.ui.thepit

interface PitPermissionsView : piuk.blockchain.androidcoreui.ui.base.View {
    fun promptForEmailVerification(email: String)
    fun showEmailVerifiedDialog()
}