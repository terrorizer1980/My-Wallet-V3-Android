package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith

class SimpleBuyAvailability(
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
    private val simpleBuyFlag: FeatureFlag,
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) {
    private val nabuUser = nabuToken
        .fetchNabuToken()
        .flatMap {
            nabuDataManager.getUser(it)
        }

    fun isAvailable(): Single<Boolean> {
        val hasStartedAtLeastOnce = simpleBuyPrefs.flowStartedAtLeastOnce()

        val eligibleCheck = nabuUser.map {
            !it.isCoinifyTagged() || hasStartedAtLeastOnce
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