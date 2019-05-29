package com.blockchain.kycui.splash

import androidx.navigation.NavDirections
import piuk.blockchain.androidcoreui.ui.base.View

interface KycSplashView : View {
    fun displayLoading(isLoading: Boolean)

    fun goToNextKycStep(direction: NavDirections)

    fun goToBuySellView()

    fun showError(message: String)
}