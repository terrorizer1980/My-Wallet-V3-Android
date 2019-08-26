package piuk.blockchain.android.ui.kyc.extensions

import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.swap.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.swap.nabu.models.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.models.mapFromMetadata
import com.blockchain.serialization.fromMoshiJson
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager

@Deprecated("Use the MetadataRepository, it's easier to mock, and doesn't require any custom json deserializing")
internal fun MetadataManager.fetchNabuToken(): Single<NabuOfflineTokenResponse> =
    this.fetchMetadata(NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE)
        .map {
            if (!it.isPresent) throw MetadataNotFoundException("Nabu Token not found")

            val metadata = NabuCredentialsMetadata::class.fromMoshiJson(it.get())
            if (!metadata.isValid()) throw MetadataNotFoundException("Nabu Token is empty")

            return@map metadata.mapFromMetadata()
        }
        .subscribeOn(Schedulers.io())
        .singleOrError()
        .cache()
