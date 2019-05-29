package com.blockchain.kycui.stablecoin

import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.Single

class StableCoinCampaignHelper(private val featureFlag: FeatureFlag) {
    fun isEnabled(): Single<Boolean> =
        featureFlag.enabled
}