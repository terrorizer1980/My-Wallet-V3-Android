package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.swap.nabu.CreateNabuToken
import com.blockchain.swap.nabu.models.NabuOfflineTokenResponse
import io.reactivex.Single

internal class CreateNabuTokenAdapter(
    private val nabuDataManager: NabuDataManager
) : CreateNabuToken {

    override fun createNabuOfflineToken(): Single<NabuOfflineTokenResponse> =
        nabuDataManager.requestJwt()
            .flatMap { jwt ->
                nabuDataManager.getAuthToken(jwt)
            }
}
