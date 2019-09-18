package piuk.blockchain.android.ui.home

import android.content.Intent
import android.support.annotation.StringRes
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.data.datamanagers.PromptDlgFactory
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface MainView : View, HomeNavigator {

    @Deprecated("Used for processing deep links. Find a way to get rid of this")
    fun getStartIntent(): Intent

    fun onHandleInput(strUri: String)

    fun startBalanceFragment()

    fun kickToLauncherPage()

    fun showProgressDialog(@StringRes message: Int)

    fun hideProgressDialog()

    fun clearAllDynamicShortcuts()

    fun showMetadataNodeFailure()

    fun setBuySellEnabled(enabled: Boolean, useWebView: Boolean)

    fun setPitEnabled(enabled: Boolean)

    fun showTradeCompleteMsg(txHash: String)

    fun setWebViewLoginDetails(loginDetails: WebViewLoginDetails)

    fun showCustomPrompt(dlgFn: PromptDlgFactory)

    fun showSecondPasswordDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showHomebrewDebugMenu()

    fun enableSwapButton(isEnabled: Boolean)

    fun displayLockboxMenu(lockboxAvailable: Boolean)

    fun showTestnetWarning()

    fun onStartBuySell()

    fun launchSwap(defCurrency: String, targetCrypto: CryptoCurrency? = null)

    fun launchSwapIntro()

    fun refreshDashboard()

    fun shouldIgnoreDeepLinking(): Boolean

    fun displayDialog(@StringRes title: Int, @StringRes message: Int)
}
