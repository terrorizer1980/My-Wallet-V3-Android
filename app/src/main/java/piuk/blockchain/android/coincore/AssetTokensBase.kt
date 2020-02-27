package piuk.blockchain.android.coincore

import androidx.annotation.VisibleForTesting
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.model.ActivitySummaryItem
import piuk.blockchain.android.coincore.model.ActivitySummaryList
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.switchToSingleIfEmpty
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

enum class AssetFilter {
    Total,
    Wallet,
    Custodial
}

enum class AssetAction {
    ViewActivity,
    Send,
    Receive,
    Swap
}

typealias AvailableActions = Set<AssetAction>

// Placeholder for proper account interface. Build on AccountReference and ItemAccount
typealias CryptoAccount = AccountReference

// TODO: For account fetching/default accounts look steal the code from xxxAccountListAdapter in core

interface AssetTokens {
    val asset: CryptoCurrency

    fun defaultAccount(): Single<CryptoAccount>
    //    fun accounts(): Single<AccountsList>
    fun receiveAddress(): Single<String>

    fun totalBalance(filter: AssetFilter = AssetFilter.Total): Single<CryptoValue>
    fun balance(account: CryptoAccount): Single<CryptoValue>

    fun exchangeRate(): Single<FiatValue>
    fun historicRate(epochWhen: Long): Single<FiatValue>
    fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries>

    fun fetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList>
    fun findCachedActivityItem(txHash: String): ActivitySummaryItem?

//    interface PendingTransaction { }
//    fun computeFees(priority: FeePriority, pending: PendingTransaction): Single<PendingTransaction>
//    fun validate(pending: PendingTransaction): Boolean
//    fun execute(pending: PendingTransaction)

    fun actions(filter: AssetFilter): AvailableActions

    /** Has this user got a configured wallet for asset type?
    // The balance methods will return zero for un-configured wallets - ie custodial - but we need a way
    // to differentiate between zero and not configured, so call this in the dashboard asset view when
    // deciding if to show custodial etc **/
    fun hasActiveWallet(filter: AssetFilter): Boolean
}

abstract class AssetTokensBase(rxBus: RxBus) : AssetTokens {

    val logoutSignal = rxBus.register(AuthEvent.UNPAIR::class.java)
        .observeOn(Schedulers.computation())
        .subscribeBy(onNext = ::onLogoutSignal)

    val metadataSignal = rxBus.register(MetadataEvent.SETUP_COMPLETE::class.java)
        .observeOn(Schedulers.computation())
        .subscribeBy(onNext = ::onMetadataSignal)

    private val txActivityCache: MutableList<ActivitySummaryItem> = mutableListOf()

    protected open fun onLogoutSignal(event: AuthEvent) {}
    protected open fun onMetadataSignal(event: MetadataEvent) {}

    final override fun totalBalance(filter: AssetFilter): Single<CryptoValue> =
        when (filter) {
            AssetFilter.Wallet -> noncustodialBalance()
            AssetFilter.Custodial -> custodialBalance()
            AssetFilter.Total -> Singles.zip(
                noncustodialBalance(),
                custodialBalance()
            ) { noncustodial, custodial -> noncustodial + custodial }
        }

    protected abstract fun custodialBalanceMaybe(): Maybe<CryptoValue>
    protected abstract fun noncustodialBalance(): Single<CryptoValue>

    private val isNonCustodialConfigured = AtomicBoolean(false)

    private fun custodialBalance(): Single<CryptoValue> =
        custodialBalanceMaybe()
            .doOnComplete { isNonCustodialConfigured.set(false) }
            .doOnSuccess { isNonCustodialConfigured.set(true) }
            .switchToSingleIfEmpty { Single.just(CryptoValue.zero(asset)) }
            // Report and then eat errors getting custodial balances - TODO add UI element to inform the user?
            .onErrorReturn {
                Timber.d("Unable to get non-custodial balance: $it")
                CryptoValue.zero(asset)
            }

    protected open val noncustodialActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send,
        AssetAction.Receive,
        AssetAction.Swap
    )

    protected open val custodialActions = setOf(
        AssetAction.Send
    )

    override fun actions(filter: AssetFilter): AvailableActions =
        when (filter) {
            AssetFilter.Total -> custodialActions.intersect(noncustodialActions)
            AssetFilter.Custodial -> custodialActions
            AssetFilter.Wallet -> noncustodialActions
        }

    override fun hasActiveWallet(filter: AssetFilter): Boolean =
        when (filter) {
            AssetFilter.Total -> true
            AssetFilter.Wallet -> true
            AssetFilter.Custodial -> isNonCustodialConfigured.get()
        }

    final override fun fetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList> =
        doFetchActivity(itemAccount)
            .doOnSubscribe { txActivityCache.clear() }
            .onErrorReturn { emptyList() }
            .subscribeOn(Schedulers.io())
            .doOnSuccess { txActivityCache.addAll(it.sorted()) }

    final override fun findCachedActivityItem(txHash: String): ActivitySummaryItem? =
        txActivityCache.firstOrNull { it.hash == txHash }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    abstract fun doFetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList>

    // These are constant ATM, but may need to change this so hardcode here
    protected val transactionFetchCount = 50
    protected val transactionFetchOffset = 0
}

fun ExchangeRateDataManager.fetchLastPrice(
    cryptoCurrency: CryptoCurrency,
    currencyName: String
): Single<FiatValue> =
    updateTickers()
        .andThen(Single.defer { Single.just(getLastPrice(cryptoCurrency, currencyName)) })
        .map { FiatValue.fromMajor(currencyName, it.toBigDecimal(), false) }

fun <T, R> Observable<List<T>>.mapList(func: (T) -> R): Observable<List<R>> {
    return flatMapIterable { list ->
        list.map { func(it) }
    }.toList().toObservable()
}