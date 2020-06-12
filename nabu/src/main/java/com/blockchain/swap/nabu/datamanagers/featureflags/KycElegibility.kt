package com.blockchain.swap.nabu.datamanagers.featureflags

import com.blockchain.swap.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.swap.nabu.models.nabu.KycState
import io.reactivex.Single
import timber.log.Timber

class KycElegibility(private val nabuDataUserProvider: NabuDataUserProvider) :
    ElegibilityInterface {
    override fun isElegibleForCall(): Single<Boolean> {
        return nabuDataUserProvider.getUser().map {
            Timber.e("---- isElegibleForCall $it - tier ${it.currentTier} - state ${it.kycState}")
            it.currentTier == 2 &&
                it.kycState == KycState.Verified
        }
    }
}