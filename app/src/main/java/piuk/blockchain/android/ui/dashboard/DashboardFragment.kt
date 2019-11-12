package piuk.blockchain.android.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.campaign.CampaignType
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_dashboard.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.service.CoinsWebSocketService
import piuk.blockchain.android.data.websocket.WebSocketService
import piuk.blockchain.android.ui.campaign.CampaignBlockstackCompleteSheet
import piuk.blockchain.android.ui.charts.ChartsActivity
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.ui.home.HomeFragment
import piuk.blockchain.android.ui.home.MainActivity.Companion.ACCOUNT_EDIT
import piuk.blockchain.android.ui.home.MainActivity.Companion.SETTINGS_EDIT
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.util.OSUtil
import piuk.blockchain.android.util.start
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import java.util.Locale

class DashboardFragment : HomeFragment<DashboardView, DashboardPresenter>(),
    DashboardView {

    override val locale: Locale by inject()

    private val dashboardPresenter: DashboardPresenter by inject()

    private val osUtil: OSUtil by inject()

    private val analytics: Analytics by inject()

    private val rxBus: RxBus by inject()

    private val dashboardAdapter by unsafeLazy {
        DashboardDelegateAdapter(
            context!!,
            { ChartsActivity.start(context!!, it) },
            { showTransactionsFor(it) },
            { presenter.setBalanceFilter(it) },
            analytics
        )
    }

    private val compositeDisposable = CompositeDisposable()

    private val event by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private fun showTransactionsFor(cryptoCurrency: CryptoCurrency) {
        navigator().gotoTransactionsFor(cryptoCurrency)
    }

    private val spacerDecoration: BottomSpacerDecoration by unsafeLazy {
        BottomSpacerDecoration(ViewUtils.convertDpToPixel(56f, context).toInt())
    }

    private val safeLayoutManager by unsafeLazy { SafeLayoutManager(context!!) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_dashboard)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)

        recycler_view?.apply {
            layoutManager = safeLayoutManager
            adapter = dashboardAdapter
        }

        onViewReady()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()

        navigator().showNavigation()

        compositeDisposable += event.subscribe {
            if (activity != null) {
                // Update balances
                presenter?.updateBalances()
            }
        }

        recycler_view?.scrollToPosition(0)
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SETTINGS_EDIT,
            ACCOUNT_EDIT -> presenter.updateBalances()
            KYC_FOR_STX -> if (resultCode == KycNavHostActivity.RESULT_KYC_STX_COMPLETE)
                showBottomSheetDialog(CampaignBlockstackCompleteSheet())
        }
    }

    override fun scrollToTop() {
        safeLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    override fun notifyItemAdded(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemInserted(position)
        handleRecyclerViewUpdates()
    }

    override fun notifyItemUpdated(displayItems: MutableList<Any>, positions: List<Int>) {
        dashboardAdapter.items = displayItems
        positions.forEach { dashboardAdapter.notifyItemChanged(it) }
        handleRecyclerViewUpdates()
    }

    override fun notifyItemRemoved(displayItems: MutableList<Any>, position: Int) {
        dashboardAdapter.items = displayItems
        dashboardAdapter.notifyItemRemoved(position)
    }

    override fun updatePieChartState(chartsState: PieChartsState) {
        dashboardAdapter.updatePieChartState(chartsState)
        handleRecyclerViewUpdates()
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    override fun startBuySell() {
        navigator().launchBuySell()
    }

    override fun startSwap(defCurrency: String, currency: CryptoCurrency?) {
        navigator().launchSwap(defCurrency, currency)
    }

    override fun startBitcoinCashReceive() {
        navigator().gotoReceiveFor(CryptoCurrency.BCH)
    }

    override fun startKycFlow(campaignType: CampaignType) {
        navigator().launchKyc(campaignType)
    }

    override fun startKycForStx() {
        KycNavHostActivity.startForResult(this, CampaignType.Blockstack, KYC_FOR_STX)
    }

    override fun startPitLinkingFlow(linkId: String) {
        navigator().launchThePitLinking(linkId)
    }

    override fun startBackupWallet() {
        navigator().launchBackupFunds()
    }

    override fun startSetup2Fa() {
        navigator().launchSetup2Fa()
    }

    override fun startVerifyEmail() {
        navigator().launchVerifyEmail()
    }

    override fun startEnableFingerprintLogin() {
        navigator().launchSetupFingerprintLogin()
    }

    override fun startIntroTour() {
        navigator().launchIntroTour()
    }

    override fun startTransferCrypto() {
        navigator().launchTransfer()
    }

    override fun startWebsocketService() {
        context?.run {
            CoinsWebSocketService::class.java.start(this, osUtil)
            WebSocketService::class.java.start(this, osUtil)
        }
    }

    override fun launchWaitlist() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.blockchain.com/getcrypto")
            )
        )
    }

    override fun createPresenter() = dashboardPresenter

    override fun getMvpView() = this

    /**
     * Inserts a spacer into the last position in the list
     */
    private fun handleRecyclerViewUpdates() {
        recycler_view?.apply {
            removeItemDecoration(spacerDecoration)
            addItemDecoration(spacerDecoration)
        }
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).supportActionBar?.let {
            (activity as ToolBarActivity).setupToolbar(it, R.string.dashboard_title)
        }
    }

    override fun onBackPressed() = false

    companion object {

        internal const val KYC_FOR_STX = 9267

        @JvmStatic
        fun newInstance(): DashboardFragment {
            return DashboardFragment()
        }
    }

    override fun showBottomSheetDialog(bottomSheetDialogFragment: BottomSheetDialogFragment) {
        bottomSheetDialogFragment.show(fragmentManager, "BOTTOM_DIALOG")
    }

    /**
     * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
     */
    private inner class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun supportsPredictiveItemAnimations() = false
    }
}