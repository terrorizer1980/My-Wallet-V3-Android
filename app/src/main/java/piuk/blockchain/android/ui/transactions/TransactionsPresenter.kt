package piuk.blockchain.android.ui.transactions

import androidx.annotation.VisibleForTesting
import com.blockchain.annotations.CommonCode
import com.blockchain.notifications.models.NotificationPayload
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.Environment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.coincore.activity.TransactionNoteUpdater
import piuk.blockchain.android.coincore.model.ActivitySummaryList
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.androidcoreui.ui.base.UiState
import timber.log.Timber

interface TransactionsView : MvpView {
    fun setupAccountsAdapter(accountsList: List<ItemAccount>)
    fun setupTxFeedAdapter(isCrypto: Boolean)
    fun updateTransactionDataSet(isCrypto: Boolean, txItems: ActivitySummaryList)
    fun updateAccountsDataSet(accountsList: List<ItemAccount>)
    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrency)
    fun updateBalanceHeader(balance: CryptoValue)
    fun selectDefaultAccount()
    fun setUiState(@UiState.UiStateDef uiState: Int, crypto: CryptoCurrency)
    fun updateTransactionValueType(showCrypto: Boolean)
    fun startReceiveFragmentBtc()
    fun startBuyActivity()
    fun getCurrentAccountPosition(): Int?
    fun startSwapOrKyc(targetCurrency: CryptoCurrency)
    fun setDropdownVisibility(visible: Boolean)
    fun disableCurrencyHeader()
}

class TransactionsPresenter(
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val assetSelect: AssetTokenLookup,
    private val ethDataManager: EthDataManager,
    private val paxAccount: Erc20Account,
    private val payloadDataManager: PayloadDataManager,
    private val buyDataManager: BuyDataManager,
    private val rxBus: RxBus,
    private val currencyState: CurrencyState,
    private val bchDataManager: BchDataManager,
    private val walletAccountHelper: WalletAccountHelper,
    private val environmentSettings: EnvironmentConfig,
    private val transactionNotes: TransactionNoteUpdater // Move to asset token/coincore
) : MvpPresenter<TransactionsView>() {

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = false

    override fun onViewAttached() { }
    override fun onViewDetached() { }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var notificationObservable: Observable<NotificationPayload>? = null
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var authEventObservable: Observable<AuthEvent>? = null
    val exchangePaxRequested = PublishSubject.create<Unit>()

    private val assetTokens: AssetTokens
        get() = assetSelect[currencyState.cryptoCurrency]

    private var crypto: CryptoCurrency
        get() = currencyState.cryptoCurrency
        set(v) { currencyState.cryptoCurrency = v }

    override fun onViewResumed() {
        super.onViewResumed()

        subscribeToEvents()

        setViewType(currencyState.isDisplayingCryptoCurrency)

        onAccountsAdapterSetup()
        onTxFeedAdapterSetup()

        if (environmentSettings.environment == Environment.TESTNET) {
            crypto = CryptoCurrency.BTC
            view?.disableCurrencyHeader()
        }

        compositeDisposable += exchangePaxRequested.subscribe {
            view?.startSwapOrKyc(crypto)
        }
    }

    override fun onViewPaused() {
        notificationObservable?.let { rxBus.unregister(NotificationPayload::class.java, it) }
        authEventObservable?.let { rxBus.unregister(AuthEvent::class.java, it) }
        super.onViewPaused()
    }

    private fun subscribeToEvents() {
        authEventObservable = rxBus.register(AuthEvent::class.java).apply {
            subscribe {
                view?.updateTransactionDataSet(currencyState.isDisplayingCryptoCurrency, listOf())
            }
        }

        notificationObservable = rxBus.register(NotificationPayload::class.java).apply {
            subscribe { /* no-op */ }
        }
    }

    internal fun requestRefresh() {
        compositeDisposable +=
            getCurrentAccount()
                .flatMap { refreshAll(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view?.setUiState(UiState.LOADING, crypto) }
                .doOnError { view?.setUiState(UiState.FAILURE, crypto) }
                .subscribeBy(
                    onSuccess = {
                        view?.setDropdownVisibility(it)
                        setViewType(currencyState.isDisplayingCryptoCurrency)
                    },
                    onError = {
                        Timber.e(it)
                    }
                )
    }

    private fun refreshAll(account: ItemAccount): Single<Boolean> =
        getUpdateTickerCompletable()
            .andThen(updateBalanceAndTransactionsCompletable(account))
            .andThen(getAccounts().map { it.size > 1 })

    private fun getUpdateTickerCompletable(): Completable =
        exchangeRateDataManager.updateTickers()

    private val updateBalanceAndTransactionsCompletable: (ItemAccount) -> Completable = {
        Completable.concat(
            listOf(
                updateBalancesCompletable(),
                updateTransactionsListCompletable(it)
            )
        )
    }

    /**
     * API call - Fetches latest balance for selected currency and updates UI balance
     */
    private fun updateBalancesCompletable() =
        when (crypto) {
            CryptoCurrency.BTC -> payloadDataManager.updateAllBalances()
            CryptoCurrency.ETHER -> ethDataManager.fetchEthAddressCompletable()
            CryptoCurrency.BCH -> bchDataManager.updateAllBalances()
            CryptoCurrency.XLM -> Completable.complete()
            CryptoCurrency.PAX -> paxAccount.fetchAddressCompletable()
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }.doOnError {
            Timber.e("TRANSACTIONS: Failed to get balance: $it")
        }.onErrorComplete()

    /**
     * API call - Fetches latest transactions for selected currency and account, and updates UI tx list
     */
    private fun updateTransactionsListCompletable(account: ItemAccount): Completable {
        return Completable.defer {
            assetTokens.fetchActivity(account)
                .flatMap { txs -> transactionNotes.updateWithNotes(txs)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess {
                        updateUiTxList(it)
                    }
                    .doOnError {
                        Timber.e(it)
                        view?.setUiState(UiState.FAILURE, crypto)
                    }
                }
                .ignoreElement()
        }
    }

    private fun updateUiTxList(txs: ActivitySummaryList) {
        when {
            txs.isEmpty() -> view?.setUiState(UiState.EMPTY, crypto)
            else -> view?.setUiState(UiState.CONTENT, crypto)
        }

        view?.updateTransactionDataSet(currencyState.isDisplayingCryptoCurrency, txs)
    }

    /* Currency selected from dropdown */
    internal fun onCurrencySelected(cryptoCurrency: CryptoCurrency) {
        Timber.d(">PRESENTER: On Currency Selected")
        // Set new currency state
        crypto = cryptoCurrency

        // Select default account for this currency
        compositeDisposable +=
            getAccounts()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess {
                    view?.setDropdownVisibility(it.size > 1)
                    refreshViewHeaders(it.first())
                }
                .flatMapCompletable {
                    updateBalanceAndTransactionsCompletable(it.first())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    view?.setUiState(UiState.LOADING, crypto)
                    refreshAccountDataSet()
                }
                .subscribeBy(
                    onError = {
                        Timber.e(it)
                        view?.setUiState(UiState.FAILURE, crypto)
                    },
                    onComplete = {
                        view?.selectDefaultAccount()
                    }
                )
    }

    @CommonCode("This can be moved elsewhere, so other 'get BTC' actions can make the same call")
    internal fun onGetBitcoinClicked() {
        compositeDisposable +=
            buyDataManager.canBuy
                .subscribeBy(
                    onSuccess = {
                        if (it) {
                            view?.startBuyActivity()
                        } else {
                            view?.startReceiveFragmentBtc()
                        }
                    },
                    onError = {
                        Timber.e(it)
                    }
                )
    }

    /*
    Fetch all active accounts for initial selected currency and set up account adapter
     */
    private fun onAccountsAdapterSetup() {
        compositeDisposable +=
            getAccounts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    view?.setupAccountsAdapter(it.toMutableList())
                }
    }

    internal fun onAccountSelected(position: Int) {
        compositeDisposable +=
            getAccountAt(position)
                .doOnSubscribe { view?.setUiState(UiState.LOADING, crypto) }
                .flatMapCompletable {
                    updateBalanceAndTransactionsCompletable(it) // Here is the second load!
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete {
                            refreshViewHeaders(it)
                            refreshAccountDataSet()
                        }
                }
                .doOnError { Timber.e(it) }
                .subscribeBy(onError = { view?.setUiState(UiState.FAILURE, crypto) })
    }

    /*
    Set fiat or crypto currency state
     */
    internal fun setViewType(showCrypto: Boolean) {
        // Set new currency state
        currencyState.isDisplayingCryptoCurrency = showCrypto

        // Update balance header
        compositeDisposable +=
            getCurrentAccount()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { refreshViewHeaders(it) }

        // Update tx list balances
        view?.updateTransactionValueType(showCrypto)

        // Update accounts data set
        refreshAccountDataSet()
    }

    /*
    Toggle between fiat - crypto currency
     */
    internal fun onBalanceClick() = setViewType(!currencyState.isDisplayingCryptoCurrency)

    private fun refreshViewHeaders(account: ItemAccount) {
        view?.updateSelectedCurrency(crypto)
        view?.updateBalanceHeader(account.balance ?: CryptoValue(crypto, 0.toBigInteger()))
    }

    private fun refreshAccountDataSet() {
        compositeDisposable +=
            getAccounts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { view?.updateAccountsDataSet(it) }
    }

    private fun onTxFeedAdapterSetup() {
        view?.setupTxFeedAdapter(currencyState.isDisplayingCryptoCurrency)
    }

    /**
     * Get accounts based on selected currency. Mutable list necessary for adapter. This needs fixing.
     */
    private fun getAccounts() =
        walletAccountHelper.getAccountItemsForOverview(crypto)

    private fun getCurrentAccount(): Single<ItemAccount> =
        getAccountAt(view?.getCurrentAccountPosition() ?: 0)

    private fun getAccountAt(position: Int): Single<ItemAccount> = getAccounts()
        .map { it[if (position < 0 || position >= it.size) 0 else position] }
}