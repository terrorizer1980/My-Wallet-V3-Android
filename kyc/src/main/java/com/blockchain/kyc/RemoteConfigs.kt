package com.blockchain.kyc

import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag

fun sunriverAirdropRemoteConfig(remoteConfiguration: RemoteConfig) =
    remoteConfiguration.featureFlag("android_sunriver_airdrop_enabled")

fun stableCoinRemoteConfig(remoteConfiguration: RemoteConfig) =
    remoteConfiguration.featureFlag("android_stablecoin_enabled")

fun smsVerificationRemoteConfig(remoteConfiguration: RemoteConfig) =
    remoteConfiguration.featureFlag("android_sms_verification")
