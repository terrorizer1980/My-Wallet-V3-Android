package piuk.blockchain.android.simplebuy

import io.reactivex.Single

interface SimpleBuyConfiguration {
    fun isEnabled(): Single<Boolean>
}

class SimpleBuyConfigurationImpl : SimpleBuyConfiguration {
    override fun isEnabled(): Single<Boolean> =
        Single.just(true)
}