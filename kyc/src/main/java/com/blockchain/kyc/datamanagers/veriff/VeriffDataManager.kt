package com.blockchain.kyc.datamanagers.veriff

import com.blockchain.kyc.models.onfido.ApplicantResponse
import io.reactivex.Single

class VeriffDataManager {

    /**
     * Creates a new KYC application in Veriff, and returns an [ApplicantResponse] object.
     *
     * @param firstName The applicant's first name
     * @param lastName The applicant's surname
     * @param apiToken Our mobile Veriff API token
     *
     * @return An [ApplicantResponse] wrapped in a [Single]
     */
    fun createApplicant(
        firstName: String,
        lastName: String,
        apiToken: String
    ): Single<ApplicantResponse> =
    // TODO: AND-1840 This is a mock response
        Single.just(
            ApplicantResponse(
                id = "",
                createdAt = "",
                sandbox = false,
                firstName = firstName,
                lastName = lastName,
                country = ""
            )
        )
}