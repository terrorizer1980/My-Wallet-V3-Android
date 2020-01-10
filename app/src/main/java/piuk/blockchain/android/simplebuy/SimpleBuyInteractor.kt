package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.models.nabu.kycVerified
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.toFiatWithCurrency
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

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

    fun cancelOrder(): Single<SimpleBuyIntent.OrderCanceled> =
        Single.just(SimpleBuyIntent.OrderCanceled)

    fun confirmOrder(): Single<SimpleBuyIntent.OrderConfirmed> =
        Single.just(SimpleBuyIntent.OrderConfirmed)

    fun pollForKycState(): Single<SimpleBuyIntent.KycStateUpdated> =
        tierService.tiers().map {
            if (it.combinedState in kycVerified)
                SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED)
            if (it.combinedState.isRejectedOrInReview())
                SimpleBuyIntent.KycStateUpdated(KycState.FAILED)
            else
                SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
        }.onErrorReturn {
            SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
        }
            .repeatWhen { it.delay(5, TimeUnit.SECONDS).zipWith(Flowable.range(0, 6)) }
            .takeWhile { it.kycState == KycState.PENDING }
            .last(SimpleBuyIntent.KycStateUpdated(KycState.PENDING)).map {
                if (it.kycState != KycState.PENDING) {
                    return@map it
                } else {
                    return@map SimpleBuyIntent.KycStateUpdated(KycState.FAILED)
                }
            }

    private fun Kyc2TierState.isRejectedOrInReview(): Boolean =
        this == Kyc2TierState.Tier1Failed ||
                this == Kyc2TierState.Tier1InReview ||
                this == Kyc2TierState.Tier2InReview ||
                this == Kyc2TierState.Tier2Failed
}