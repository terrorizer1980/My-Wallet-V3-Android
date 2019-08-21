package piuk.blockchain.android.ui.kyc.splash

import androidx.navigation.NavDirections
import piuk.blockchain.androidcoreui.ui.base.View

interface KycSplashView : View {
    fun displayLoading(isLoading: Boolean)

    fun goToNextKycStep(direction: NavDirections)

    fun goToBuySellView()

    fun showError(message: String)

    fun onEmailNotVerified()
}