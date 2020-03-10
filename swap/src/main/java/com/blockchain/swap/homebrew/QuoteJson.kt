package com.blockchain.swap.homebrew

import com.blockchain.annotations.CommonCode
import com.blockchain.swap.nabu.service.Fix
import com.blockchain.swap.nabu.service.Quote
import com.blockchain.swap.nabu.api.CryptoAndFiat
import com.blockchain.swap.nabu.api.QuoteJson
import com.blockchain.swap.nabu.api.Value
import com.blockchain.serialization.JsonSerializable
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import java.util.UnknownFormatConversionException

internal data class QuoteMessageJson(
    val seqnum: Int,
    val channel: String,
    val event: String,
    val quote: QuoteJson?
) : JsonSerializable

internal fun QuoteJson.mapToQuote(): Quote {
    return Quote(
        fix = fix.stringToFix(),
        from = currencyRatio.base.mapToQuoteValue(),
        to = currencyRatio.counter.mapToQuoteValue(),
        baseToFiatRate = currencyRatio.baseToFiatRate,
        baseToCounterRate = currencyRatio.baseToCounterRate,
        counterToFiatRate = currencyRatio.counterToFiatRate,
        rawQuote = this
    )
}

internal fun String.stringToFix() = when (this) {
    "base" -> Fix.BASE_CRYPTO
    "baseInFiat" -> Fix.BASE_FIAT
    "counter" -> Fix.COUNTER_CRYPTO
    "counterInFiat" -> Fix.COUNTER_FIAT
    else -> throw IllegalArgumentException("Unknown fix \"$this\"")
}

private fun CryptoAndFiat.mapToQuoteValue(): Quote.Value {
    return Quote.Value(
        cryptoValue = crypto.toCryptoValue(),
        fiatValue = fiat.toFiatValue()
    )
}

@CommonCode("Also exists in NabuMarketService. Make method on Value")
private fun Value.toFiatValue() =
    FiatValue.fromMajor(symbol, value)

@CommonCode("Also exists in NabuMarketService. Make method on Value")
private fun Value.toCryptoValue() =
    CryptoValue.fromMajor(CryptoCurrency.fromNetworkTicker(symbol)
        ?: throw UnknownFormatConversionException("Unknown Crypto currency: $symbol"),
        value)
