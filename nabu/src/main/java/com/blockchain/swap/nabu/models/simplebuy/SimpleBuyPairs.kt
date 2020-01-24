package com.blockchain.swap.nabu.models.simplebuy

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue

data class SimpleBuyPairs(val pairs: List<SimpleBuyPair>)

data class SimpleBuyPair(private val pair: String, val buyLimits: BuyLimits) {
    val cryptoCurrency: CryptoCurrency
        get() = CryptoCurrency.values().first { it.symbol == pair.split("-")[0] }
    val fiatCurrency: String = pair.split("-")[1]
}

data class BuyLimits(private val min: Long, private val max: Long) {
    fun minLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, min)
    fun maxLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, max)
}

data class SimpleBuyPairsResp(val pairs: List<String>)

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)