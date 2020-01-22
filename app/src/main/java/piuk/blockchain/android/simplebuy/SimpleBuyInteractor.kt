package piuk.blockchain.android.simplebuy

import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.models.simplebuy.BuyLimits
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPair
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPairs
import com.blockchain.swap.nabu.service.TierService
import info.blockchain.balance.FiatValue
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.android.coincore.AssetTokenLookup
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import java.util.concurrent.TimeUnit

class SimpleBuyInteractor(
    private val tokens: AssetTokenLookup,
    private val nabu: NabuToken,
    private val tierService: TierService,
    private val metadataManager: MetadataManager,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val nabuDataManager: NabuDataManager
) {

    fun fetchBuyLimitsAndSupportedCryptoCurrencies(targetCurrency: String):
            Single<SimpleBuyIntent.UpdatedBuyLimitsAndSupportedCryptoCurrencies> = nabu.fetchNabuToken().flatMap {
        nabuDataManager.getSupportedCurrencies(it)
    }.map {
        SimpleBuyIntent.UpdatedBuyLimitsAndSupportedCryptoCurrencies(
            SimpleBuyPairs(listOf(
                SimpleBuyPair(pair = "BTC-USD", buyLimits = BuyLimits(100, 5024558)),
                SimpleBuyPair(pair = "BTC-EUR", buyLimits = BuyLimits(1006, 10000)),
                SimpleBuyPair(pair = "ETH-EUR", buyLimits = BuyLimits(1005, 10000)),
                SimpleBuyPair(pair = "BCH-EUR", buyLimits = BuyLimits(1001, 10000))
            )))
    }

    fun fetchPredefinedAmounts(targetCurrency: String): Single<SimpleBuyIntent.UpdatedPredefinedAmounts> =
        Single.just(SimpleBuyIntent.UpdatedPredefinedAmounts(listOf(
            FiatValue.fromMajor(targetCurrency, 100000.toBigDecimal()),
            FiatValue.fromMajor(targetCurrency, 5000.toBigDecimal()),
            FiatValue.fromMajor(targetCurrency, 1000.toBigDecimal()),
            FiatValue.fromMajor(targetCurrency, 500.toBigDecimal())
        ).sortedBy {
            it.valueMinor
        }))

    fun cancelOrder(): Single<SimpleBuyIntent.OrderCanceled> =
        Single.just(SimpleBuyIntent.OrderCanceled)

    fun confirmOrder(): Single<SimpleBuyIntent.OrderConfirmed> =
        Single.just(SimpleBuyIntent.OrderConfirmed)

    fun fetchBankAccount(): Single<SimpleBuyIntent.BankAccountUpdated> =
        Single.just(SimpleBuyIntent.BankAccountUpdated(BankAccount(
            listOf(
                BankDetail("Bank Name", "LHV"),
                BankDetail("Bank ID", "DE81 1234 5678 9101 1234 33", true),
                BankDetail("Bank Code (SWIFT/BIC", "DEKTDE7GSSS", true),
                BankDetail("Recipient", "Fred Wilson")
            )
        )))

    fun pollForKycState(): Single<SimpleBuyIntent.KycStateUpdated> =
        tierService.tiers().map {
            when {
                it.combinedState == Kyc2TierState.Tier2Approved ->
                    return@map SimpleBuyIntent.KycStateUpdated(KycState.VERIFIED)
                it.combinedState.isRejectedOrInReview() -> return@map SimpleBuyIntent.KycStateUpdated(KycState.FAILED)
                else -> return@map SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
            }
        }.onErrorReturn {
            SimpleBuyIntent.KycStateUpdated(KycState.PENDING)
        }
            .repeatWhen { it.delay(5, TimeUnit.SECONDS).zipWith(Flowable.range(0, 6)) }
            .takeUntil { it.kycState != KycState.PENDING }.last(SimpleBuyIntent.KycStateUpdated(KycState.PENDING)).map {
                if (it.kycState == KycState.PENDING) {
                    return@map SimpleBuyIntent.KycStateUpdated(KycState.UNDECIDED)
                } else {
                    return@map it
                }
            }

    private fun Kyc2TierState.isRejectedOrInReview(): Boolean =
        this == Kyc2TierState.Tier1Failed ||
                this == Kyc2TierState.Tier1InReview ||
                this == Kyc2TierState.Tier2InReview ||
                this == Kyc2TierState.Tier2Failed
}