package com.blockchain.swap.nabu.datamanagers.featureflags

import io.reactivex.Single

interface Eligibility {
    fun isEligible(): Single<Boolean>
}