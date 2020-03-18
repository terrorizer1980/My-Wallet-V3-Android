package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager

class SimpleBuyAvailability(
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
    private val simpleBuyFlag: FeatureFlag,
    private val buyDataManager: BuyDataManager
) {

    fun isAvailable(): Single<Boolean> {
        val hasStartedAtLeastOnce = simpleBuyPrefs.flowStartedAtLeastOnce()

        val eligibleCheck = buyDataManager.isCoinifyAllowed
            .map {
                !it || hasStartedAtLeastOnce
            }.onErrorReturn { false }

        return custodialWalletManager.isCurrencySupportedForSimpleBuy(currencyPrefs.selectedFiatCurrency)
            .onErrorReturn { false }
            .zipWith(simpleBuyFlag.enabled)
            .flatMap { (currencySupported, enabled) ->
                if (!currencySupported || !enabled) {
                    Single.just(false)
                } else {
                    eligibleCheck
                }
            }
    }
}