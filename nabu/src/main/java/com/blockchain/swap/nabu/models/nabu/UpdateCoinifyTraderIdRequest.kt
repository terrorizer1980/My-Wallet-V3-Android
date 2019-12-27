package com.blockchain.swap.nabu.models.nabu

import com.blockchain.serialization.JsonSerializable

internal data class UpdateCoinifyTraderIdRequest(
    val coinifyTraderId: Int
) : JsonSerializable