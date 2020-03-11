package piuk.blockchain.android.coincore.impl

import androidx.annotation.VisibleForTesting
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.switchToSingleIfEmpty
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class AssetTokensBase(rxBus: RxBus) :
    AssetTokens {

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

    internal abstract fun custodialBalanceMaybe(): Maybe<CryptoValue>
    internal abstract fun noncustodialBalance(): Single<CryptoValue>

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
