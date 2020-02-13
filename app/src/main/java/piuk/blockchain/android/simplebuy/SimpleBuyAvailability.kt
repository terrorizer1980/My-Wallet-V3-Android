package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith

class SimpleBuyAvailability(
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
    private val tierService: TierService,
    private val simpleBuyFlag: FeatureFlag
) {

    fun isAvailable(): Single<Boolean> {
        val hasStartedAtLeastOnce = simpleBuyPrefs.flowStartedAtLeastOnce()

        val goldAndEligibleAvailabilityCheck = tierService.tiers().flatMap {
            when (it.combinedState) {
                Kyc2TierState.Tier2Approved -> custodialWalletManager.isEligibleForSimpleBuy()
                else -> Single.just(hasStartedAtLeastOnce)
            }
        }.onErrorReturn { false }

        return custodialWalletManager.isCurrencySupportedForSimpleBuy(currencyPrefs.selectedFiatCurrency)
            .zipWith(simpleBuyFlag.enabled).flatMap { (currencySupported, enabled) ->
                if (!currencySupported || !enabled) {
                    Single.just(false)
                } else {
                    goldAndEligibleAvailabilityCheck
                }
            }
    }
}