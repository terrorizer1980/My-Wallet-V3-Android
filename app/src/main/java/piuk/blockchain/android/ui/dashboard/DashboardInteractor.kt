package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.android.coincore.BalanceFilter
import piuk.blockchain.androidcore.data.charts.TimeSpan
import timber.log.Timber

class DashboardInteractor(
    private val tokens: AssetTokenLookup
) {

    fun refreshBalances(model: DashboardModel, balanceFilter: BalanceFilter): Disposable {

        val cd = CompositeDisposable()
        CryptoCurrency.activeCurrencies().forEach {
            cd += tokens[it].totalBalance(balanceFilter)
                    .subscribeBy(
                        onSuccess = { balance -> model.process(BalanceUpdate(it, balance)) },
                        onError = { e -> Timber.e("Failed getting balance for $it: $e") }
                    )
        }
        return cd
    }

    fun refreshPrices(model: DashboardModel, crypto: CryptoCurrency): Disposable {
        val oneDayAgo = (System.currentTimeMillis() / 1000) - ONE_DAY

        return Singles.zip(
            tokens[crypto].exchangeRate(),
            tokens[crypto].historicRate(oneDayAgo)
        ) { rate, day -> PriceUpdate(crypto, rate, day) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )
    }

    fun refreshPriceHistory(model: DashboardModel, crypto: CryptoCurrency): Disposable =
        if (crypto.hasFeature(CryptoCurrency.PRICE_CHARTING)) {
            tokens[crypto].historicRateSeries(TimeSpan.DAY, TimeInterval.ONE_HOUR)
        } else {
            Single.just(FLATLINE_CHART)
        }
        .map { PriceHistoryUpdate(crypto, it) }
        .subscribeBy(
            onSuccess = { model.process(it) },
            onError = { Timber.e(it) }
        )

    companion object {
        private const val ONE_DAY = 24 * 60 * 60L
        private val FLATLINE_CHART = listOf(
                PriceDatum(price = 1.0, timestamp = 0),
                PriceDatum(price = 1.0, timestamp = System.currentTimeMillis() / 1000)
            )
    }
}
