package com.blockchain.announcement

import io.reactivex.Single

interface Announcement<Context> {

    fun shouldShow(context: Context): Single<Boolean>

    fun show(context: Context)
}
