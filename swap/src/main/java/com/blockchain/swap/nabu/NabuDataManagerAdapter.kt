package com.blockchain.swap.nabu

import com.blockchain.swap.common.trade.MorphTrade
import com.blockchain.swap.common.trade.MorphTradeDataManager
import com.blockchain.swap.common.trade.MorphTradeStatus
import com.blockchain.swap.nabu.service.NabuMarketsService
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

internal class NabuDataManagerAdapter(
    private val nabuMarketsService: NabuMarketsService,
    private val currencyPreference: CurrencyPrefs
) : MorphTradeDataManager {

    override fun findTrade(depositAddress: String): Single<MorphTrade> =
        nabuMarketsService
            .getTrades(currencyPreference.selectedFiatCurrency)
            .flattenAsObservable { it }
            .filter { it.depositAddress == depositAddress }
            .map<MorphTrade> {
                NabuTradeAdapter(
                    it
                )
            }
            .singleOrError()

    override fun getTrades(): Single<List<MorphTrade>> =
        nabuMarketsService
            .getTrades(currencyPreference.selectedFiatCurrency)
            .flattenAsObservable { it }
            .map<MorphTrade> {
                NabuTradeAdapter(
                    it
                )
            }
            .toList()

    override fun getTradeStatus(depositAddress: String): Observable<MorphTradeStatus> =
        nabuMarketsService
            .getTrades(currencyPreference.selectedFiatCurrency)
            .flattenAsObservable { it }
            .filter { it.depositAddress == depositAddress }
            .map<MorphTradeStatus> {
                NabuTradeStatusResponseAdapter(
                    it
                )
            }

    override fun updateTrade(
        orderId: String,
        newStatus: MorphTrade.Status,
        newHashOut: String?
    ): Completable = Completable.complete()
}