package com.blockchain.kyc

import com.blockchain.swap.nabu.models.NabuSessionTokenResponse

fun getEmptySessionToken(): NabuSessionTokenResponse = NabuSessionTokenResponse(
    "ID",
    "USER_ID",
    "TOKEN",
    true,
    "EXPIRES_AT",
    "INSERTED_AT",
    "UPDATED_AT"
)