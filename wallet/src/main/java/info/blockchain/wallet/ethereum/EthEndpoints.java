package info.blockchain.wallet.ethereum;

import info.blockchain.wallet.ethereum.data.EthAddressResponseMap;
import info.blockchain.wallet.ethereum.data.EthLatestBlock;
import info.blockchain.wallet.ethereum.data.EthPushTxRequest;
import info.blockchain.wallet.ethereum.data.Erc20AddressResponse;
import info.blockchain.wallet.ethereum.data.EthTxDetails;
import io.reactivex.Observable;

import java.util.HashMap;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

interface EthEndpoints {

    @GET(EthUrls.ACCOUNT + "/{address}")
    Observable<EthAddressResponseMap> getEthAccount(@Path("address") String address);

    @GET(EthUrls.ACCOUNT + "/{address}" + EthUrls.IS_CONTRACT)
    Observable<HashMap<String, Boolean>> getIfContract(@Path("address") String address);

    @POST(EthUrls.PUSH_TX)
    Observable<HashMap<String, String>> pushTx(@Body EthPushTxRequest ethPushTxRequest);

    @GET(EthUrls.LATEST_BLOCK)
    Observable<EthLatestBlock> getLatestBlock();

    @GET(EthUrls.TX + "/{hash}")
    Observable<EthTxDetails> getTransaction(@Path("hash") String txHash);

    @GET(EthUrls.V2_DATA_ACCOUNT + "/{address}/token/{hash}/wallet")
    @Headers({"Accept: application/json"})
    Observable<Erc20AddressResponse> getErc20Address(@Path("address") String address,
                                                     @Path("hash") String hash);

}
