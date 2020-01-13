package piuk.blockchain.android.simplebuy

interface SimpleBuyScreen {
    fun navigator(): SimpleBuyNavigator
    fun onBackPressed(): Boolean
}

interface SimpleBuyNavigator {
    fun exitSimpleBuyFlow()
    fun goToBuyCryptoScreen()
    fun goToCheckOutScreen()
    fun goToKycVerificationScreen()
    fun goToBankDetailsScreen()
    fun startKyc()
}