package com.blockchain.swap.nabu

import io.reactivex.Single

interface EthEligibility {
    fun isEligible(): Single<Boolean>
}