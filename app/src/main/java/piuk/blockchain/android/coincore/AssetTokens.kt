package piuk.blockchain.android.coincore

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.prices.TimeInterval
import io.reactivex.Single
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.androidcore.data.charts.PriceSeries
import piuk.blockchain.androidcore.data.charts.TimeSpan

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

// TODO: For account fetching/default accounts look steal the code from xxxAccountListAdapter in core

interface AssetTokens {
    val asset: CryptoCurrency

    fun defaultAccount(): Single<CryptoSingleAccount>
    fun accounts(filter: AssetFilter = AssetFilter.Total): Single<CryptoAccountGroup>

    fun exchangeRate(): Single<FiatValue>
    fun historicRate(epochWhen: Long): Single<FiatValue>
    fun historicRateSeries(period: TimeSpan, interval: TimeInterval): Single<PriceSeries>

    @Deprecated(replaceWith = ReplaceWith("defaultAccount"), message = "CoinCore update")
    fun defaultAccountRef(): Single<AccountReference>

    @Deprecated(message = "CoinCore update")
    fun receiveAddress(): Single<String>

    @Deprecated(message = "CoinCore update")
    fun totalBalance(filter: AssetFilter = AssetFilter.Total): Single<CryptoValue>
    @Deprecated(message = "CoinCore update")
    fun balance(account: AccountReference): Single<CryptoValue>

    @Deprecated(message = "CoinCore update")
    fun fetchActivity(itemAccount: ItemAccount): Single<ActivitySummaryList>
    @Deprecated(message = "CoinCore update")
    fun findCachedActivityItem(txHash: String): ActivitySummaryItem?

    @Deprecated(message = "CoinCore update")
    fun actions(filter: AssetFilter): AvailableActions

    /** Has this user got a configured wallet for asset type?
    // The balance methods will return zero for un-configured wallets - ie custodial - but we need a way
    // to differentiate between zero and not configured, so call this in the dashboard asset view when
    // deciding if to show custodial etc **/
    @Deprecated(message = "CoinCore update")
    fun hasActiveWallet(filter: AssetFilter): Boolean
}
