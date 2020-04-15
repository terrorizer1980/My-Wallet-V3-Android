package piuk.blockchain.android.ui.activity

import android.content.Context
import android.os.Bundle
import androidx.annotation.UiThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.annotations.CommonCode
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_activities.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.ui.accounts.AccountSelectSheet
import piuk.blockchain.android.ui.activity.adapter.ActivitiesDelegateAdapter
import piuk.blockchain.android.ui.activity.detail.TransactionDetailActivity
import piuk.blockchain.android.ui.home.HomeScreenMviFragment
import piuk.blockchain.android.util.setCoinIcon
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber

class ActivitiesFragment
    : HomeScreenMviFragment<ActivitiesModel, ActivitiesIntent, ActivitiesState>(),
    AccountSelectSheet.Host {
    override val model: ActivitiesModel by inject()

    private val theAdapter: ActivitiesDelegateAdapter by lazy {
        ActivitiesDelegateAdapter(
            disposables = disposables,
            prefs = get(),
            onItemClicked = { cc, tx, isCustodial -> onActivityClicked(cc, tx, isCustodial) },
            analytics = get()
        )
    }

    private lateinit var theLayoutManager: RecyclerView.LayoutManager

    private val displayList = mutableListOf<ActivitySummaryItem>()

    private val disposables = CompositeDisposable()
    private val rxBus: RxBus by inject()
    private val currencyPrefs: CurrencyPrefs by inject()
    private val exchangeRates: ExchangeRateDataManager by inject()

    private val actionEvent by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    private var state: ActivitiesState? = null

    @UiThread
    override fun render(newState: ActivitiesState) {

        switchView(newState)

        swipe.isRefreshing = newState.isLoading

        renderAccountDetails(newState)
        renderTransactionList(newState)

        if (this.state?.bottomSheet != newState.bottomSheet) {
            when (newState.bottomSheet) {
                ActivitiesSheet.ACCOUNT_SELECTOR -> showBottomSheet(AccountSelectSheet.newInstance())
            }
        }

        this.state = newState
    }

    private fun switchView(newState: ActivitiesState) {
        if (newState.activityList.isEmpty()) {
            content_layout.gone()
            empty_view.visible()
        } else {
            content_layout.visible()
            empty_view.gone()
        }
    }

    private fun renderAccountDetails(newState: ActivitiesState) {
        if (newState.account == state?.account) {
            return
        }

        if (newState.account == null) {
            // Should not happen! TODO: Crash
            return
        }

        disposables.clear()

        val account = newState.account

        account_icon.setAccountIcon(account)

        account_spend_locked.goneIf { account.isCustodial().not() }
        account_name.text = account.label
        fiat_balance.text = ""

        disposables += account.fiatBalance(currencyPrefs.selectedFiatCurrency, exchangeRates)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    fiat_balance.text = it.toStringWithSymbol()
                },
                onError = {
                    Timber.e("Unable to get balance for ${account.label}")
                }
            )
    }

    private fun ImageView.setAccountIcon(account: CryptoAccount) {
        when (account.cryptoCurrencies.size) {
            0 -> throw IllegalStateException("Account is invalid; no crypto")
            1 -> setCoinIcon(account.cryptoCurrencies.first())
            else -> setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_all_wallets_white))
        }
    }

    private fun renderTransactionList(newState: ActivitiesState) {
        if (state?.activityList == newState.activityList) {
            return
        }

        with(newState.activityList) {
            displayList.clear()
            if (isEmpty()) {
                Timber.d("Render new tx list - empty")
                // TODO: Show no-transactions, or loading view. There should _always_ be transactions, since the account
                // selector filters out accounts with no transactions
            } else {
                displayList.addAll(this)
            }
            theAdapter.notifyDataSetChanged()
        }
    }

    override fun onBackPressed(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_activities)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwipeRefresh()
        setupRecycler()
        setupAccountSelect()
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
            activity.setupToolbar(it, R.string.activities_title)
        }
    }

    private fun setupAccountSelect() {
        account_select_btn.setOnClickListener {
            model.process(ShowAccountSelectionIntent)
        }
    }

    private fun setupSwipeRefresh() {
        swipe.setOnRefreshListener {
            state?.account?.let {
                model.process(AccountSelectedIntent(it))
            }
        }

        // Configure the refreshing colors
        swipe.setColorSchemeResources(
            R.color.blue_800,
            R.color.blue_600,
            R.color.blue_400,
            R.color.blue_200
        )
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    override fun onPause() {
        disposables.clear()
        rxBus.unregister(ActionEvent::class.java, actionEvent)
        super.onPause()
    }

    private fun onActivityClicked(cryptoCurrency: CryptoCurrency, txHash: String, isCustodial: Boolean) {
        // TODO: Use an intent, when we upgrade the de3tail sheet as per then designs.
        // For expediency, currently using to old details sheet
        // model.process(ShowActivityDetailsIntent(cryptoCurrency, txHash))
        // Also, custodial detains are not supported, until the new designs are built.
        // Show a toast in this case, for now - this may change come design review...
        if (isCustodial) {
            Toast.makeText(
                requireContext(),
                "Custodial activity details are not supported in this release",
                Toast.LENGTH_LONG
            ).show()
        } else {
            TransactionDetailActivity.start(requireContext(), cryptoCurrency, txHash)
        }
    }

    private fun onShowAllActivity() {
        model.process(SelectDefaultAccountIntent)
    }

    override fun onAccountSelected(account: CryptoAccount) {
        model.process(AccountSelectedIntent(account))
    }

    // SlidingModalBottomDialog.Host
    override fun onSheetClosed() {
        model.process(ClearBottomSheetIntent)
    }

    companion object {
        fun newInstance(account: CryptoAccount?): ActivitiesFragment {
            return ActivitiesFragment().apply {
                account?.let { onAccountSelected(it) } ?: onShowAllActivity()
            }
        }
    }
}

/**
 * supportsPredictiveItemAnimations = false to avoid crashes when computing changes.
 */
@CommonCode(commonWith = "DashboardFragment - move to ui utils package")
private class SafeLayoutManager(context: Context) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations() = false
}
