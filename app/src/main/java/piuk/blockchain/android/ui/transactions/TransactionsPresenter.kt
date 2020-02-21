package piuk.blockchain.android.ui.transactions

import androidx.annotation.VisibleForTesting
import com.blockchain.annotations.CommonCode
import com.blockchain.notifications.models.NotificationPayload
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.formatWithUnit
import info.blockchain.wallet.api.Environment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.old.TransactionListDataManager
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import com.blockchain.swap.shapeshift.data.Trade
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.UiState
import timber.log.Timber

interface TransactionsView : MvpView {
    fun setupAccountsAdapter(accountsList: List<ItemAccount>)
    fun setupTxFeedAdapter(isCrypto: Boolean)
    fun updateTransactionDataSet(isCrypto: Boolean, displayObjects: List<Any>)
    fun updateAccountsDataSet(accountsList: List<ItemAccount>)
    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrency)
    fun updateBalanceHeader(balance: String)
    fun selectDefaultAccount()
    fun setUiState(@UiState.UiStateDef uiState: Int)
    fun updateTransactionValueType(showCrypto: Boolean)
    fun startReceiveFragmentBtc()
    fun startBuyActivity()
    fun getCurrentAccountPosition(): Int?
    fun generateLauncherShortcuts()
    fun startSwapOrKyc(targetCurrency: CryptoCurrency)
    fun setDropdownVisibility(visible: Boolean)
    fun disableCurrencyHeader()
}

class TransactionsPresenter(
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val assetSelect: AssetTokenLookup,
    private val transactionListDataManager: TransactionListDataManager,
    private val ethDataManager: EthDataManager,
    private val paxAccount: Erc20Account,
    internal val payloadDataManager: PayloadDataManager,
    private val buyDataManager: BuyDataManager,
    private val stringUtils: StringUtils,
    private val prefs: PersistentPrefs,
    private val rxBus: RxBus,
    private val currencyState: CurrencyState,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val bchDataManager: BchDataManager,
    private val walletAccountHelper: WalletAccountHelper,
    private val environmentSettings: EnvironmentConfig,
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager,
    private val fiatExchangeRates: FiatExchangeRates
) : MvpPresenter<TransactionsView>() {

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = false

    override fun onViewAttached() { }
    override fun onViewDetached() { }

    @VisibleForTesting
    var notificationObservable: Observable<NotificationPayload>? = null
    @VisibleForTesting
    var authEventObservable: Observable<AuthEvent>? = null
    val exchangePaxRequested = PublishSubject.create<Unit>()

    private var shortcutsGenerated = false

    private val updateBalanceAndTransactionsCompletable: (ItemAccount) -> Completable = {
        Completable.concat(
            listOf(
                updateBalancesCompletable(),
                updateTransactionsListCompletable(it)
            )
        )
    }

    override fun onViewResumed() {
        super.onViewResumed()

        onAccountsAdapterSetup()
        onTxFeedAdapterSetup()
        subscribeToEvents()
        if (environmentSettings.environment == Environment.TESTNET) {
            currencyState.cryptoCurrency = CryptoCurrency.BTC
            view?.disableCurrencyHeader()
        }

        compositeDisposable += exchangePaxRequested.subscribe {
            view?.startSwapOrKyc(currencyState.cryptoCurrency)
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
                // Clear tx feed
                view?.updateTransactionDataSet(
                    currencyState.isDisplayingCryptoCurrency,
                    mutableListOf()
                )
                transactionListDataManager.clearTransactionList()
            }
        }

        notificationObservable = rxBus.register(NotificationPayload::class.java).apply {
            subscribe { /* no-op */ }
        }
    }

    private fun refreshAll(account: ItemAccount): Single<Boolean> =
        getUpdateTickerCompletable()
            .andThen(updateBalanceAndTransactionsCompletable(account))
            .andThen(getAccounts().map { it.size > 1 })

    internal fun requestRefresh() {
        compositeDisposable +=
            getCurrentAccount()
                .flatMap { refreshAll(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view?.setUiState(UiState.LOADING) }
                .doOnError { view?.setUiState(UiState.FAILURE) }
                .doOnSuccess {
                    view?.setDropdownVisibility(it)
                    if (!shortcutsGenerated) {
                        shortcutsGenerated = true
                        refreshLauncherShortcuts()
                    }
                    setViewType(currencyState.isDisplayingCryptoCurrency)
                }
                .doOnError { Timber.e(it) }
                .subscribe()
    }

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .singleOrError()
            .map { it.coinify?.token ?: "" }

    @VisibleForTesting
    internal fun getUpdateTickerCompletable(): Completable = exchangeRateDataManager.updateTickers()

    /**
     * API call - Fetches latest balance for selected currency and updates UI balance
     */
    @VisibleForTesting
    internal fun updateBalancesCompletable() =
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> payloadDataManager.updateAllBalances()
            CryptoCurrency.ETHER -> ethDataManager.fetchEthAddressCompletable()
            CryptoCurrency.BCH -> bchDataManager.updateAllBalances()
            CryptoCurrency.XLM -> Completable.complete()
            CryptoCurrency.PAX -> paxAccount.fetchAddressCompletable()
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    /**
     * API call - Fetches latest transactions for selected currency and account, and updates UI tx list
     */
    @VisibleForTesting
    internal fun updateTransactionsListCompletable(account: ItemAccount): Completable {
        return Completable.defer {
            assetSelect[currencyState.cryptoCurrency].fetchActivity(account)
                .flatMap { txs ->
                    Singles.zip(
                        getShapeShiftTxNotes(),
                        getCoinifyTxNotes()
                    ) { ssTx, cyTx -> mergeTxNotes(ssTx, cyTx) }
                        .map { it }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess { txNotesMap -> processTxNotes(txs, txNotesMap) }
                        .doOnError {
                            Timber.e(it)
                            view?.setUiState(UiState.FAILURE)
                        }
                }
                .ignoreElement()
        }
    }

    private fun mergeTxNotes(
        shapeshiftNotes: Map<String, String>,
        coinifyNotes: Map<String, String>
    ): Map<String, String> =
        mutableMapOf<String, String>().apply {
            putAll(shapeshiftNotes)
            putAll(coinifyNotes)
        }.toMap()

    private fun processTxNotes(txs: List<ActivitySummaryItem>, txNotesMap: Map<String, String>) {
        for (tx in txs) {
        // Add shapeShift notes
            txNotesMap[tx.hash]?.let { tx.note = it }

            val cryptoValue = CryptoValue(currencyState.cryptoCurrency, tx.total)
            tx.totalDisplayableCrypto = cryptoValue.formatWithUnit()
            tx.totalDisplayableFiat = cryptoValue.getFiatDisplayString()
        }

        when {
            txs.isEmpty() -> view?.setUiState(UiState.EMPTY)
            else -> view?.setUiState(UiState.CONTENT)
        }

        view?.updateTransactionDataSet(
            currencyState.isDisplayingCryptoCurrency,
            txs
        )
    }

    /*
    Currency selected from dropdown
     */
    internal fun onCurrencySelected(cryptoCurrency: CryptoCurrency) {
        // Set new currency state
        currencyState.cryptoCurrency = cryptoCurrency

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
                    view?.setUiState(UiState.LOADING)
                    refreshAccountDataSet()
                }
                .subscribeBy(
                    onError = {
                        Timber.e(it)
                        view?.setUiState(UiState.FAILURE)
                    },
                    onComplete = {
                        view?.selectDefaultAccount()
                    }
                )
    }

    private fun getShapeShiftTxNotes() =
        shapeShiftDataManager.getTradesList()
            .map { processShapeShiftTxNotes(it) }
            .doOnError { Timber.e(it) }
            .onErrorReturn { emptyMap() }
            .singleOrError()

    private fun processShapeShiftTxNotes(list: List<Trade>): Map<String, String> { // TxHash -> description
        val mutableMap: MutableMap<String, String> = mutableMapOf()

        for (trade in list) {
            trade.hashIn?.let {
                mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_to))
            }
            trade.hashOut?.let {
                mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_from))
            }
        }
        return mutableMap.toMap()
    }

//    private fun getShapeShiftTxNotesObservable() =
//        shapeShiftDataManager.getTradesList()
//            .map { processShapeShiftTxNotes(it) }
//            .doOnError { Timber.e(it) }
//            .onErrorReturn { emptyMap() }
//
//    private fun processShapeShiftTxNotes(list: List<Trade>): Map<String, String> { // TxHash -> description
//        val mutableMap: MutableMap<String, String> = mutableMapOf()
//
//        for (trade in list) {
//            trade.hashIn?.let {
//                mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_to))
//            }
//            trade.hashOut?.let {
//                mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_from))
//            }
//        }
//        return mutableMap.toMap()
//    }

    private fun getCoinifyTxNotes() =
        tokenSingle.flatMap {
            coinifyDataManager.getTrades(it).toList()
        }.doOnError { Timber.e(it) }
            .map { processCoinifyTxNotes(it) }
            .toObservable()
            .onErrorReturn { emptyMap() }
            .singleOrError()

    private fun processCoinifyTxNotes(list: List<CoinifyTrade>): Map<String, String> { // TxId -> description
        val mutableMap: MutableMap<String, String> = mutableMapOf()
        for (trade in list) {
            val transfer = if (trade.isSellTransaction()) {
                trade.transferIn.details as BlockchainDetails
            } else {
                trade.transferOut.details as BlockchainDetails
            }
            transfer.eventData?.txId?.let {
                mutableMap.put(
                    it,
                    stringUtils.getFormattedString(
                        R.string.buy_sell_transaction_list_label,
                        trade.id
                    )
                )
            }
        }
        return mutableMap.toMap()
    }

    @CommonCode("This can be moved elsewhere, so other 'get BTC' actions can make the same call")
    internal fun onGetBitcoinClicked() {
        compositeDisposable +=
            buyDataManager.canBuy
                .subscribe(
                    {
                        if (it) {
                            view?.startBuyActivity()
                        } else {
                            view?.startReceiveFragmentBtc()
                        }
                    }, { Timber.e(it) }
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
                .doOnSubscribe { view?.setUiState(UiState.LOADING) }
                .flatMapCompletable {
                    updateBalanceAndTransactionsCompletable(it)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete {
                            refreshViewHeaders(it)
                            refreshAccountDataSet()
                        }
                }
                .doOnError { Timber.e(it) }
                .subscribeBy(onError = { view?.setUiState(UiState.FAILURE) })
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

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun refreshViewHeaders(account: ItemAccount) {
        view?.updateSelectedCurrency(currencyState.cryptoCurrency)
        view?.updateBalanceHeader(account.displayBalance ?: "")
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun refreshAccountDataSet() {
        compositeDisposable +=
            getAccounts()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { view?.updateAccountsDataSet(it) }
    }

    private fun refreshLauncherShortcuts() {
        view?.generateLauncherShortcuts()
    }

    private fun onTxFeedAdapterSetup() {
        view?.setupTxFeedAdapter(currencyState.isDisplayingCryptoCurrency)
    }

    /**
     * Get accounts based on selected currency. Mutable list necessary for adapter. This needs fixing.
     */
    private fun getAccounts() = walletAccountHelper.getAccountItemsForOverview()

    private fun getCurrentAccount(): Single<ItemAccount> = getAccountAt(view?.getCurrentAccountPosition() ?: 0)

    private fun getAccountAt(position: Int): Single<ItemAccount> = getAccounts()
        .map { it[if (position < 0 || position >= it.size) 0 else position] }

    private fun CryptoValue.getFiatDisplayString(): String =
        fiatExchangeRates.getFiat(this).toStringWithSymbol()

    internal fun areLauncherShortcutsEnabled() =
        prefs.getValue(PersistentPrefs.KEY_RECEIVE_SHORTCUTS_ENABLED, true)

    internal fun getCurrentCurrency() = currencyState.cryptoCurrency
}