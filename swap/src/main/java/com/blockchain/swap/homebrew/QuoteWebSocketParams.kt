package com.blockchain.swap.homebrew

import com.blockchain.swap.common.quote.ExchangeQuoteRequest
import com.blockchain.serialization.JsonSerializable
import io.reactivex.Observable
import java.util.Locale

data class QuoteWebSocketParams(
    val pair: String,
    val volume: String,
    val fiatCurrency: String,
    val fix: String,
    val type: String = "conversionSpecification"
) : JsonSerializable

data class QuoteWebSocketUnsubscribeParams(
    val pair: String,
    val type: String
) : JsonSerializable

fun Observable<ExchangeQuoteRequest>.mapToSocketParameters(): Observable<QuoteWebSocketParams> =
    map(ExchangeQuoteRequest::mapToSocketParameters)

internal fun ExchangeQuoteRequest.mapToSocketParameters() =
    when (this) {
        is ExchangeQuoteRequest.Selling ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = offering.toNetworkString(),
                fiatCurrency = indicativeFiatSymbol,
                fix = "base"
            )
        is ExchangeQuoteRequest.SellingFiatLinked ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = offeringFiatValue.toNetworkString(),
                fiatCurrency = offeringFiatValue.currencyCode,
                fix = "baseInFiat"
            )
        is ExchangeQuoteRequest.Buying ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = wanted.toNetworkString(),
                fiatCurrency = indicativeFiatSymbol,
                fix = "counter"
            )
        is ExchangeQuoteRequest.BuyingFiatLinked ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = wantedFiatValue.toNetworkString(),
                fiatCurrency = wantedFiatValue.currencyCode,
                fix = "counterInFiat"
            )
    }
