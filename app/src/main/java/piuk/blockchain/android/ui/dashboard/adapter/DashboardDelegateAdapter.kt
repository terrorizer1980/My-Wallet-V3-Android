package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import com.blockchain.notifications.analytics.Analytics
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.android.ui.dashboard.BalanceFilter
import piuk.blockchain.android.ui.dashboard.PieChartsState
import piuk.blockchain.android.ui.dashboard.adapter.delegates.AssetPriceCardDelegate
import piuk.blockchain.android.ui.dashboard.adapter.delegates.HeaderDelegate
import piuk.blockchain.android.ui.dashboard.adapter.delegates.PieChartDelegate
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementDelegate
import piuk.blockchain.android.ui.dashboard.announcements.MiniAnnouncementDelegate

/**
 * @param context The Activity/Fragment [Context]
 * @param assetSelector A callback for getting the selected coin from the asset balance card
 * @param coinSelector A callback for getting the selected coin from the pie Chart
 * @param balanceModeSelector A callback for getting the selected balance mode from the pie Chart
 */
class DashboardDelegateAdapter(
    context: Context,
    assetSelector: (CryptoCurrency) -> Unit,
    coinSelector: (CryptoCurrency) -> Unit,
    balanceModeSelector: (BalanceFilter) -> Unit,
    analytics: Analytics
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val pieChartDelegate = PieChartDelegate<Any>(context, coinSelector, balanceModeSelector)
    private val assetPriceDelegate = AssetPriceCardDelegate<Any>(context, assetSelector)

    init {
        // Add all necessary AdapterDelegate objects here
        with(delegatesManager) {
            addAdapterDelegate(HeaderDelegate())
            addAdapterDelegate(AnnouncementDelegate(analytics))
            addAdapterDelegate(MiniAnnouncementDelegate(analytics))
            addAdapterDelegate(pieChartDelegate)
            addAdapterDelegate(assetPriceDelegate)
        }
    }

    /**
     * Updates the state of the Balance card without causing a refresh of the entire View.
     */
    fun updatePieChartState(chartsState: PieChartsState) {
        pieChartDelegate.updateChartState(chartsState)
    }
}