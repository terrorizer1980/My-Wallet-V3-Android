package com.blockchain.kyc.models.nabu

data class SendToMercuryAddressRequest(val cryptoSymbol: String)

data class SendToMercuryAddressResponse(
    val address: String,
    val currency: String,
    val state: String // "PENDING" | "ACTIVE" | "BLOCKED"
)