package piuk.blockchain.android.ui.transactions

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.TransactionsAnalyticsEvents
import com.blockchain.ui.urllinks.URL_BLOCKCHAIN_PAX_FAQ
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_balance.*
import kotlinx.android.synthetic.main.fragment_balance.app_bar
import kotlinx.android.synthetic.main.fragment_balance.currency_header
import kotlinx.android.synthetic.main.include_no_transaction_message.*
import kotlinx.android.synthetic.main.layout_pax_no_transactions.*
import kotlinx.android.synthetic.main.view_expanding_currency_header.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.transactions.adapter.AccountsAdapter
import piuk.blockchain.android.ui.transactions.adapter.TxFeedAdapter
import piuk.blockchain.android.ui.transactions.adapter.TxFeedClickListener
import piuk.blockchain.android.ui.customviews.BottomSpacerDecoration
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.home.HomeScreenMvpFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.shortcuts.LauncherShortcutHelper
import piuk.blockchain.android.util.calloutToExternalSupportLinkDlg
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.UiState
import piuk.blockchain.androidcoreui.utils.AndroidUtils
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.onItemSelectedListener

@Suppress("MemberVisibilityCanPrivate")
class TransactionsFragment : HomeScreenMvpFragment<TransactionsView, TransactionsPresenter>(),
    TransactionsView,
    TxFeedClickListener {

    override val presenter: TransactionsPresenter by inject()
    override val view: TransactionsView = this

    private var accountsAdapter: AccountsAdapter? = null
    private var txFeedAdapter: TxFeedAdapter? = null

    private val rxBus: RxBus by inject()

    private var spacerDecoration: BottomSpacerDecoration? = null
    private val compositeDisposable = CompositeDisposable()

    private val itemSelectedListener =
        onItemSelectedListener {
            currency_header?.close()
            presenter.onAccountSelected(it)
            recyclerview.smoothScrollToPosition(0)
        }

    private val event by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_balance)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setOnTouchOutsideViewListener(app_bar,
            object : OnTouchOutsideViewListener {
                override fun onTouchOutside(view: View, event: MotionEvent) {
                    currency_header.close()
                }
            })

        swipe_container.setProgressViewEndTarget(
            false,
            ViewUtils.convertDpToPixel(72F + 20F, context).toInt()
        )
        swipe_container.setOnRefreshListener { presenter.requestRefresh() }
        swipe_container.setColorSchemeResources(
            R.color.product_green_medium,
            R.color.primary_blue_medium,
            R.color.product_red_medium
        )

        textview_balance.setOnClickListener {
            presenter.onBalanceClick()
            currency_header?.close()
        }
        currency_header.setSelectionListener { presenter.onCurrencySelected(it) }

        link_what_is_pax.setOnClickListener {
            calloutToExternalSupportLinkDlg(activity, URL_BLOCKCHAIN_PAX_FAQ)
        }

        onViewReady()
        presenter.requestRefresh()
    }

    override fun disableCurrencyHeader() {
        textview_selected_currency?.apply {
            isClickable = false
        }
    }

    override fun onResume() {
        super.onResume()

        navigator().showNavigation()
        compositeDisposable += event.subscribe { event ->
            if (activity != null) {
                recyclerview?.scrollToPosition(0)
                presenter.requestRefresh()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, event)
        // Fixes issue with Swipe Layout messing with Fragment transitions
        swipe_container?.let {
            it.isRefreshing = false
            it.destroyDrawingCache()
            it.clearAnimation()
        }

        currency_header?.close()
    }

    override fun setupAccountsAdapter(accountsList: List<ItemAccount>) {
        if (accountsAdapter == null) {
            accountsAdapter = AccountsAdapter(
                context,
                R.layout.spinner_balance_header,
                accountsList
            ).apply { setDropDownViewResource(R.layout.item_balance_account_dropdown) }
        }

        accounts_spinner.apply {
            adapter = accountsAdapter
            onItemSelectedListener = itemSelectedListener
            setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_UP && (activity as MainActivity).drawerOpen
            }
        }
    }

    override fun setupTxFeedAdapter(isCrypto: Boolean) {
        if (txFeedAdapter == null) {
            txFeedAdapter = TxFeedAdapter(
                activity,
                isCrypto,
                this
            )

            recyclerview.layoutManager = LayoutManager(context!!)
            recyclerview.adapter = txFeedAdapter
            // Disable blinking animations in RecyclerView
            val animator = recyclerview.itemAnimator
            if (animator is SimpleItemAnimator) animator.supportsChangeAnimations = false
        }
    }

    override fun selectDefaultAccount() {
        if (accountsAdapter?.isNotEmpty == true) {
            accounts_spinner.apply {
                onItemSelectedListener = null
                setSelection(0, false)
                onItemSelectedListener = itemSelectedListener
            }
        }
    }

    override fun updateAccountsDataSet(accountsList: List<ItemAccount>) {
        accountsAdapter?.updateAccountList(accountsList)
    }

    override fun updateTransactionDataSet(isCrypto: Boolean, displayObjects: List<Any>) {
        setupTxFeedAdapter(isCrypto)
        txFeedAdapter!!.items = displayObjects
        addBottomNavigationBarSpace()
    }

    /**
     * Adds space to bottom of tx feed recyclerview to make room for bottom navigation bar
     */
    private fun addBottomNavigationBarSpace() {
        if (spacerDecoration == null) {
            spacerDecoration = BottomSpacerDecoration(
                ViewUtils.convertDpToPixel(56f, context).toInt()
            )
        }
        recyclerview?.apply {
            spacerDecoration?.let {
                removeItemDecoration(it)
                addItemDecoration(it)
            }
        }
    }

    /**
     * Updates launcher shortcuts with latest receive address
     */
    @TargetApi(Build.VERSION_CODES.M)
    override fun generateLauncherShortcuts() {
        if (AndroidUtils.is25orHigher() && presenter.areLauncherShortcutsEnabled()) {
            val launcherShortcutHelper = LauncherShortcutHelper(
                activity,
                presenter.payloadDataManager,
                activity.getSystemService(ShortcutManager::class.java)
            )

            launcherShortcutHelper.generateReceiveShortcuts()
        }
    }

    override fun updateTransactionValueType(showCrypto: Boolean) {
        txFeedAdapter?.onViewFormatUpdated(showCrypto)
    }

    override fun onBackPressed(): Boolean =
        if (currency_header.isOpen()) {
            currency_header.close()
            true
        } else {
            false
        }

    private fun setShowRefreshing(showRefreshing: Boolean) {
        swipe_container.isRefreshing = showRefreshing
    }

    override fun setDropdownVisibility(visible: Boolean) {
        layout_spinner.goneIf { !visible }
    }

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.FAILURE,
            UiState.EMPTY -> onEmptyState()
            UiState.CONTENT -> onContentLoaded()
            UiState.LOADING -> {
                textview_balance.text = ""
                setShowRefreshing(true)
            }
        }
    }

    private fun onEmptyState() {
        setShowRefreshing(false)
        no_transaction_include.visible()

        when (presenter.getCurrentCurrency()) {
            CryptoCurrency.BTC -> {
                button_get_bitcoin.setText(R.string.onboarding_get_bitcoin)
                button_get_bitcoin.setOnClickListener {
                    presenter.onGetBitcoinClicked()
                }
                description.setText(R.string.transaction_occur_when_bitcoin)
                pax_no_transactions.gone()
                non_pax_no_transactions_container.visible()
            }
            CryptoCurrency.ETHER -> {
                button_get_bitcoin.setText(R.string.onboarding_get_eth)
                button_get_bitcoin.setOnClickListener { navigator().gotoReceiveFor(CryptoCurrency.ETHER) }
                description.setText(R.string.transaction_occur_when_eth)
                pax_no_transactions.gone()
                non_pax_no_transactions_container.visible()
            }
            CryptoCurrency.BCH -> {
                button_get_bitcoin.setText(R.string.onboarding_get_bitcoin_cash)
                button_get_bitcoin.setOnClickListener { navigator().gotoReceiveFor(CryptoCurrency.BCH) }
                description.setText(R.string.transaction_occur_when_bitcoin_cash)
                pax_no_transactions.gone()
                non_pax_no_transactions_container.visible()
            }
            CryptoCurrency.XLM -> {
                button_get_bitcoin.setText(R.string.onboarding_get_lumens)
                button_get_bitcoin.setOnClickListener { navigator().gotoReceiveFor(CryptoCurrency.XLM) }
                description.setText(R.string.transaction_occur_when_lumens)
                pax_no_transactions.gone()
                non_pax_no_transactions_container.visible()
            }
            CryptoCurrency.PAX -> {
                pax_no_transactions.visible()
                non_pax_no_transactions_container.gone()
                swap_for_pax_now.setOnClickListener {
                    (activity as? Context)?.let {
                        presenter.exchangePaxRequested.onNext(Unit)
                    }
                }
            }
            else -> throw IllegalArgumentException(
                "Cryptocurrency ${presenter.getCurrentCurrency().unit} not supported"
            )
        }
    }

    // Called back by presenter.onGetBitcoinClicked() if buy/sell is not available
    override fun startReceiveFragmentBtc() = navigator().gotoReceiveFor(CryptoCurrency.BTC)

    override fun updateBalanceHeader(balance: String) {
        textview_balance.text = balance
    }

    private fun onContentLoaded() {
        setShowRefreshing(false)
        no_transaction_include.gone()
    }

    override fun startBuyActivity() = navigator().launchBuySell()

    override fun onTransactionClicked(correctedPosition: Int, absolutePosition: Int) {
        val bundle = Bundle()
        bundle.putInt(KEY_TRANSACTION_LIST_POSITION, correctedPosition)
        TransactionDetailActivity.start(activity as Context, bundle)
        currency_header?.getCurrentlySelectedCurrency()?.symbol?.let {
            analytics.logEvent(TransactionsAnalyticsEvents.ItemClick(it))
        }
    }

    /*
    Toggle between fiat - crypto currency
     */
    override fun onValueClicked(isBtc: Boolean) {
        presenter.setViewType(isBtc)
    }

    override fun updateSelectedCurrency(cryptoCurrency: CryptoCurrency) {
        currency_header?.setCurrentlySelectedCurrency(cryptoCurrency)
    }

    override fun startSwapOrKyc(targetCurrency: CryptoCurrency) = navigator().launchSwapOrKyc(targetCurrency)

    override fun getCurrentAccountPosition() = accounts_spinner.selectedItemPosition

    companion object {

        const val KEY_TRANSACTION_LIST_POSITION = "transaction_list_position"
        const val KEY_TRANSACTION_HASH = "transaction_hash"

        private const val ARGUMENT_BROADCASTING_PAYMENT = "broadcasting_payment"

        @JvmStatic
        fun newInstance(broadcastingPayment: Boolean): TransactionsFragment {
            return TransactionsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARGUMENT_BROADCASTING_PAYMENT, broadcastingPayment)
                }
            }
        }
    }

    private inner class LayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun supportsPredictiveItemAnimations() = false
    }
}