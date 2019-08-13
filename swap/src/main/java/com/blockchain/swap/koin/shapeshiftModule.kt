package com.blockchain.swap.koin

import com.blockchain.swap.common.trade.MorphTradeDataHistoryList
import com.blockchain.swap.common.trade.MorphTradeDataManager
import com.blockchain.swap.shapeshift.ShapeShiftApi
import com.blockchain.swap.shapeshift.ShapeShiftEndpoints
import com.blockchain.swap.shapeshift.ShapeShiftUrls
import org.koin.dsl.module.applicationContext
import com.blockchain.swap.shapeshift.ShapeShiftDataManager
import com.blockchain.swap.shapeshift.ShapeShiftDataManagerAdapter
import com.blockchain.swap.shapeshift.datastore.ShapeShiftDataStore
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory

val shapeShiftModule = applicationContext {

    bean("shapeshift") {
        Retrofit.Builder()
            .baseUrl(ShapeShiftUrls.SHAPESHIFT_URL)
            .client(get())
            .addConverterFactory(get<JacksonConverterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .build()
    }

    bean {
        get<Retrofit>("shapeshift").create(ShapeShiftEndpoints::class.java)
    }

    factory { ShapeShiftApi(get()) }

    context("Payload") {

        bean { ShapeShiftDataStore() }

        factory { ShapeShiftDataManager(get(), get(), get(), get()) }

        factory("shapeshift") { ShapeShiftDataManagerAdapter(get()) }
            .bind(MorphTradeDataManager::class)
            .bind(MorphTradeDataHistoryList::class)
    }
}
