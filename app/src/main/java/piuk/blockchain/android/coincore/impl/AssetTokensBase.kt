package piuk.blockchain.android.coincore.impl

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.CryptoSingleAccountList
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.then
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class AssetTokensBase(
    protected val exchangeRates: ExchangeRateDataManager,
    protected val historicRates: ChartsDataManager,
    protected val currencyPrefs: CurrencyPrefs,
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

    private val isCustodialConfigured = AtomicBoolean(false)

    override fun hasActiveWallet(filter: AssetFilter): Boolean =
        when (filter) {
            AssetFilter.Total -> true
            AssetFilter.Wallet -> true
            AssetFilter.Custodial -> isCustodialConfigured.get()
        }

    final override fun exchangeRate(): Single<FiatValue> =
        exchangeRates.fetchLastPrice(asset, currencyPrefs.selectedFiatCurrency)

    final override fun historicRate(epochWhen: Long): Single<FiatValue> =
        exchangeRates.getHistoricPrice(asset, currencyPrefs.selectedFiatCurrency, epochWhen)

    override fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries> =
        historicRates.getHistoricPriceSeries(asset, currencyPrefs.selectedFiatCurrency, period)

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
