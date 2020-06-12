package com.blockchain.swap.nabu.datamanagers.featureflags

import com.blockchain.swap.nabu.datamanagers.NabuUserRepository
import com.blockchain.swap.nabu.models.nabu.KycState
import io.reactivex.Single

class KycEligibility(private val userRepository: NabuUserRepository) : Eligibility {
    override fun isEligible(): Single<Boolean> =
        Single.fromObservable(
            userRepository.fetchUser().map {
                it.currentTier == 2 && it.kycState == KycState.Verified
            }
        )
}