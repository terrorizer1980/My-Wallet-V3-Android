package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager

class SimpleBuyAvailability(
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val simpleBuyFlag: FeatureFlag,
    private val buyDataManager: BuyDataManager
) {

    fun isAvailable(): Single<Boolean> {
        val hasStartedAtLeastOnce = simpleBuyPrefs.flowStartedAtLeastOnce()

        val eligibleCheck = buyDataManager.isCoinifyAllowed
            .map {
                !it || hasStartedAtLeastOnce
            }.onErrorReturn { false }

        return simpleBuyFlag.enabled
            .flatMap { enabled ->
                if (!enabled) {
                    Single.just(false)
                } else {
                    eligibleCheck
                }
            }
    }
}