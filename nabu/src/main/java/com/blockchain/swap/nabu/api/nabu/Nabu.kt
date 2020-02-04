package com.blockchain.swap.nabu.api.nabu

import com.blockchain.swap.nabu.datamanagers.CustodialWalletOrder
import com.blockchain.swap.nabu.models.nabu.AddAddressRequest
import com.blockchain.swap.nabu.models.nabu.AirdropStatusList
import com.blockchain.swap.nabu.models.nabu.ApplicantIdRequest
import com.blockchain.swap.nabu.models.nabu.NabuBasicUser
import com.blockchain.swap.nabu.models.nabu.NabuCountryResponse
import com.blockchain.swap.nabu.models.nabu.NabuJwt
import com.blockchain.swap.nabu.models.nabu.NabuStateResponse
import com.blockchain.swap.nabu.models.nabu.NabuUser
import com.blockchain.swap.nabu.models.nabu.RecordCountryRequest
import com.blockchain.swap.nabu.models.nabu.RegisterCampaignRequest
import com.blockchain.swap.nabu.models.nabu.SendToMercuryAddressRequest
import com.blockchain.swap.nabu.models.nabu.SendToMercuryAddressResponse
import com.blockchain.swap.nabu.models.nabu.SendWithdrawalAddressesRequest
import com.blockchain.swap.nabu.models.nabu.SupportedDocumentsResponse
import com.blockchain.swap.nabu.models.nabu.TierUpdateJson
import com.blockchain.swap.nabu.models.nabu.TiersJson
import com.blockchain.swap.nabu.models.nabu.UpdateCoinifyTraderIdRequest
import com.blockchain.swap.nabu.models.nabu.VeriffToken
import com.blockchain.swap.nabu.models.nabu.WalletMercuryLink
import com.blockchain.swap.nabu.models.simplebuy.OrderCreationResponse
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyEligibility
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyQuoteResponse
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPairsResp
import com.blockchain.swap.nabu.models.simplebuy.SimpleBuyPredefinedAmountsResponse
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenRequest
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.swap.nabu.models.tokenresponse.NabuSessionTokenResponse
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface Nabu {

    @POST(NABU_INITIAL_AUTH)
    fun getAuthToken(
        @Body jwt: NabuOfflineTokenRequest,
        @Query("fiatCurrency") currency: String? = null,
        @Query("action") action: String? = null
    ): Single<NabuOfflineTokenResponse>

    @POST(NABU_SESSION_TOKEN)
    fun getSessionToken(
        @Query("userId") userId: String,
        @Header("authorization") authorization: String,
        @Header("X-WALLET-GUID") guid: String,
        @Header("X-WALLET-EMAIL") email: String,
        @Header("X-APP-VERSION") appVersion: String,
        @Header("X-CLIENT-TYPE") clientType: String,
        @Header("X-DEVICE-ID") deviceId: String
    ): Single<NabuSessionTokenResponse>

    @PUT(NABU_USERS_CURRENT)
    fun createBasicUser(
        @Body basicUser: NabuBasicUser,
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_USERS_CURRENT)
    fun getUser(
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET(NABU_AIRDROP_CENTRE)
    fun getAirdropCampaignStatus(
        @Header("authorization") authorization: String
    ): Single<AirdropStatusList>

    @PUT(NABU_UPDATE_WALLET_INFO)
    fun updateWalletInformation(
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Single<NabuUser>

    @GET(NABU_COUNTRIES)
    fun getCountriesList(
        @Query("scope") scope: String?
    ): Single<List<NabuCountryResponse>>

    @GET("$NABU_COUNTRIES/{regionCode}/$NABU_STATES")
    fun getStatesList(
        @Path("regionCode") countryCode: String,
        @Query("scope") scope: String?
    ): Single<List<NabuStateResponse>>

    @GET("$NABU_SUPPORTED_DOCUMENTS/{countryCode}")
    fun getSupportedDocuments(
        @Path("countryCode") countryCode: String,
        @Header("authorization") authorization: String
    ): Single<SupportedDocumentsResponse>

    @PUT(NABU_PUT_ADDRESS)
    fun addAddress(
        @Body address: AddAddressRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST(NABU_RECORD_COUNTRY)
    fun recordSelectedCountry(
        @Body recordCountryRequest: RecordCountryRequest,
        @Header("authorization") authorization: String
    ): Completable

    /**
     * This is a GET, but it actually starts a veriff session on the server for historical reasons.
     * So do not call more than once per veriff launch.
     */

    @GET(NABU_VERIFF_TOKEN)
    fun startVeriffSession(
        @Header("authorization") authorization: String
    ): Single<VeriffToken>

    @POST(NABU_SUBMIT_VERIFICATION)
    fun submitVerification(
        @Body applicantIdRequest: ApplicantIdRequest,
        @Header("authorization") authorization: String
    ): Completable

    @POST("$NABU_RECOVER_USER/{userId}")
    fun recoverUser(
        @Path("userId") userId: String,
        @Body jwt: NabuJwt,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_REGISTER_CAMPAIGN)
    fun registerCampaign(
        @Body campaignRequest: RegisterCampaignRequest,
        @Header("X-CAMPAIGN") campaignHeader: String,
        @Header("authorization") authorization: String
    ): Completable

    @GET(NABU_KYC_TIERS)
    fun getTiers(
        @Header("authorization") authorization: String
    ): Single<TiersJson>

    @POST(NABU_KYC_TIERS)
    fun setTier(
        @Body tierUpdateJson: TierUpdateJson,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_UPDATE_COINIFY_ID)
    fun setCoinifyTraderId(
        @Body coinifyTraderId: UpdateCoinifyTraderIdRequest,
        @Header("authorization") authorization: String
    ): Completable

    @PUT(NABU_CONNECT_WALLET_TO_PIT)
    fun connectWalletWithMercury(
        @Header("authorization") authorization: String
    ): Single<WalletMercuryLink>

    @PUT(NABU_CONNECT_PIT_TO_WALLET)
    fun connectMercuryWithWallet(
        @Header("authorization") authorization: String,
        @Body linkId: WalletMercuryLink
    ): Completable

    @POST(NABU_SEND_WALLET_ADDRESSES_TO_PIT)
    fun sharePitReceiveAddresses(
        @Header("authorization") authorization: String,
        @Body addresses: SendWithdrawalAddressesRequest
    ): Completable

    @PUT(NABU_FETCH_PIT_ADDRESS_FOR_WALLET)
    fun fetchPitSendAddress(
        @Header("authorization") authorization: String,
        @Body currency: SendToMercuryAddressRequest
    ): Single<SendToMercuryAddressResponse>

    @GET(NABU_SIMPLE_BUY_PAIRS)
    fun getSupportedSimpleBuyPairs(
        @Header("authorization") authorization: String
    ): Single<SimpleBuyPairsResp>

    @GET(NABU_SIMPLE_BUY_AMOUNTS)
    fun getPredefinedAmounts(
        @Header("authorization") authorization: String,
        @Query("currency") currency: String
    ): Single<SimpleBuyPredefinedAmountsResponse>

    @GET(NABU_SIMPLE_QUOTE)
    fun getSimpleBuyQuote(
        @Header("authorization") authorization: String,
        @Query("currencyPair") currencyPair: String,
        @Query("action") action: String,
        @Query("amount") amount: String
    ): Single<SimpleBuyQuoteResponse>

    @GET(NABU_SIMPLE_BUY_ELIGIBILITY)
    fun isEligibleForSimpleBuy(
        @Query("fiatCurrency") currency: String
    ): Single<SimpleBuyEligibility>

    @POST(NABU_SIMPLE_BUY_CREATE_ORDER)
    fun createOrder(
        @Header("authorization") authorization: String,
        @Body order: CustodialWalletOrder
    ): Single<OrderCreationResponse>
}
