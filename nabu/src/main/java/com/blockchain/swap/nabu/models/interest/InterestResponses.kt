package com.blockchain.swap.nabu.models.interest

import com.squareup.moshi.Json

data class InterestAddressResponse(
    val depositAddress: String?
)

data class InterestResponse(
    @Json(name = "rates")
    val rates : RateResponseData
)

data class RateResponseData(
    @Json(name = "BTC")
    val assetInterestRate: Double
)

@Json(name = "BTC")
data class InterestAccountBalanceResponse(
    val balance: Long
)