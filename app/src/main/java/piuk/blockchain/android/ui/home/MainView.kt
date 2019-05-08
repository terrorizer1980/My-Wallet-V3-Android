package piuk.blockchain.android.ui.home

import android.content.Intent
import android.support.annotation.StringRes
import com.blockchain.kycui.navhost.models.CampaignType
import piuk.blockchain.android.data.datamanagers.PromptDlgFactory
import piuk.blockchain.androidbuysell.models.WebViewLoginDetails
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

interface MainView : View {

    @Deprecated("Used for processing deep links. Find a way to get rid of this")
    fun getStartIntent(): Intent

    fun onScanInput(strUri: String)

    fun startBalanceFragment(paymentToContactMade: Boolean)

    fun kickToLauncherPage()

    fun showProgressDialog(@StringRes message: Int)

    fun hideProgressDialog()

    fun clearAllDynamicShortcuts()

    fun showMetadataNodeFailure()

    fun setBuySellEnabled(enabled: Boolean, useWebView: Boolean)

    fun showTradeCompleteMsg(txHash: String)

    fun setWebViewLoginDetails(loginDetails: WebViewLoginDetails)

    fun showCustomPrompt(dlgFn: PromptDlgFactory)

    fun showSecondPasswordDialog()

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun showExchange()

    fun hideExchange()

    fun showTestnetWarning()

    fun onStartLegacyBuySell()

    fun onStartBuySell()

    fun showHomebrewDebug()

    fun displayLockbox(lockboxAvailable: Boolean)

    fun launchKyc(campaignType: CampaignType)

    fun refreshDashboard()

    fun displayDialog(@StringRes title: Int, @StringRes message: Int)
}
