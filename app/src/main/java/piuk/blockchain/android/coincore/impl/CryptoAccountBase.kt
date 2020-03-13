package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccountGroup
import piuk.blockchain.android.coincore.CryptoAccountsList
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.androidcore.utils.extensions.switchToSingleIfEmpty
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

abstract class CryptoSingleAccountBase
    : CryptoSingleAccount {

    protected val cryptoAsset: CryptoCurrency
        get() = cryptoCurrency!!

    override val hasTransactions: Boolean
        get() = false
}

abstract class CryptoSingleAccountCustodialBase
    : CryptoSingleAccountBase() {

    protected abstract val custodialWalletManager: CustodialWalletManager

    private val isNonCustodialConfigured = AtomicBoolean(false)

    override val receiveAddress: Single<String>
        get() = Single.error(NotImplementedError("Custodial accounts don't support receive"))

    override val balance: Single<CryptoValue>
        get() = custodialWalletManager.getBalanceForAsset(cryptoAsset)
            .doOnComplete { isNonCustodialConfigured.set(false) }
            .doOnSuccess { isNonCustodialConfigured.set(true) }
            .switchToSingleIfEmpty { Single.just(CryptoValue.zero(cryptoAsset)) }
            .onErrorReturn {
                Timber.d("Unable to get non-custodial balance: $it")
                CryptoValue.zero(cryptoAsset)
            }

    override val activity: Single<ActivitySummaryList>
        get() = Single.error(NotImplementedError("activity not implemented"))

    override fun findActivityItem(txHash: String): Maybe<ActivitySummaryItem> =
        Maybe.empty()

    override val hasTransactions: Boolean
        get() = false

    override val isFunded: Boolean
        get() = isNonCustodialConfigured.get()

    final override val actions: AvailableActions
        get() = availableActions

    private val availableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send
    )
}

abstract class CryptoSingleAccountNonCustodialBase
    : CryptoSingleAccountBase() {

    override val receiveAddress: Single<String>
        get() = Single.error(NotImplementedError("ReceiveAddress not implemented"))

    override val balance: Single<CryptoValue>
        get() = Single.error(NotImplementedError("balance not implemented"))

    override val activity: Single<ActivitySummaryList>
        get() = Single.error(NotImplementedError("activity not implemented"))

    override fun findActivityItem(txHash: String): Maybe<ActivitySummaryItem> =
        Maybe.empty()

    override val hasTransactions: Boolean
        get() = false

    override val isFunded: Boolean
        get() = false

    final override val actions: AvailableActions
        get() = availableActions

    private val availableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send,
        AssetAction.Receive,
        AssetAction.Swap
    )
}

// Currently only one custodial account is supported for each asset,
// so all the methods on this can just delegate directly
// to the (required) CryptoSingleAccountCustodialBase

class CryptoAccountCustodialGroup(
    asset: CryptoCurrency,
    override val label: String,
    accounts: CryptoAccountsList
) : CryptoAccountGroup {

    private val account: CryptoSingleAccountCustodialBase

    init {
        require(accounts.size == 1)
        require(accounts[0] is CryptoSingleAccountCustodialBase)

        account = accounts[0] as CryptoSingleAccountCustodialBase
    }

    override val cryptoCurrency: CryptoCurrency?
        get() = account.cryptoCurrency

    override val balance: Single<CryptoValue>
        get() = account.balance

    override val activity: Single<ActivitySummaryList>
        get() = account.activity

    override val actions: AvailableActions
        get() = account.actions

    override val hasTransactions: Boolean
        get() = account.hasTransactions

    override val isFunded: Boolean
        get() = account.isFunded

    override fun findActivityItem(txHash: String): Maybe<ActivitySummaryItem> =
        account.findActivityItem(txHash)
}

class CryptoAccountCompoundGroup(
    val asset: CryptoCurrency,
    override val label: String,
    val accounts: CryptoAccountsList
) : CryptoAccountGroup {
    override val cryptoCurrency: CryptoCurrency? = asset

    // Produce the sum of all balances of all accounts
    override val balance: Single<CryptoValue>
        get() = Single.zip(
                accounts.map { it.balance }
            ) { t: Array<Any> ->
                t.sumBy { it as CryptoValue }
            }

    fun <T> Array<T>.sumBy(selector: (T) -> CryptoValue): CryptoValue {
        var sum: CryptoValue = CryptoValue.zero(asset)
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }

    // Al; the activities for all the accounts
    override val activity: Single<ActivitySummaryList>
        get() = Single.zip(
            accounts.map { it.activity }
        ) { t: Array<Any> ->
            t.filterIsInstance<List<ActivitySummaryItem>>().flatten()
        }

    // The intersection of the actions for each account
    override val actions: AvailableActions
        get() = accounts.map { it.actions }.reduce { a, b -> a.intersect(b) }

    // if _any_ of the accounts have transactions
    override val hasTransactions: Boolean
        get() = accounts.map { it.hasTransactions }.any { it }

    // Are any of the accounts funded
    override val isFunded: Boolean =
        accounts.map { it.isFunded }.any { it }

    // We can delegate to the asset token for this, since it holds a cache.
    // All the assets are the same, so a call on any account will delegate
    override fun findActivityItem(txHash: String): Maybe<ActivitySummaryItem> =
        accounts[0].findActivityItem(txHash)
}