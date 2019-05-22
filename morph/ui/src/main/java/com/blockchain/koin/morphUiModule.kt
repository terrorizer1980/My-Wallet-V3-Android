package com.blockchain.koin

import com.blockchain.datamanagers.BalanceCalculator
import com.blockchain.datamanagers.BalanceCalculatorImpl
import com.blockchain.morph.trade.MergingMorphTradeDataManager
import com.blockchain.morph.trade.MorphTradeDataHistoryList
import com.blockchain.morph.ui.homebrew.exchange.ExchangeModel
import com.blockchain.morph.ui.homebrew.exchange.confirmation.ExchangeConfirmationPresenter
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryPresenter
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.utils.PrefsUtil

val morphUiModule = applicationContext {

    bean { PrefsUtil(get()) }

    context("Payload") {

        factory { ExchangeConfirmationPresenter(get("Priority"), get(), get()) }

        factory("merge") {
            MergingMorphTradeDataManager(
                get("nabu"),
                get("shapeshift")
            )
        }.bind(MorphTradeDataHistoryList::class)

        factory { TradeHistoryPresenter(get("merge"), get()) }

        factory { BalanceCalculatorImpl(get()) }.bind(BalanceCalculator::class)

        context("Quotes") {

            factory {
                ExchangeModel(quoteServiceFactory = get(),
                    allAccountList = get(),
                    tradeLimitService = get(),
                    currentTier = get(),
                    transactionExecutor = get("Priority"),
                    maximumSpendableCalculator = get("Priority"),
                    currencyPreference = get(),
                    ethEligibility = get())
            }
        }
    }

    apply { registerDebug() }
}
