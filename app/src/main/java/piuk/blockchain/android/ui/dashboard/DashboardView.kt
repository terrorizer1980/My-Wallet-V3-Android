package piuk.blockchain.android.ui.dashboard

import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialogFragment
import com.blockchain.kycui.navhost.models.CampaignType
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface DashboardView : View {

    val locale: Locale

    fun updatePieChartState(chartsState: PieChartsState)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)

    fun notifyItemAdded(displayItems: MutableList<Any>, position: Int)

    fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int)

    fun notifyItemUpdated(displayItems: MutableList<Any>, positions: List<Int>)

    fun startBuyActivity()

    fun startBitcoinCashReceive()

    fun scrollToTop()

    fun startWebsocketService()

    fun startKycFlow(campaignType: CampaignType)

    fun startPitLinkingFlow(linkId: String = "")

    fun launchWaitlist()

    fun showBottomSheetDialog(bottomSheetDialogFragment: BottomSheetDialogFragment)

    fun goToExchange(currency: CryptoCurrency?, defCurrency: String)
}
