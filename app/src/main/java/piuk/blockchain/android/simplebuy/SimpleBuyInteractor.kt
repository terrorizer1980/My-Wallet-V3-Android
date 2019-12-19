package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import java.math.BigDecimal

class SimpleBuyInteractor(
    private val tokens: AssetTokenLookup,
    private val tierService: TierService,
    private val metadataManager: MetadataManager
) {

    fun updateExchangePriceForCurrency(cryptoCurrency: CryptoCurrency): Single<SimpleBuyIntent.PriceUpdate> =
        tokens[cryptoCurrency].exchangeRate().map { fiatValue ->
            SimpleBuyIntent.PriceUpdate(fiatValue)
        }

    fun fetchBuyLimits(): Single<SimpleBuyIntent.BuyLimits> =
        metadataManager.attemptMetadataSetup().andThen(
            tierService.tiers().map {
                val highestTierLimits = it.tiers.sortedBy { it.index }.last().limits
                SimpleBuyIntent.BuyLimits(FiatValue.fromMajor(highestTierLimits.currency, 1.toBigDecimal()),
                    highestTierLimits.dailyFiat ?: FiatValue.fromMajor(highestTierLimits.currency, BigDecimal.ZERO))
            })
}