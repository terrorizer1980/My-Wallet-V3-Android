package piuk.blockchain.android.coincore

import androidx.annotation.CallSuper
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.math.BigInteger

abstract class BitcoinLikeTokens(rxBus: RxBus) : AssetTokensBase(rxBus) {

    private var lastBalanceRefresh: Long = 0

    protected fun accountReference(account: AccountReference): AccountReference.BitcoinLike =
        account as? AccountReference.BitcoinLike ?: throw IllegalArgumentException("Not an BTC/BCH Account Ref")

    protected fun updater(force: Boolean = false): Completable {
        val now = System.currentTimeMillis()

        return if (force || (now > lastBalanceRefresh + REFRESH_INTERVAL)) {
            doUpdateBalances()
                .doOnComplete { lastBalanceRefresh = System.currentTimeMillis() }
        } else {
            Completable.complete()
        }
    }

    protected abstract fun doUpdateBalances(): Completable

    @CallSuper
    override fun onLogoutSignal(event: AuthEvent) {
        lastBalanceRefresh = 0
    }

    @CallSuper
    override fun onMetadataSignal(event: MetadataEvent) {
       Timber.d(">>>>>>> METADATA LOADED: TODO -> Init coin: ${asset.symbol}")
    }

    companion object {
        private const val REFRESH_INTERVAL = 60 * 1000
    }
}

fun Completable.toCryptoSingle(cryptoValue: CryptoCurrency, getValue: () -> BigInteger): Single<CryptoValue> {
    return this.andThen(
        Single.defer {
            Single.just(CryptoValue(cryptoValue, getValue()))
        }
    )
}
