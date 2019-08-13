package com.blockchain.swap.nabu

import io.reactivex.Single

interface CurrentTier {

    fun usersCurrentTier(): Single<Int>
}