package com.blockchain.sunriver

import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

interface XlmFees {
    val perOperationFee: Single<CryptoValue>
}

class XlmFeesService internal constructor(
    private val horizonProxy: HorizonProxy
) : XlmFees {

    override val perOperationFee: Single<CryptoValue>
        get() = Single.fromCallable {
            horizonProxy.fees() ?: CryptoValue.lumensFromStroop(100.toBigInteger())
        }.cache().subscribeOn(Schedulers.io())
}