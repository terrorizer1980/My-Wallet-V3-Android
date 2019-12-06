package com.blockchain.swap.nabu

import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import io.reactivex.Single

interface NabuToken {

    /**
     * Find or creates the token
     */
    fun fetchNabuToken(): Single<NabuOfflineTokenResponse>
}
