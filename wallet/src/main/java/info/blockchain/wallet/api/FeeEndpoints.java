package info.blockchain.wallet.api;

import info.blockchain.wallet.api.data.FeeOptions;
import io.reactivex.Observable;
import retrofit2.http.GET;

public interface FeeEndpoints {

    @GET("mempool/fees/btc")
    Observable<FeeOptions> getBtcFeeOptions();

    @GET("mempool/fees/eth")
    Observable<FeeOptions> getEthFeeOptions();
}
