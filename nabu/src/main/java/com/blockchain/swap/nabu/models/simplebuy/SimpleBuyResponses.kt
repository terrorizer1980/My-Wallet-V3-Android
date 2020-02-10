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

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)

data class SimpleBuyEligibility(val eligible: Boolean)

data class SimpleBuyCurrency(val currency: String)

data class SimpleBuyQuoteResponse(
    val time: Date
)

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

data class BuyOrderResponse(
    val id: String,
    val pair: String,
    val inputCurrency: String,
    val inputQuantity: String,
    val outputCurrency: String,
    val outputQuantity: String,
    val state: OrderStateResponse,
    val expiresAt: Date
)

typealias BuyOrderListResponse = List<BuyOrderResponse>