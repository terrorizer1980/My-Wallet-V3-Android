package com.blockchain.swap.common.exchange.service

import com.blockchain.swap.nabu.service.Quote
import com.blockchain.swap.common.quote.ExchangeQuoteRequest
import info.blockchain.balance.ExchangeRate
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

interface ReadOnlyQuoteStream {

    /**
     * Stream of quotes
     */
    val quotes: Observable<Quote>
}

interface ReadOnlyRatesStream {

    /**
     * Stream of exchange rates
     */
    val rates: Observable<ExchangeRate>
}

interface QuoteRequestListener {
    /**
     * Replace the last quote request with a new request
     */
    fun updateQuoteRequest(quoteRequest: ExchangeQuoteRequest)
}

interface QuoteStream : ReadOnlyQuoteStream,
    QuoteRequestListener

interface ExchangeRateStream : ReadOnlyRatesStream,
    QuoteRequestListener

interface QuoteService : QuoteStream,
    ExchangeRateStream {

    /**
     * Start the service. On [Disposable.dispose] it will close.
     */
    fun openAsDisposable(): Disposable

    /**
     * Stream of connection status
     */
    val connectionStatus: Observable<Status>

    enum class Status {
        Open,
        Closed,
        Error
    }
}

interface QuoteServiceFactory {

    fun createQuoteService(): QuoteService
}
