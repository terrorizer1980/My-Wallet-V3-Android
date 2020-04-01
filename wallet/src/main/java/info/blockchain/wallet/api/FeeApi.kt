package info.blockchain.wallet.api

import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

private val FEE_CACHE = mutableMapOf<String, FeeOptionsCacheEntry>()
private val CACHE_TIME = TimeUnit.MINUTES.toMillis(2)

data class FeeApi(private val feeEndpoints: FeeEndpoints) {

    /**
     * Returns a [FeeOptions] object for BTC which contains both a "regular" and a "priority"
     * fee option, both listed in Satoshis per byte.
     */
    val btcFeeOptions: Observable<FeeOptions>
        get() = byCache("BTC") { feeEndpoints.btcFeeOptions }

    /**
     * Returns a [FeeOptions] object for BCH which contains both a "regular" and a "priority"
     * fee option, both listed in Satoshis per byte.
     */
    val bchFeeOptions: Observable<FeeOptions>
        get() = byCache("BCH") { feeEndpoints.bchFeeOptions }

    /**
     * Returns a [FeeOptions] object for ETH which contains both a "regular" and a "priority"
     * fee option.
     */
    val ethFeeOptions: Observable<FeeOptions>
        get() = byCache("ETH") { feeEndpoints.ethFeeOptions }

    /**
     * Returns a [FeeOptions] object for XLM which contains both a "regular" and a "priority"
     * fee option.
     */
    val xlmFeeOptions: Observable<FeeOptions>
        get() = byCache("XLM") { feeEndpoints.getFeeOptions(CryptoCurrency.XLM.networkTicker.toLowerCase()) }
}

private data class FeeOptionsCacheEntry(val fee: Observable<FeeOptions>, val timestamp: Long)

private fun byCache(currency: String, loader: () -> Observable<FeeOptions>): Observable<FeeOptions> {
    val entry = FEE_CACHE[currency]

    val timestamp = System.currentTimeMillis()
    return if (entry == null || (timestamp - entry.timestamp) > CACHE_TIME) {
        val newEntry = loader().cache()
        FEE_CACHE[currency] = FeeOptionsCacheEntry(newEntry, timestamp)
        newEntry
    } else {
        entry.fee
    }
}
