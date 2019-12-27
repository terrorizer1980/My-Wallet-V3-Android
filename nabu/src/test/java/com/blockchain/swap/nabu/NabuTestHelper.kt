package com.blockchain.swap.nabu

import com.blockchain.swap.nabu.models.tokenresponse.NabuSessionTokenResponse

fun getEmptySessionToken(): NabuSessionTokenResponse =
    NabuSessionTokenResponse(
        "ID",
        "USER_ID",
        "TOKEN",
        true,
        "EXPIRES_AT",
        "INSERTED_AT",
        "UPDATED_AT"
    )