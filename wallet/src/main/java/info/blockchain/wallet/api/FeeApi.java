package info.blockchain.wallet.api;

import info.blockchain.balance.CryptoCurrency;
import info.blockchain.wallet.api.data.FeeOptions;

import io.reactivex.Observable;

public final class FeeApi {

    private final FeeEndpoints feeEndpoints;

    public FeeApi(FeeEndpoints feeEndpoints) {
        this.feeEndpoints = feeEndpoints;
    }

    /**
     * Returns a {@link FeeOptions} object for BTC which contains both a "regular" and a "priority"
     * fee option, both listed in Satoshis per byte.
     */
    public Observable<FeeOptions> getBtcFeeOptions() {
        return feeEndpoints.getBtcFeeOptions();
    }

    /**
     * Returns a {@link FeeOptions} object for BCH which contains both a "regular" and a "priority"
     * fee option, both listed in Satoshis per byte.
     */
    public Observable<FeeOptions> getBchFeeOptions() {
        return feeEndpoints.getBchFeeOptions();
    }

    /**
     * Returns a {@link FeeOptions} object for ETH which contains both a "regular" and a "priority"
     * fee option.
     */
    public Observable<FeeOptions> getEthFeeOptions() {
        return feeEndpoints.getEthFeeOptions();
    }

    /**
     * Returns a {@link FeeOptions} object for XLM which contains both a "regular" and a "priority"
     * fee option.
     */
    public Observable<FeeOptions> getXlmFeeOptions() {
        return feeEndpoints.getFeeOptions(CryptoCurrency.XLM.getSymbol().toLowerCase());
    }
}
