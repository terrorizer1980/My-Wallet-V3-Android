package com.blockchain.swap.nabu.models.simplebuy

import com.blockchain.swap.nabu.datamanagers.OrderInput
import com.blockchain.swap.nabu.datamanagers.OrderOutput

import java.util.Date

data class SimpleBuyPairsResp(val pairs: List<SimpleBuyPairResp>)

data class SimpleBuyPairResp(val pair: String, val buyMin: Long, val buyMax: Long) {
    @Transient
    val fiatCurrency = pair.split("-")[1]
    @Transient
    val cryptoCurrency = pair.split("-")[0]
}

data class SimpleBuyEligibility(val eligible: Boolean)

data class SimpleBuyCurrency(val currency: String)

data class SimpleBuyQuoteResponse(
    val time: Date
)

data class BankAccountResponse(val address: String?, val agent: BankAgentResponse, val currency: String)

data class BankAgentResponse(
    val account: String?,
    val address: String?,
    val code: String?,
    val country: String?,
    val name: String?,
    val recipient: String?,
    val routingNumber: String?
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

data class SimpleBuyBalanceResponse(
    val available: String
)

data class CustodialWalletOrder(
    private val pair: String,
    private val action: String,
    private val input: OrderInput,
    private val output: OrderOutput
)
