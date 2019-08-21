package com.blockchain.koin

import com.blockchain.swap.nabu.service.TradeLimitService
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.api.NabuMarkets
import com.blockchain.swap.nabu.api.TransactionStateAdapter
import com.blockchain.swap.nabu.metadata.MetadataRepositoryNabuTokenAdapter
import com.blockchain.swap.nabu.service.NabuMarketsService
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit

val nabuModule = applicationContext {

    bean { get<Retrofit>("nabu").create(NabuMarkets::class.java) }

    context("Payload") {

        factory { NabuMarketsService(get(), get()) }
            .bind(TradeLimitService::class)

        factory { MetadataRepositoryNabuTokenAdapter(get(), get()) as NabuToken }
    }

    moshiInterceptor("nabu") { builder ->
        builder.add(TransactionStateAdapter())
    }
}
