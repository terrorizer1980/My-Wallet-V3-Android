package com.blockchain.swap.nabu

import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single

class DisabledFeatureFlag(
    override val enabled: Single<Boolean> = Single.just(false)
) : FeatureFlag