package piuk.blockchain.android.coincore.impl

import androidx.annotation.CallSuper
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Single
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import java.lang.IllegalArgumentException
import java.math.BigInteger

internal abstract class BitcoinLikeTokens(rxBus: RxBus)
    : AssetTokensBase(rxBus) {

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

    companion object {
        private const val REFRESH_INTERVAL = 60 * 1000
    }
}

fun Completable.toCryptoSingle(cryptoValue: CryptoCurrency, getValue: () -> BigInteger): Single<CryptoValue> {
    return thenSingle {
        Single.just(CryptoValue(cryptoValue, getValue()))
    }
}
