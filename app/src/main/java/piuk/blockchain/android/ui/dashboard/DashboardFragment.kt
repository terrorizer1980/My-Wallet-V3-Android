package piuk.blockchain.android.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.AnalyticsEvents
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_dashboard.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.android.ui.dashboard.adapter.DashboardDelegateAdapter
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailSheet
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus

class EmptyDashboardItem : DashboardItem

class DashboardFragment : HomeScreenMviFragment<DashboardModel, DashboardIntent, DashboardState>(),
    AssetDetailSheet.Host {

    override val model: DashboardModel by inject()

    private val theAdapter: DashboardDelegateAdapter by lazy {
        DashboardDelegateAdapter(
            prefs = get(),
            onCardClicked = { onAssetClicked(it) },
            analytics = get()
        )
    }

    private lateinit var theLayoutManager: RecyclerView.LayoutManager

    private val displayList = mutableListOf<DashboardItem>()

    // TODO: This should be handled by the model
    private val compositeDisposable = CompositeDisposable()
    private val rxBus: RxBus by inject()

    private val actionEvent by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private val metadataEvent by unsafeLazy {
        rxBus.register(MetadataEvent::class.java)
    }

    private var state: DashboardState? = null // Hold the 'current' display state, to enable optimising of state updates

    @UiThread
    override fun render(newState: DashboardState) {

        swipe.isRefreshing = false

        if (displayList.isEmpty()) {
            createDisplayList(newState)
        } else {
            updateDisplayList(newState)
        }

        // Update/show bottom sheet
        if (this.state?.showAssetSheetFor != newState.showAssetSheetFor) {
            showAssetSheet(newState.showAssetSheetFor)
        }

        this.state = newState
    }

    private fun createDisplayList(newState: DashboardState) {
        with(displayList) {
            add(IDX_CARD_ANNOUNCE, EmptyDashboardItem()) // Placeholder for announcements
            add(IDX_CARD_BALANCE, newState)
            add(IDX_CARD_BTC, newState.assets[CryptoCurrency.BTC])
            add(IDX_CARD_ETH, newState.assets[CryptoCurrency.ETHER])
            add(IDX_CARD_BCH, newState.assets[CryptoCurrency.BCH])
            add(IDX_CARD_XLM, newState.assets[CryptoCurrency.XLM])
            add(IDX_CARD_PAX, newState.assets[CryptoCurrency.PAX])
        }
        theAdapter.notifyDataSetChanged()
    }

    private fun updateDisplayList(newState: DashboardState) {
        with(displayList) {
            if (get(IDX_CARD_ANNOUNCE) != EmptyDashboardItem()) { // Placeholder for announcements
                // Currently always false
            }

            var isModified = false
            isModified = isModified || handleUpdatedAssetState(IDX_CARD_BTC, newState.assets[CryptoCurrency.BTC])
            isModified = isModified || handleUpdatedAssetState(IDX_CARD_ETH, newState.assets[CryptoCurrency.ETHER])
            isModified = isModified || handleUpdatedAssetState(IDX_CARD_BCH, newState.assets[CryptoCurrency.BCH])
            isModified = isModified || handleUpdatedAssetState(IDX_CARD_XLM, newState.assets[CryptoCurrency.XLM])
            isModified = isModified || handleUpdatedAssetState(IDX_CARD_PAX, newState.assets[CryptoCurrency.PAX])

            if (isModified) {
                set(IDX_CARD_BALANCE, newState)
                theAdapter.notifyItemChanged(IDX_CARD_BALANCE)
            }
        }
    }

    private fun handleUpdatedAssetState(index: Int, newState: AssetModel): Boolean =
        if (displayList[index] != newState) {
            displayList[index] = newState
            theAdapter.notifyItemChanged(index)
            true
        } else {
            false
        }

    private fun showAssetSheet(sheetFor: CryptoCurrency?) {
        if (sheetFor != null) {
            showBottomSheet(AssetDetailSheet.newInstance(sheetFor))
        } else {
            // TODO: Remove the bottom sheet
        }
    }

    override fun onBackPressed(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_dashboard)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analytics.logEvent(AnalyticsEvents.Dashboard)

        setupSwipeRefresh()
        setupRecycler()
    }

    private fun setupRecycler() {
        theLayoutManager = SafeLayoutManager(requireContext())

        recycler_view.apply {
            layoutManager = theLayoutManager
            adapter = theAdapter
        }
        theAdapter.items = displayList
    }

    private fun setupToolbar() {
        activity.supportActionBar?.let {
            activity.setupToolbar(it, R.string.dashboard_title)
        }
    }

    private fun setupSwipeRefresh() {

        swipe.setOnRefreshListener { model.process(RefreshAllIntent) }

        // Configure the refreshing colors
        swipe.setColorSchemeResources(
            R.color.blue_800,
            R.color.blue_600,
            R.color.blue_400,
            R.color.blue_200
        )
    }

    // TODO: This should be handled by the model
    override fun onPause() {
        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, actionEvent)
        rxBus.unregister(MetadataEvent::class.java, actionEvent)

        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()

        // TODO: This should be handled by the model
        compositeDisposable += metadataEvent.subscribe {
            model.process(RefreshAllIntent)
            compositeDisposable += actionEvent.subscribe {
                model.process(RefreshAllIntent)
            }
        }
    }

    private fun onAssetClicked(cryptoCurrency: CryptoCurrency) {
        model.process(ShowAssetDetails(cryptoCurrency))
    }

    // From AssetDetailSheet.Host
    override fun gotoSendFor(cryptoCurrency: CryptoCurrency) {
        navigator().gotoSendFor(cryptoCurrency)
    }

    override fun goToReceiveFor(cryptoCurrency: CryptoCurrency) {
        navigator().gotoReceiveFor(cryptoCurrency)
    }

    override fun onSheetClosed() {
        model.process(HideAssetDetails)
    }

    override fun goToBuy() {
        navigator().launchBuySell()
    }

    override fun gotoSwapFor(cryptoCurrency: CryptoCurrency) {
        navigator().launchSwapOrKyc(cryptoCurrency)
    }

    companion object {
        fun newInstance() = DashboardFragment()

        private const val IDX_CARD_ANNOUNCE = 0
        private const val IDX_CARD_BALANCE = 1
        private const val IDX_CARD_BTC = 2
        private const val IDX_CARD_ETH = 3
        private const val IDX_CARD_BCH = 4
        private const val IDX_CARD_XLM = 5
        private const val IDX_CARD_PAX = 6
    }
}

/**
 * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
 */
private class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}
