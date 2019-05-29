package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.ArrayList
import java.util.HashMap

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class WalletOptions {

    @JsonProperty("showBuySellTab")
    val buySellCountries: List<String> = ArrayList()

    @JsonProperty("partners")
    val partners: Partners = Partners()

    @JsonProperty("androidBuyPercent")
    val rolloutPercentage: Double = 0.toDouble()

    @JsonProperty("android")
    var androidFlags: MutableMap<String, Boolean> = mutableMapOf()

    @JsonProperty("ethereum")
    val ethereum: EthereumOptions = EthereumOptions()

    @JsonProperty("androidUpgrade")
    val androidUpgrade: Map<String, Int> = HashMap()

    @JsonProperty("mobileInfo")
    val mobileInfo: Map<String, String> = HashMap()

    @JsonProperty("bcash")
    private val bitcoinCashFees = HashMap<String, Int>()

    @JsonProperty("xlm")
    private val xlm = HashMap<String, Long>()

    @JsonProperty("mobile")
    private val mobile = HashMap<String, String>()

    @JsonProperty("domains")
    private val domains = HashMap<String, String>()

    val bchFeePerByte: Int
        get() = bitcoinCashFees["feePerByte"] ?: 0

    /**
     * Returns the XLM transaction timeout in seconds.
     *
     * See: https://github.com/stellar/stellar-core/issues/1811
     *
     * @return the timeout
     */
    val xlmTransactionTimeout: Long
        get() = xlm["sendTimeOutSeconds"] ?: 0

    val stellarhorizonUrl: String
        get() = domains["stellarHorizon"] ?: ""

    val buyWebviewWalletLink: String?
        get() = mobile["walletRoot"]

    val comRootLink: String
        get() = domains["comRoot"] ?: ""

    val walletLink: String
        get() = domains["comWalletApp"] ?: ""

    companion object {
        var XLM_DEFAULT_TIMEOUT_SECS: Long? = 10L
    }
}
