package piuk.blockchain.android.coincore.impl

import androidx.annotation.CallSuper
import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import io.reactivex.Completable
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus

internal abstract class BitcoinLikeTokens(
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    crashLogger: CrashLogger,
    rxBus: RxBus
) : AssetTokensBase(
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    crashLogger,
    rxBus
) {

    private var lastBalanceRefresh: Long = 0

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
