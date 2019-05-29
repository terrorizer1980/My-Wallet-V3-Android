package com.blockchain.kyc.models.nabu

import com.blockchain.serialization.JsonSerializable

internal data class UpdateCoinifyTraderIdRequest(
    val coinifyTraderId: Int
) : JsonSerializable