package piuk.blockchain.android.data.api.bitpay

import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import io.reactivex.Single
import piuk.blockchain.android.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Url

interface BitPay {

    @GET
    @Headers(
        "BP_PARTNER: Blockchain",
        "BP_PARTNER_VERSION: V${BuildConfig.VERSION_NAME}"
    )
    fun getRawPaymentRequest(
        @Url path: String,
        @Header("Accept") acceptType: String
    ): Single<RawPaymentRequest>
}
