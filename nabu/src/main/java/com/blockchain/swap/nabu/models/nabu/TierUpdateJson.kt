package com.blockchain.swap.nabu.models.nabu

import com.blockchain.serialization.JsonSerializable

internal data class TierUpdateJson(
    val selectedTier: Int
) : JsonSerializable
