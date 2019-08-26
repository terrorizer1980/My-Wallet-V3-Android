package info.blockchain.wallet.ethereum

import com.blockchain.serialization.JsonSerializableAccount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

import info.blockchain.wallet.ethereum.util.HashUtil

import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder

import java.util.Arrays

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE)
class EthereumAccount : JsonSerializableAccount {

    @JsonProperty("archived")
    private val archived: Boolean = false

    @JsonProperty("label")
    var label = ""

    @JsonProperty("correct")
    var isCorrect: Boolean = false

    @JsonProperty("addr")
    private var _address: String = ""

    val address: String
        get() = Keys.toChecksumAddress(_address)

    constructor() {
        // default constructor for Jackson
    }

    // TODO: 24/08/2017 Case sensitivity is used for optional checksum
    // TODO: 24/08/2017 https://forum.ethereum.org/discussion/9220/eth-address-upper-and-lower-characters-does-not-matter
    constructor(addressKey: ECKey) {
        this._address =
            HashUtil.toHexString(
                computeAddress(addressKey.pubKeyPoint.getEncoded(false)))
    }

    /**
     * Compute an address from an encoded public key.
     *
     * @param pubBytes an encoded (uncompressed) public key
     * @return 20-byte address
     */
    private fun computeAddress(pubBytes: ByteArray): ByteArray {
        return HashUtil.sha3omit12(Arrays.copyOfRange(pubBytes, 1, pubBytes.size))
    }

    /**
     * @param transaction
     * @return Signed transaction bytes
     */
    fun signTransaction(transaction: RawTransaction, accountKey: ECKey): ByteArray {
        val credentials = Credentials.create(accountKey.privateKeyAsHex)
        return TransactionEncoder.signMessage(transaction, credentials)
    }

    companion object {

        private val DERIVATION_PATH = "m/44'/60'/0'/0"
        private val DERIVATION_PATH_PURPOSE = 44
        private val DERIVATION_PATH_COIN = 60
        private val CHANGE_INDEX = 0
        private val ADDRESS_INDEX = 0

        fun deriveAccount(masterKey: DeterministicKey, accountIndex: Int, label: String): EthereumAccount {
            val ethereumAccount = EthereumAccount(deriveECKey(masterKey, accountIndex))
            ethereumAccount.label = label
            ethereumAccount.isCorrect = true
            return ethereumAccount
        }

        fun deriveECKey(masterKey: DeterministicKey, accountIndex: Int): ECKey {

            val purposeKey =
                HDKeyDerivation.deriveChildKey(
                    masterKey,
                    DERIVATION_PATH_PURPOSE or ChildNumber.HARDENED_BIT
                )
            val rootKey = HDKeyDerivation.deriveChildKey(
                purposeKey,
                DERIVATION_PATH_COIN or ChildNumber.HARDENED_BIT
            )
            val accountKey = HDKeyDerivation.deriveChildKey(
                rootKey,
                accountIndex or ChildNumber.HARDENED_BIT
            )
            val changeKey = HDKeyDerivation.deriveChildKey(
                accountKey,
                CHANGE_INDEX
            )
            val addressKey = HDKeyDerivation.deriveChildKey(
                changeKey,
                ADDRESS_INDEX
            )

            return ECKey.fromPrivate(addressKey.privKeyBytes)
        }
    }
}
