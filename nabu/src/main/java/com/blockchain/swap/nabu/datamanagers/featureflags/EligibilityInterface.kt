package com.blockchain.swap.nabu.datamanagers.featureflags

import io.reactivex.Single

interface EligibilityInterface {
    fun isEligibleForCall(): Single<Boolean>
}