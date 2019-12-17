package info.blockchain.wallet.metadata

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Optional

import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.api.PersistentUrls
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.MetadataException
import info.blockchain.wallet.metadata.data.MetadataRequest
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.MetadataUtil

import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.DeterministicKey
import org.json.JSONException
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.util.encoders.Base64
import org.spongycastle.util.encoders.Hex

import java.io.IOException
import java.nio.charset.StandardCharsets

class Metadata(
    @VisibleForTesting
    val address: String,
    private val node: ECKey,
    private val encryptionKey: ByteArray? = null,
    private val isEncrypted: Boolean = true,
    private val type: Int = 0
) {
    var magicHash: ByteArray? = null
    @VisibleForTesting
        set

    private var attempt: Short = FETCH_MAGIC_HASH_ATTEMPT_LIMIT

    private val apiInstance: MetadataEndpoints by lazy {
        BlockchainFramework.getRetrofitApiInstance().create(MetadataEndpoints::class.java)
    }

    val metadata: String?
        @Throws(MetadataException::class, IOException::class, InvalidCipherTextException::class)
        get() = getMetadataEntry(address, isEncrypted).orNull()

    // Handling null in RxJava 2.0
    val metadataOptional: Optional<String>
        @Throws(MetadataException::class, IOException::class, InvalidCipherTextException::class)
        get() = getMetadataEntry(address, isEncrypted)

    @Throws(IOException::class, MetadataException::class)
    fun fetchMagic() {
        val response = apiInstance.getMetadata(address)

        val exe = response.execute()

        if (exe.isSuccessful) {
            val body = exe.body()!!

            val encryptedPayloadBytes = Base64.decode(body.payload.toByteArray(charset("utf-8")))

            magicHash = if (body.prevMagicHash != null) {
                val prevMagicBytes = Hex.decode(body.prevMagicHash)
                MetadataUtil.magic(encryptedPayloadBytes, prevMagicBytes)
            } else {
                MetadataUtil.magic(encryptedPayloadBytes, null)
            }
        } else {
            if (exe.code() == 404) {
                magicHash = null
            } else {
                throw MetadataException(exe.code().toString() + " " + exe.message())
            }
        }
    }

    @Throws(IOException::class, InvalidCipherTextException::class, MetadataException::class)
    fun putMetadata(payloadJson: String) {

        // Ensure json syntax is correct
        if (!FormatsUtil.isValidJson(payloadJson))
            throw JSONException("Payload is not a valid json object.")

        val encryptedPayloadBytes: ByteArray = if (isEncrypted) {
            // base64 to buffer
            Base64.decode(AESUtil.encryptWithKey(encryptionKey, payloadJson))
        } else {
            payloadJson.toByteArray(charset("utf-8"))
        }

        val nextMagicHash = MetadataUtil.magic(encryptedPayloadBytes, magicHash)

        val message = MetadataUtil.message(encryptedPayloadBytes, magicHash)

        val sig = node.signMessage(String(Base64.encode(message)))

        val body = MetadataRequest().apply {
            version = METADATA_VERSION
            payload = String(Base64.encode(encryptedPayloadBytes))
            signature = sig
            prevMagicHash = if (magicHash != null) Hex.toHexString(magicHash) else null
            typeId = type
        }

        val response = apiInstance.putMetadata(address, body)
        val exe = response.execute()

        if (!exe.isSuccessful) {
            if (exe.code() == 401 && attempt > 0) {
                // Unauthorized - Possible cross platform clash
                // Fetch magic hash and retry
                fetchMagic()
                attempt--
                putMetadata(payloadJson)
            } else {
                throw MetadataException(exe.code().toString() + " " + exe.message())
            }
        } else {
            attempt = FETCH_MAGIC_HASH_ATTEMPT_LIMIT
            magicHash = nextMagicHash
        }
    }

    @Throws(MetadataException::class, IOException::class, InvalidCipherTextException::class)
    fun getMetadata(address: String, isEncrypted: Boolean): String? {
        return getMetadataEntry(address, isEncrypted).orNull()
    }

    @Throws(MetadataException::class, IOException::class, InvalidCipherTextException::class)
    private fun getMetadataEntry(address: String?, isEncrypted: Boolean): Optional<String> {

        val response = apiInstance.getMetadata(address)
        val exe = response.execute()

        return if (exe.isSuccessful) {
            val body = exe.body()!!
            if (isEncrypted) {
                Optional.of(AESUtil.decryptWithKey(encryptionKey, body.payload))
            } else {
                Optional.of(String(Base64.decode(body.payload)))
            }
        } else {
            if (exe.code() == 404) {
                Optional.absent()
            } else {
                throw MetadataException(exe.code().toString() + " " + exe.message())
            }
        }
    }

    /**
     * Delete metadata entry
     */
    @Throws(IOException::class, InvalidCipherTextException::class, MetadataException::class)
    fun deleteMetadata(payload: String) {

        val encryptedPayloadBytes = if (isEncrypted) {
            // base64 to buffer
            Base64.decode(AESUtil.encryptWithKey(encryptionKey, payload))
        } else {
            payload.toByteArray(StandardCharsets.UTF_8)
        }

        val message = MetadataUtil.message(encryptedPayloadBytes, magicHash)

        val signature = node.signMessage(String(Base64.encode(message)))

        val response = apiInstance.deleteMetadata(address, signature)
        val exe = response.execute()

        if (!exe.isSuccessful) {
            throw MetadataException(exe.code().toString() + " " + exe.message())
        } else {
            magicHash = null
        }
    }

    class Builder(
        private val metaDataHDNode: DeterministicKey, // Required
        private val type: Int = -1
    ) {
        // Optional Override
        private var isEncrypted = true // default
        private var encryptionKey: ByteArray? = null

        fun setEncrypted(isEncrypted: Boolean): Builder {
            this.isEncrypted = isEncrypted
            return this
        }

        /**
         * purpose' / type' / 0' : https://meta.blockchain.info/{address} - signature used to
         * authorize purpose' / type' / 1' : sha256(private key) used as 256 bit AES key
         */
        @Throws(IOException::class, MetadataException::class)
        fun build(): Metadata {

            val payloadTypeNode = MetadataUtil.deriveHardened(metaDataHDNode, type)
            val newNode = MetadataUtil.deriveHardened(payloadTypeNode, 0)

            if (encryptionKey == null) {
                val privateKeyBuffer = MetadataUtil.deriveHardened(payloadTypeNode, 1).privKeyBytes
                encryptionKey = Sha256Hash.hash(privateKeyBuffer)
            }

            val address = newNode.toAddress(PersistentUrls.getInstance().bitcoinParams).toString()
            return Metadata(
                address = address,
                node = newNode,
                encryptionKey = encryptionKey,
                isEncrypted = isEncrypted,
                type = type
            ).apply {
                fetchMagic()
            }
        }
    }

    companion object {
        private const val METADATA_VERSION = 1
        private const val FETCH_MAGIC_HASH_ATTEMPT_LIMIT: Short = 1

        const val METADATA_TYPE_EXTERNAL_CONTACTS = 4
    }
}