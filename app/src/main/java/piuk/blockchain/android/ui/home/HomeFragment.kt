package piuk.blockchain.android.ui.home

import com.blockchain.kycui.navhost.models.CampaignType
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.View
import java.lang.IllegalStateException

abstract class HomeFragment<VIEW : View, PRESENTER : BasePresenter<VIEW>>
    : BaseFragment<VIEW, PRESENTER>() {

    fun navigator(): HomeNavigator =
        (activity as? HomeNavigator) ?: throw IllegalStateException("Parent must implement HomeNavigator")

    /*abstract*/ fun refresh(cryptoCurrency: CryptoCurrency) {}
    abstract fun onBackPressed(): Boolean
}

interface HomeNavigator {
    fun showNavigation()
    fun hideNavigation()

    fun gotoDashboard()

    fun launchSwapOrKyc(targetCurrency: CryptoCurrency? = null)

    fun launchKyc(campaignType: CampaignType)

    fun gotoSendFor(cryptoCurrency: CryptoCurrency)
    fun gotoReceiveFor(cryptoCurrency: CryptoCurrency)
    fun gotoTransactionsFor(cryptoCurrency: CryptoCurrency)
}