package com.blockchain.swap.nabu.api.trade

import com.blockchain.serialization.JsonSerializable
import com.blockchain.swap.nabu.api.QuoteJson

data class TradeRequest(
    val destinationAddress: String,
    val refundAddress: String,
    val quote: QuoteJson
) : JsonSerializable
