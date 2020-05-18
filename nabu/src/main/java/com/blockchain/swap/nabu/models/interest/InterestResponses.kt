package com.blockchain.swap.nabu.models.interest

import com.squareup.moshi.Json


data class InterestAddressResponse(
    val depositAddress: String?
)

data class InterestResponse(
    @Json(name="BTC")
    val assetInterestRate: Double?
)

@Json(name="BTC")
data class InterestAccountBalanceResponse(
    val available: Double?,
    val pendingInterest:Double?,
    val fiatAmount:Double?,
    val fiatCurrency: String?
)