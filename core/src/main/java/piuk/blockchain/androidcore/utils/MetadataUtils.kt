package piuk.blockchain.androidcore.utils

import info.blockchain.wallet.exceptions.MetadataException
import info.blockchain.wallet.metadata.Metadata

import org.bitcoinj.crypto.DeterministicKey

import java.io.IOException

/**
 * Simple wrapper class to allow mocking of metadata keys
 */
class MetadataUtils {
    @Throws(IOException::class, MetadataException::class)
    fun getMetadataNode(metaDataHDNode: DeterministicKey, type: Int): Metadata {
        return Metadata.Builder(metaDataHDNode, type).build()
    }
}
