package info.blockchain.wallet.api

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions

import io.reactivex.Observable

data class FeeApi(private val feeEndpoints: FeeEndpoints) {

    /**
     * Returns a [FeeOptions] object for BTC which contains both a "regular" and a "priority"
     * fee option, both listed in Satoshis per byte.
     */
    val btcFeeOptions: Observable<FeeOptions>
        get() = feeEndpoints.btcFeeOptions

    /**
     * Returns a [FeeOptions] object for BCH which contains both a "regular" and a "priority"
     * fee option, both listed in Satoshis per byte.
     */
    val bchFeeOptions: Observable<FeeOptions>
        get() = feeEndpoints.bchFeeOptions

    /**
     * Returns a [FeeOptions] object for ETH which contains both a "regular" and a "priority"
     * fee option.
     */
    val ethFeeOptions: Observable<FeeOptions>
        get() = feeEndpoints.ethFeeOptions

    /**
     * Returns a [FeeOptions] object for XLM which contains both a "regular" and a "priority"
     * fee option.
     */
    val xlmFeeOptions: Observable<FeeOptions>
        get() = feeEndpoints.getFeeOptions(CryptoCurrency.XLM.symbol.toLowerCase())
}
