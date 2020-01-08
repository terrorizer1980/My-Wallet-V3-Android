package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiatWithCurrency
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import java.math.BigDecimal

class SimpleBuyInteractor(
    private val tokens: AssetTokenLookup,
    private val tierService: TierService,
    private val metadataManager: MetadataManager,
    private val exchangeRateFactory: ExchangeRateDataManager
) {

    fun updateExchangePriceForCurrency(cryptoCurrency: CryptoCurrency): Single<SimpleBuyIntent.PriceUpdate> =
        tokens[cryptoCurrency].exchangeRate().map { fiatValue ->
            SimpleBuyIntent.PriceUpdate(fiatValue)
        }

    fun fetchBuyLimits(targetCurrency: String): Single<SimpleBuyIntent.BuyLimits> =
        metadataManager.attemptMetadataSetup()
            // we have to ensure that exchange rates are loaded before we retrieve the exchange rates locally
            .andThen(tokens[CryptoCurrency.BTC].exchangeRate().ignoreElement())
            .andThen(tierService.tiers().map {
                val highestTierLimits = it.tiers.maxBy { tier -> tier.index }!!.limits

                val minValue = FiatValue.fromMajor(highestTierLimits.currency,
                    1.toBigDecimal()).toFiatWithCurrency(exchangeRateFactory, targetCurrency)

                val maxValue = (highestTierLimits.dailyFiat ?: FiatValue.fromMajor(highestTierLimits.currency,
                    BigDecimal.ZERO)).toFiatWithCurrency(exchangeRateFactory, targetCurrency)

                SimpleBuyIntent.BuyLimits(minValue, maxValue)
            })

    fun fetchPredefinedAmounts(targetCurrency: String): Single<SimpleBuyIntent.UpdatedPredefinedAmounts> =
        Single.just(SimpleBuyIntent.UpdatedPredefinedAmounts(listOf(
            FiatValue.fromMajor(targetCurrency, 100.toBigDecimal()),
            FiatValue.fromMajor(targetCurrency, 20.toBigDecimal()),
            FiatValue.fromMajor(targetCurrency, 10.toBigDecimal()),
            FiatValue.fromMajor(targetCurrency, 50.toBigDecimal())
        ).sortedBy {
            it.valueMinor
        }))
}