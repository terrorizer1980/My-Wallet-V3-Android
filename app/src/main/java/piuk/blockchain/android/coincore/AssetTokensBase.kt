package piuk.blockchain.android.coincore

import info.blockchain.api.data.Transaction
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Maybes
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

enum class AssetFilter {
    Total,
    Wallet,
//    ColdStorage,
//    Lockbox,
    Custodial
}

enum class AssetAction {
    ViewActivity,
    Send,
    Receive,
    Swap
}

typealias AvailableActions = Set<AssetAction>

typealias TransactionList = List<Transaction>

// TODO: For account fetching/default accounts look steal the code from xxxAccountListAdapter in core

interface AssetTokens {
    val asset: CryptoCurrency

    fun defaultAccount(): Single<AccountReference>
//    fun accounts(): Single<AccountsList>

    fun totalBalance(filter: AssetFilter = AssetFilter.Total): Maybe<CryptoValue>
    fun balance(account: AccountReference): Maybe<CryptoValue>

    fun exchangeRate(): Single<FiatValue>
    fun historicRate(epochWhen: Long): Single<FiatValue>
    fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries>

//    fun transactions(): Single<TransactionList>
//    fun transactions(account: AccountReference): Single<TransactionList>

//    interface PendingTransaction { }
//    fun computeFees(priority: FeePriority, pending: PendingTransaction): Single<PendingTransaction>
//    fun validate(pending: PendingTransaction): Boolean
//    fun execute(pending: PendingTransaction)

    fun actions(filter: AssetFilter): AvailableActions
}

abstract class AssetTokensBase(rxBus: RxBus) : AssetTokens {

    val logoutSignal = rxBus.register(AuthEvent.UNPAIR::class.java)
        .observeOn(Schedulers.computation())
        .subscribeBy(onNext = ::onLogoutSignal)

    protected open fun onLogoutSignal(event: AuthEvent) { }

    final override fun totalBalance(filter: AssetFilter): Maybe<CryptoValue> =
        when (filter) {
            AssetFilter.Wallet -> noncustodialBalance()
            AssetFilter.Custodial -> custodialBalance()
            AssetFilter.Total -> Maybes.zip(
                noncustodialBalance(),
                custodialBalance()
            ) { noncustodial, custodial -> noncustodial + custodial }
        }

    protected abstract fun custodialBalance(): Maybe<CryptoValue>
    protected abstract fun noncustodialBalance(): Maybe<CryptoValue>

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
}

fun ExchangeRateDataManager.fetchLastPrice(
    cryptoCurrency: CryptoCurrency,
    currencyName: String
): Single<FiatValue> =
    updateTickers()
        .andThen(Single.defer { Single.just(getLastPrice(cryptoCurrency, currencyName)) })
        .map { FiatValue.fromMajor(currencyName, it.toBigDecimal(), false) }
