package com.blockchain.swap.nabu.models.simplebuy

import java.util.Date

data class SimpleBuyPairsResp(val pairs: List<SimpleBuyPairResp>)

data class SimpleBuyPairResp(val pair: String, val buyMin: Long, val buyMax: Long)

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)

data class SimpleBuyEligibility(val eligible: Boolean)

data class SimpleBuyCurrency(val currency: String)

data class SimpleBuyQuoteResponse(
    val time: Date
)

data class OrderCreationResponse(val id: String, val pair: String, val expiresAt: Date, val state: OrderStateResponse)

enum class OrderStateResponse {
    PENDING_DEPOSIT,
    PENDING_EXECUTION,
    DEPOSIT_MATCHED,
    FINISHED,
    CANCELED,
    FAILED,
    EXPIRED
}
