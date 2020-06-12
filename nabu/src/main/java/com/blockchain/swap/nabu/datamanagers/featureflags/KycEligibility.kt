package com.blockchain.swap.nabu.datamanagers.featureflags

import com.blockchain.swap.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.swap.nabu.models.nabu.KycState
import io.reactivex.Single
import timber.log.Timber

class KycEligibility(private val nabuDataUserProvider: NabuDataUserProvider) :
    EligibilityInterface {
    override fun isEligibleForCall(): Single<Boolean> {
        return nabuDataUserProvider.getUser().map {
            Timber.e("---- isElegibleForCall $it - tier ${it.currentTier} - state ${it.kycState}")
            it.currentTier == 2 &&
                it.kycState == KycState.Verified
        }
    }
}