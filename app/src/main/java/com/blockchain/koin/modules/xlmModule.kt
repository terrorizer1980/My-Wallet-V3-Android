package com.blockchain.koin.modules

import com.blockchain.sunriver.XlmFees
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import org.koin.dsl.module.applicationContext

val xlmModule = applicationContext {

    bean {
        object : XlmFees {
            // TODO: AND-1969 Get fees from wallet options
            override val perOperationFee: Single<CryptoValue>
                get() = Single.just(CryptoValue.lumensFromStroop(1500.toBigInteger()))
        } as XlmFees
    }
}
