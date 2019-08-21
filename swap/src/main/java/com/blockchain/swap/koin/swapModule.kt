package com.blockchain.swap.koin

import com.blockchain.swap.homebrew.QuoteWebSocketServiceFactory
import com.blockchain.swap.common.exchange.service.QuoteServiceFactory
import com.blockchain.swap.nabu.service.TradeExecutionService
import com.blockchain.swap.homebrew.HomeBrewTradeExecutionService
import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import com.blockchain.swap.common.trade.MorphTradeDataManager
import com.blockchain.swap.nabu.NabuDataManagerAdapter
import org.koin.dsl.module.applicationContext

val swapModule = applicationContext {

    context("Payload") {

        factory {
            QuoteWebSocketServiceFactory(
                get("nabu"),
                get(),
                get(),
                get()
            ) as QuoteServiceFactory
        }

        factory {
            HomeBrewTradeExecutionService(get()) as TradeExecutionService
        }

        factory("nabu") { NabuDataManagerAdapter(get(), get()) }
            .bind(MorphTradeDataManager::class)
            .bind(MorphTradeDataHistoryList::class)
    }
}
