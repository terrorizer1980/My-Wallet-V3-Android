package piuk.blockchain.android.coincore.impl

import com.blockchain.logging.CrashLogger
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class AssetTokensBase(
    private val labels: DefaultLabels,
    protected val crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokens {

    val logoutSignal = rxBus.register(AuthEvent.UNPAIR::class.java)
        .observeOn(Schedulers.computation())
        .subscribeBy(onNext = ::onLogoutSignal)

    private val accounts = mutableListOf<CryptoSingleAccount>()

    // Init token, set up accounts and fetch a few activities
    fun init(): Completable =
        initToken()
            .doOnError { throwable ->
                crashLogger.logException(throwable, "Coincore: Failed to load $asset wallet")
            }
            .then { loadAccounts() }
            .then { initActivities() }
            .doOnComplete { Timber.d("Coincore: Init $asset Complete") }
            .doOnError { Timber.d("Coincore: Init $asset Failed") }

    private fun loadAccounts(): Completable =
        Completable.fromCallable { accounts.clear() }
            .then {
                loadNonCustodialAccounts(labels)
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .then {
                loadCustodialAccounts(labels)
                    .doOnSuccess { accounts.addAll(it) }
                    .ignoreElement()
            }
            .doOnError { Timber.e("Error loading accounts for ${asset.networkTicker}: $it") }

    abstract fun initToken(): Completable

    private fun initActivities(): Completable {
        return Single.zip(
            accounts.map {
                Timber.d(">>>>> Account init: ${it.label}")
                it.activity.onErrorReturn {
                    emptyList()
                }
            }
        ) { t: Array<Any> -> t }
            .subscribeOn(Schedulers.computation())
            .ignoreElement()
    }

    abstract fun loadNonCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList>
    abstract fun loadCustodialAccounts(labels: DefaultLabels): Single<CryptoSingleAccountList>

    protected open fun onLogoutSignal(event: AuthEvent) {}

    final override fun accounts(filter: AssetFilter): Single<CryptoAccountGroup> =
        Single.fromCallable {
            filterTokenAccounts(asset, labels, accounts, filter)
        }

    final override fun defaultAccount(): Single<CryptoSingleAccount> =
        Single.fromCallable {
            accounts.first { it.isDefault }
        }

//    final override fun totalBalance(filter: AssetFilter): Single<CryptoValue> =
//        accounts(filter)
//            .flatMap { it.balance }

    private val isNonCustodialConfigured = AtomicBoolean(false)

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
