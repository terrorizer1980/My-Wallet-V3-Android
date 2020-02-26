package com.blockchain.swap.nabu.metadata

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.metadata.MetadataRepository
import com.blockchain.swap.nabu.CreateNabuToken
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.rx.maybeCache
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.models.tokenresponse.mapFromMetadata
import com.blockchain.swap.nabu.models.tokenresponse.mapToMetadata
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.metadata.MetadataNotInitialisedException

class MetadataRepositoryNabuTokenAdapter(
    private val metadataRepository: MetadataRepository,
    private val createNabuToken: CreateNabuToken,
    private val metadataManager: MetadataManager
) : NabuToken {

    private fun createMetaData(currency: String?, action: String?) = Maybe.defer {
        createNabuToken.createNabuOfflineToken(currency, action)
            .map {
                it.mapToMetadata()
            }
            .flatMapMaybe {
                metadataRepository.saveMetadata(
                    it,
                    NabuCredentialsMetadata::class.java,
                    NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                ).andThen(Maybe.just(it))
            }
    }

    private val defer = Maybe.defer {
        metadataRepository.loadMetadata(
            NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE,
            NabuCredentialsMetadata::class.java
        ).retryWhen {
            return@retryWhen it.zipWith(Flowable.range(1, 1)).flatMap { (error, _) ->
                if (error is MetadataNotInitialisedException)
                    metadataManager.attemptMetadataSetup().toFlowable(BackpressureStrategy.LATEST)
                else
                    Flowable.error(error)
            }
        }
    }.maybeCache()
        .filter { it.isValid() }

    override fun fetchNabuToken(currency: String?, action: String?): Single<NabuOfflineTokenResponse> =
        defer
            .switchIfEmpty(createMetaData(currency, action))
            .map { metadata ->
                if (!metadata.isValid()) throw MetadataNotFoundException("Nabu Token is empty")
                metadata.mapFromMetadata()
            }
            .toSingle()
}
