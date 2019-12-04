package piuk.blockchain.android.ui.dashboard.assetdetails

import com.jakewharton.rxrelay2.BehaviorRelay
import info.blockchain.balance.toFiat
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.coincore.AssetTokens
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.charts.TimeSpan
import java.util.Locale

class AssetDetailsViewModel(buyDataManager: BuyDataManager, locale: Locale) {
    // input
    val token = BehaviorRelay.create<AssetTokens>()
    val timeSpan = BehaviorRelay.createDefault<TimeSpan>(TimeSpan.MONTH)

    // output
    val balance: Observable<Pair<String, String>> = token.flatMapSingle {
        it.totalBalance()
            .zipWith(it.exchangeRate())
    }.map { (cryptoBalance, fiatPrice) ->
        cryptoBalance.toStringWithSymbol() to cryptoBalance.toFiat(fiatPrice).toStringWithSymbol(locale)
    }.subscribeOn(Schedulers.io())

    private val _chartLoading: BehaviorRelay<Boolean> = BehaviorRelay.createDefault<Boolean>(false)

    val chartLoading: Observable<Boolean>
        get() = _chartLoading

    val exchangeRate: Observable<String> = token.flatMapSingle {
        it.exchangeRate()
    }.map {
        it.toStringWithSymbol(locale)
    }.subscribeOn(Schedulers.io())

    val historicPrices: Observable<List<PriceDatum>> =
        (timeSpan.distinctUntilChanged().withLatestFrom(token).doOnNext { _chartLoading.accept(true) })
            .switchMapSingle { (timeSpan, token) ->
                token.historicRateSeries(timeSpan, TimeInterval.FIFTEEN_MINUTES)
                    .onErrorResumeNext(Single.just(emptyList()))
            }
            .doOnNext { _chartLoading.accept(false) }
            .subscribeOn(Schedulers.io())

    val userCanBuy: Observable<Boolean> =
        buyDataManager
            .canBuy
            .onErrorReturn { true }
            .subscribeOn(Schedulers.io())
}