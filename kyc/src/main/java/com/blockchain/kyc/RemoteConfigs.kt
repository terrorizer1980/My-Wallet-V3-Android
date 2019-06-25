package com.blockchain.kyc

import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag

fun sunriverAirdropRemoteConfig(remoteConfiguration: RemoteConfig) =
    remoteConfiguration.featureFlag("android_sunriver_airdrop_enabled")

fun smsVerificationRemoteConfig(remoteConfiguration: RemoteConfig) =
    remoteConfiguration.featureFlag("android_sms_verification")
