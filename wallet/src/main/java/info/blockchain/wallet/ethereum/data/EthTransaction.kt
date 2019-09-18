package info.blockchain.wallet.ethereum.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigInteger

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
class EthTransaction {

    @JsonProperty("blockNumber")
    var blockNumber: Long? = 0L
    @JsonProperty("timeStamp")
    var timeStamp: Long = 0L
    @JsonProperty("hash")
    var hash: String = ""
    @JsonProperty("failFlag")
    var failFlag: Boolean = false
    @JsonProperty("errorDescription")
    var errorDescription: String = ""
    @JsonProperty("nonce")
    var nonce: String = ""
    @JsonProperty("blockHash")
    var blockHash: String? = ""
    @JsonProperty("transactionIndex")
    var transactionIndex: Int = 0
    @JsonProperty("from")
    var from: String = ""
    @JsonProperty("to")
    var to: String = ""
    @JsonProperty("value")
    var value: BigInteger = 0.toBigInteger()
    @JsonProperty("gas")
    var gas: BigInteger = 0.toBigInteger()
    @JsonProperty("gasPrice")
    var gasPrice: BigInteger = 0.toBigInteger()
    @JsonProperty("gasUsed")
    var gasUsed: BigInteger = 0.toBigInteger()
    @JsonProperty("input")
    var input: String = ""
    @JsonProperty("internalFlag")
    var internalFlag: Boolean = false

    fun getErrorDescription(): Any? {
        return errorDescription
    }
}
