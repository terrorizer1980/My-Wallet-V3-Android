package com.blockchain.nabu.api

import com.blockchain.serialization.JsonSerializable

data class TradeFailureJson(
    val txHash: String?,
    val failureReason: FailureReasonJson?,
    val diagnostics: TradeDiagnostics?
) : JsonSerializable

data class FailureReasonJson(
    val message: String
) : JsonSerializable

data class TradeDiagnostics(
    val maxAvailable: String,
    val tradeValueFiat: String,
    val tradeValueCrypto: String
) : JsonSerializable