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
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.androidcore.utils.extensions.switchToSingleIfEmpty
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

abstract class CryptoSingleAccountBase
    : CryptoSingleAccount {

    protected val cryptoAsset: CryptoCurrency
        get() = cryptoCurrency!!

    override val hasTransactions: Single<Boolean>
        get() = Single.just(false)
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

    override val hasTransactions: Single<Boolean>
        get() = Single.just(false)

    override val isFunded: Single<Boolean>
        get() = Single.just(isNonCustodialConfigured.get())

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

    override val hasTransactions: Single<Boolean>
        get() = Single.just(false)

    override val isFunded: Single<Boolean>
        get() = Single.just(false)

    final override val actions: AvailableActions
        get() = availableActions

    private val availableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.Send,
        AssetAction.Receive,
        AssetAction.Swap
    )
}