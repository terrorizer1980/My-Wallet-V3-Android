package com.blockchain.koin.modules

import com.blockchain.sunriver.XlmFeesFetcher
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.sunriver.XlmFeesFetcherAdapter

val xlmModule = applicationContext {
    context("Payload") {
        bean {
            XlmFeesFetcherAdapter(get()) as XlmFeesFetcher
        }
    }
}
