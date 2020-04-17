package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single

class SimpleBuyAvailability(
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val simpleBuyFlag: FeatureFlag
) {

    fun isAvailable(): Single<Boolean> {
        val hasStartedAtLeastOnce = simpleBuyPrefs.flowStartedAtLeastOnce()

        return simpleBuyFlag.enabled
            .flatMap { enabled ->
                if (!enabled) {
                    Single.just(false)
                } else {
                    Single.just(hasStartedAtLeastOnce)
                }
            }
    }
}