package com.blockchain.swap.nabu.datamanagers.featureflags

import io.reactivex.Single

interface ElegibilityInterface {
    fun isElegibleForCall(): Single<Boolean>
}