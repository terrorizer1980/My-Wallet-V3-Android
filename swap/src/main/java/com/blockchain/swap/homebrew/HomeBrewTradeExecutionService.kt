package com.blockchain.swap.homebrew

import com.blockchain.swap.nabu.service.Quote
import com.blockchain.swap.nabu.service.TradeExecutionService
import com.blockchain.swap.nabu.service.TradeTransaction
import com.blockchain.swap.nabu.api.QuoteJson
import com.blockchain.swap.nabu.api.TradeRequest
import com.blockchain.swap.nabu.service.NabuMarketsService
import io.reactivex.Completable
import io.reactivex.Single

internal class HomeBrewTradeExecutionService(private val marketsService: NabuMarketsService) :
    TradeExecutionService {

    override fun executeTrade(
        quote: Quote,
        destinationAddress: String,
        refundAddress: String
    ): Single<TradeTransaction> {
        val rawQuote = quote.rawQuote ?: throw IllegalArgumentException("No quote supplied")
        val quoteJson = rawQuote as? QuoteJson ?: throw IllegalArgumentException("Quote is not expected type")

        return marketsService.executeTrade(
            TradeRequest(
                destinationAddress = destinationAddress,
                refundAddress = refundAddress,
                quote = quoteJson
            )
        ).map { it }
    }

    override fun putTradeFailureReason(tradeRequest: TradeTransaction, txHash: String?, message: String?): Completable {
        return marketsService.putTradeFailureReason(
            tradeRequestId = tradeRequest.id,
            txHash = txHash,
            message = message
        )
    }
}
