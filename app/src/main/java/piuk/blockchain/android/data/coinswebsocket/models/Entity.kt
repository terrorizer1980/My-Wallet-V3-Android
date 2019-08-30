package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName

enum class Entity {
    @SerializedName("height")
    HEIGHT,
    @SerializedName("header")
    HEADER,
    @SerializedName("confirmed_transaction")
    CONFIRMED_TRANSACTION,
    @SerializedName("pending_transaction")
    PENDING_TRANSACTION,
    @SerializedName("account")
    ACCOUNT,
    @SerializedName("token_transfer")
    TOKEN_TRANSFER,
    @SerializedName("token_account_delta")
    TOKEN_ACCOUNT_DELTA,
    @SerializedName("token_account")
    TOKEN_ACCOUNT
}