package com.blockchain.swap.nabu.models.simplebuy

data class SimpleBuyPairsResp(val pairs: List<SimpleBuyPairResp>)

data class SimpleBuyPairResp(val pair: String, val buyMin: Long, val buyMax: Long)

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)

data class SimpleBuyEligibility(val eligible: Boolean)