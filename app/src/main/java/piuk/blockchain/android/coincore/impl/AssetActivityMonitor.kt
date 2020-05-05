package piuk.blockchain.android.coincore.impl

import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataManager
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.SwapActivitySummaryItem
import piuk.blockchain.android.ui.swap.homebrew.exchange.model.Trade
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import timber.log.Timber

class AssetActivityMonitor(
    private val coincore: Coincore,
    private val swapHistory: MorphTradeDataManager,
    private val exchangeRates: ExchangeRateDataManager
) {
    fun fetch(account: CryptoAccount): Single<ActivitySummaryList> {
        return Singles.zip(
            fetchSwapHistory(),
            account.activity
        ) { swapList, accountList ->
            accountList + swapList
        }
    }

    private fun fetchSwapHistory(): Single<ActivitySummaryList> {
        return swapHistory.getTrades()
            .flattenAsObservable { it }
            .doOnNext { Timber.d("***> $it")}
            .map { it.map(exchangeRates) }
            .toList()
    }
}

private fun MorphTrade.map(exchangeRates: ExchangeRateDataManager): ActivitySummaryItem =
    SwapActivitySummaryItem(
        exchangeRates = exchangeRates,
        cryptoCurrency = quote.pair.from,
        txId = this.hashOut ?: "",
        timeStampMs = timestamp * 1000,
        totalCrypto = quote.depositAmount,
        targetValue = quote.withdrawalAmount,
        fee = quote.minerFee
    )