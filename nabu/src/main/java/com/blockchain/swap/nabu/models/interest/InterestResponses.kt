package com.blockchain.swap.nabu.models.interest

import com.squareup.moshi.Json


data class InterestAddressResponse(
    val depositAddress: String?
)

data class InterestResponse(
    @Json(name="BTC")
    val assetInterestRate: Double?
)