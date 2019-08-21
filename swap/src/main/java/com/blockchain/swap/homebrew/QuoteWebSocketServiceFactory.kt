package com.blockchain.swap.homebrew

import com.blockchain.swap.common.exchange.service.QuoteService
import com.blockchain.swap.common.exchange.service.QuoteServiceFactory
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.network.websocket.Options
import com.blockchain.network.websocket.autoRetry
import com.blockchain.network.websocket.bufferUntilAuthenticated
import com.blockchain.network.websocket.debugLog
import com.blockchain.network.websocket.newBlockchainWebSocket
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient

internal class QuoteWebSocketServiceFactory(
    private val nabuWebSocketOptions: Options,
    private val auth: Authenticator,
    private val moshi: Moshi,
    private val okHttpClient: OkHttpClient
) : QuoteServiceFactory {

    override fun createQuoteService(): QuoteService {
        val socket = okHttpClient.newBlockchainWebSocket(nabuWebSocketOptions)
            .debugLog("Quotes")
            .autoRetry()
            .authenticate(auth)
            .bufferUntilAuthenticated(limit = 10)

        return QuoteWebSocket(socket, moshi)
    }
}
